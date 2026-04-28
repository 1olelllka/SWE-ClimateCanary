import yaml
import os
import aiohttp
import logging

logger = logging.getLogger(__name__)

class ConfigManager:

    @staticmethod
    def load(filepath="/home/pi/run/conf.yaml") -> dict:
        """Load static bootstrap config from YAML. Only called once at startup."""
        if not os.path.exists(filepath):
            raise FileNotFoundError(f"Config file not found: {filepath}")

        with open(filepath, "r") as f:
            config = yaml.safe_load(f)

        for path_key in ['database', 'log_file']:
            full_path = config['paths'].get(path_key)
            if full_path:
                os.makedirs(os.path.dirname(full_path), exist_ok=True)

        return config

# Startup, fetch full config and seed db

    @staticmethod
    async def fetch_and_seed(static_config: dict, db, auth) -> None:
        """Fetch full config from webapp on first boot and seed the DB.
        On first boot: writes all keys.
        On subsequent boots: skips keys already in DB so live updates win.
        If webapp is unreachable: logs a warning and continues with existing DB.
        """
        pi_id = static_config['identity']['raspberry_id']
        server_url = static_config['webapp']['server_url']

        await db.set_config('raspberry_id', pi_id)
        await db.set_config('server_url', server_url)

        remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/config", auth
                )

        sensors = remote.get('sensors', [])
        limits = remote.get('limits', {})
        tips = remote.get('tips', {})

        sensor_list = [
                {
                    'name':       s.get('name'),
                    'char_uuid':  s['readId'],
                    'write_uuid': s['writeId'],
                    }
                for s in sensors if s.get('name')
                ]

        candidates = {
                'room_id':         remote.get('roomId'),
                'raspberry_id':    pi_id,
                'server_url':      server_url,
                'frequency':       str(remote.get('frequency', 10000)),
                'max_temp':        limits.get('tempMax'),
                'min_temp':        limits.get('tempMin'),
                'max_moisture':    limits.get('humMax'),
                'min_moisture':    limits.get('humMin'),
                'max_co2':         limits.get('co2Max'),
                }

        seeded = 0
        for key, value in candidates.items():
            if value is None:
                continue
            if await db.get_config(key) is None:
                await db.set_config(key, value)
                seeded += 1

        if sensor_list:
            await db.set_sensors(sensor_list)

        if tips:
            await db.set_tips(tips)

        logger.info(f"[Config] Boot seed complete: {seeded} new keys, {len(sensor_list)} sensors for room {remote.get('roomId')}")

# live update handlers, called by WebManager when /notify fires

    @staticmethod
    async def handle_limit_change(db, auth) -> None:
        """Re-fetch and overwrite sensor limits from webapp."""
        pi_id, server_url = await ConfigManager._identity(db)

        remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/limits", auth
                )

        mapping = {
                'max_temp':     remote.get('tempMax'),
                'min_temp':     remote.get('tempMin'),
                'max_moisture': remote.get('humMax'),
                'min_moisture': remote.get('humMin'),
                'max_co2':      remote.get('co2Max'),
                }
        for key, value in mapping.items():
            if value is not None:
                await db.set_limit(key, float(value))

        logger.info("[Config] Limits refreshed from webapp.")

    @staticmethod
    async def handle_sensor_change(db, auth) -> None:
        """Re-fetch and overwrite BLE sensor list. """
        pi_id, server_url = await ConfigManager._identity(db)

        remote  = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/sensors", auth
                )
        sensors = remote.get('sensors', [])

        sensor_list = [
                {'name': s.get('name'), 'char_uuid': s['readId'], 'write_uuid': s['writeId']}
                for s in sensors if s.get('name')
                ]
        if sensor_list:
            await db.set_sensors(sensor_list)

        logger.info(f"[Config] Sensor list updated: {[s['name'] for s in sensor_list]}")

    @staticmethod
    async def handle_tips_change(db, auth) -> None:
        """Re-fetch and overwrite sensor tips from webapp.
        Expects the endpoint to return:
            {"tips": {"temperature": "Turn on the radiator", "co2": "Open a window", ...}}
        """
        pi_id, server_url = await ConfigManager._identity(db)

        remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/tips", auth
                )

        tips = remote.get('tips', {})
        if tips:
            await db.set_tips(tips)

        logger.info(f"[Config] Tips refreshed: {list(tips.keys())}")

    @staticmethod
    async def handle_occupancy_change(db, auth) -> None:
        """Re-fetch and overwrite occupancy values from webapp."""
        pi_id, server_url = await ConfigManager._identity(db)

        remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/occupancy", auth
                )

        if remote.get('current') is not None:
            await db.set_limit('current_occupancy', float(remote['current']))
        if remote.get('max') is not None:
            await db.set_limit('max_occupancy', float(remote['max']))

        logger.info("[Config] Occupancy refreshed from webapp.")

    # TODO implement separate handlers for frequency and roomid 
    @staticmethod
    async def handle_config_change(db, auth) -> None:
        """Re-fetch and overwrite general config (frequency, room assignment, etc.)."""
        pi_id, server_url = await ConfigManager._identity(db)

        remote = await ConfigManager._fetch(
                f"{server_url}/api/raspberry/{pi_id}/config", auth
                )

        updates = {
                'room_id':   remote.get('roomId'),
                'frequency': str(remote['frequency']) if remote.get('frequency') else None,
                }
        for key, value in updates.items():
            if value is not None:
                await db.set_config(key, value)

        logger.info("[Config] General config refreshed from webapp.")

# internal helpers

    @staticmethod
    async def _identity(db) -> tuple[str, str]:
        """Fetch raspberry_id and server_url from DB for building API URLs."""
        pi_id      = await db.get_config('raspberry_id')
        server_url = await db.get_config('server_url')
        if not pi_id or not server_url:
            raise RuntimeError("raspberry_id or server_url missing from DB config.")
        return pi_id, server_url

    @staticmethod
    async def _fetch(url: str, auth, timeout_s: int = 10) -> dict:
        """GET a JSON endpoint with auth, auto-refreshing token on 401."""
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
