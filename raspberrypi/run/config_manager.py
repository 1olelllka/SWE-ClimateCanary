import yaml
import os
import aiohttp
import logging

logger = logging.getLogger(__name__)

class ConfigManager:

    @staticmethod
    def load(filepath="/home/pi/run/conf.yaml") -> dict:
        if not os.path.exists(filepath):
            raise FileNotFoundError(f"Config file not found: {filepath}")

        with open(filepath, "r") as f:
            config = yaml.safe_load(f)

        for path_key in ['database', 'log_file']:
            full_path = config['paths'].get(path_key)
            if full_path:
                os.makedirs(os.path.dirname(full_path), exist_ok=True)

        return config

# full config fetch

    @staticmethod
    async def fetch_and_seed(pi_id: str, server_url: str, db, auth) -> None:
        await db.set_config('raspberry_id', pi_id)
        await db.set_config('server_url', server_url)

        try:
            remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/config", auth
            )
        except Exception as e:
            logger.warning(
                f"[Config] Could not reach webapp for full config fetch: {e}. "
                "Continuing with existing DB config."
            )
            return

        sensors = remote.get('sensors', [])
        limits = remote.get('limits', {})
        tips = remote.get('tips', {})

        sensor_list = [
            {
                'id': s.get('id'),
                'name': s.get('name'),
                'char_uuid': s['readId'],
                'write_uuid': s['writeId'],
            }
            for s in sensors if s.get('name')
        ]

        # Overwrite config keys unconditionally on (re-)seed
        config_updates = {
            'room_id': remote.get('roomId'),
            'frequency': str(remote.get('frequency', 100)),
        }
        for key, value in config_updates.items():
            if value is not None:
                await db.set_config(key, value)

        limit_updates = {
            'max_temp': limits.get('tempMax'),
            'min_temp': limits.get('tempMin'),
            'max_moisture': limits.get('humMax'),
            'min_moisture': limits.get('humMin'),
            'max_co2': limits.get('co2Max'),
        }
        for key, value in limit_updates.items():
            if value is not None:
                await db.set_limit(key, float(value))

        if sensor_list:
            await db.set_sensors(sensor_list)

        if tips:
            await db.set_tips(tips)

        logger.info(
            f"[Config] Seed complete: {len(sensor_list)} sensors, "
            f"room={remote.get('roomId')}, freq={remote.get('frequency')}"
        )

# Live update handlers, called by WebManager endpoints 

    @staticmethod
    async def handle_limit_change(db, payload: dict) -> None:
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
            logger.warning(f"[Config] SENSOR_ADD: none of the requested ids found in webapp response.")

    @staticmethod
    async def handle_sensor_delete(db, sensor_ids: list[str]) -> None:
        await db.delete_sensors_by_ids(sensor_ids)
        logger.info(f"[Config] Sensors deleted: {sensor_ids}")

    @staticmethod
    async def handle_sensor_flush(db) -> None:
        await db.delete_all_sensors()
        logger.info("[Config] All sensors flushed.")

    @staticmethod
    async def handle_config_change(db, auth, pi_id: str) -> str | None:
        server_url = await db.get_config('server_url')
        if not server_url:
            raise RuntimeError("server_url missing from DB — cannot fetch config.")

        remote = await ConfigManager._fetch(
            f"{server_url}/api/raspberry/{pi_id}/config", auth
        )

        updates = {
            'room_id': remote.get('roomId'),
            'frequency': str(remote['frequency']) if remote.get('frequency') else None,
        }
        for key, value in updates.items():
            if value is not None:
                await db.set_config(key, value)

        logger.info(f"[Config] General config refreshed for pi={pi_id}.")
        return updates.get('frequency')

# Internal helpers 

    @staticmethod
    async def _fetch(url: str, auth, timeout_s: int = 10) -> dict:
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
