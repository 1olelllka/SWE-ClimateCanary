import aiohttp
import logging

logger = logging.getLogger(__name__)

class ConfigManager:
    """Static helpers for fetching config from the webapp and applying it to the local DB."""

# Full config fetch

    @staticmethod
    async def fetch_and_seed(pi_id: str, server_url: str, db, auth):
        """Fetch the full Pi config from the webapp and write it to the DB.

        Covers room_id, frequency, all limits, occupancy, privacy_mode, and sensor list.
        Logs a warning for any null fields - DataProcessor will discard readings until they are set.
        Falls back to the existing DB config if the webapp is unreachable.
        """
        await db.set_config('raspberry_id', pi_id)
        await db.set_config('server_url', server_url)

        try:
            remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry-pis/{pi_id}/config", auth
            )
        except Exception as e:
            logger.warning(
                f"[Config] Could not reach webapp for full config fetch: {e}. "
                "Continuing with existing DB config."
            )
            return

        await db.set_config('room_id', remote.get('roomId'))
        await db.set_config('frequency', str(remote['frequency']) if remote.get('frequency') is not None else None)

        limits = remote.get('limits') or {}
        await db.set_limit('max_temp', float(limits['tempMax']) if limits.get('tempMax') is not None else None)
        await db.set_limit('min_temp', float(limits['tempMin']) if limits.get('tempMin') is not None else None)
        await db.set_limit('max_moisture', float(limits['humMax']) if limits.get('humMax') is not None else None)
        await db.set_limit('min_moisture', float(limits['humMin']) if limits.get('humMin') is not None else None)
        await db.set_limit('max_co2', float(limits['co2Max']) if limits.get('co2Max') is not None else None)

        occupancy = remote.get('occupancy') or {}
        effective = occupancy.get('effectiveOccupancy')
        privacy = occupancy.get('privacyMode')

        await db.set_limit('current_occupancy', float(effective) if effective is not None else None)
        await db.set_limit('privacy_mode', (1.0 if privacy else 0.0) if privacy is not None else None)

        sensors = remote.get('sensors', [])
        sensor_list = [
            {
                'name': s.get('name'),
                'char_uuid': s['readId'],
                'write_uuid': s['writeId'],
            }
            for s in sensors if s.get('name')
        ]
        await db.set_sensors(sensor_list)

        null_fields = [k for k, v in {
            'roomId': remote.get('roomId'),
            'frequency': remote.get('frequency'),
            'limits.tempMax': limits.get('tempMax'),
            'limits.tempMin': limits.get('tempMin'),
            'limits.humMax': limits.get('humMax'),
            'limits.humMin': limits.get('humMin'),
            'limits.co2Max': limits.get('co2Max'),
            'occupancy.effectiveOccupancy': effective,
            'occupancy.privacyMode': privacy,
        }.items() if v is None]

        if null_fields:
            logger.warning(
                f"[Config] Seed complete but null fields from remote: {null_fields}. "
                f"Processing will be halted until a valid config arrives. "
                f"Sensors seeded: {len(sensor_list)}"
            )
        else:
            logger.info(
                f"[Config] Seed complete: {len(sensor_list)} sensors, "
                f"room={remote.get('roomId')}, freq={remote.get('frequency')}, "
                f"occupancy={effective}, privacy={privacy}"
            )

# Live update handlers, called by WebManager endpoints

    @staticmethod
    async def handle_limit_change(db, payload: dict) -> None:
        """Apply a partial limit update from a /api/limits payload to the DB."""
        mapping = {
            'max_temp': payload.get('tempMax'),
            'min_temp': payload.get('tempMin'),
            'max_moisture': payload.get('humMax'),
            'min_moisture': payload.get('humMin'),
            'max_co2': payload.get('co2Max'),
        }
        updated = []
        for key, value in mapping.items():
            if value is not None:
                await db.set_limit(key, float(value))
                updated.append(f"{key}={value}")

        if updated:
            logger.info(f"[Config] Limits updated: {', '.join(updated)}")
        else:
            logger.warning("[Config] /api/limits received but all fields were null - nothing changed.")

    @staticmethod
    async def handle_occupancy_change(db, payload: dict) -> None:
        """Apply an occupancy/privacy update from a /api/occupancy payload to the DB."""
        effective = payload.get('effectiveOccupancy')
        privacy = payload.get('privacyMode')

        if effective is not None:
            await db.set_limit('current_occupancy', float(effective))
            logger.info(f"[Config] Occupancy updated: effectiveOccupancy={effective}")

        if privacy is not None:
            await db.set_limit('privacy_mode', 1.0 if privacy else 0.0)
            logger.info(f"[Config] Privacy mode updated: {privacy}")

        if effective is None and privacy is None:
            logger.warning("[Config] /api/occupancy received but no recognised fields - nothing changed.")

    @staticmethod
    async def handle_sensor_add(db, auth, sensor_ids: list[str]) -> None:
        """Fetch a sensor from /api/sensor-stations and add it to the local DB sensor list."""
        server_url = await db.get_config('server_url')
        if not server_url:
            raise RuntimeError("server_url missing from DB config.")

        remote = await ConfigManager._fetch(
            f"{server_url}/api/sensor-stations/{sensor_ids[0]}", auth
        )

        to_add = [
            {
                'name': remote['name'],
                'char_uuid': remote['readId'],
                'write_uuid': remote['writeId'],
            }
        ]

        if to_add:
            await db.add_sensors(to_add)
            logger.info(f"[Config] Sensors added: {[s['name'] for s in to_add]}")
        else:
            logger.warning("[Config] SENSOR_ADD: none of the requested ids found in webapp response.")

    @staticmethod
    async def handle_sensor_delete(db, sensor_ids: list[str]) -> None:
        """Remove sensors matching the given UUIDs from the local DB."""
        await db.delete_sensors_by_ids(sensor_ids)
        logger.info(f"[Config] Sensors deleted: {sensor_ids}")

    @staticmethod
    async def handle_sensor_flush(db) -> None:
        """Remove all sensors from the local DB."""
        await db.delete_all_sensors()
        logger.info("[Config] All sensors flushed.")

    @staticmethod
    async def handle_config_change(db, auth, pi_id: str) -> str | None:
        """Refresh room_id and frequency from the webapp and return the new frequency string."""
        server_url = await db.get_config('server_url')
        if not server_url:
            raise RuntimeError("server_url missing from DB - cannot fetch config.")

        remote = await ConfigManager._fetch(
            f"{server_url}/api/raspberry-pis/{pi_id}/config", auth
        )

        room_id = remote.get('roomId')
        frequency = str(remote['frequency']) if remote.get('frequency') else None

        await db.set_config('room_id', room_id)
        await db.set_config('frequency', frequency)

        logger.info(f"[Config] General config refreshed for pi={pi_id}.")
        return frequency

# Internal helpers

    @staticmethod
    async def _fetch(url: str, auth, timeout_s: int = 10) -> dict:
        """GET a URL with bearer auth, retrying once on 401."""
        timeout = aiohttp.ClientTimeout(total=timeout_s)
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.get(url, headers=auth.get_headers()) as response:
                if response.status == 401:
                    await auth.refresh_if_needed()
                    async with session.get(url, headers=auth.get_headers()) as retry:
                        retry.raise_for_status()
                        return await retry.json()
                response.raise_for_status()
                return await response.json()
