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

        for path in [config['paths']['database'], config['paths']['log_file']]:
            os.makedirs(os.path.dirname(path), exist_ok=True)

        return config

    @staticmethod
    async def fetch_and_seed(static_config: dict, db, auth) -> None:
        """Fetch dynamic config from webapp and seed DB on first run.
        Existing DB values are never overwritten — live updates win."""
        pi_id = static_config['identity']['raspberry_id']
        url = f"{static_config['webapp']['server_url']}/api/raspberry/{pi_id}/config"

        timeout = aiohttp.ClientTimeout(total=10)
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.get(url, headers=auth.get_headers()) as response:
                response.raise_for_status()
                remote = await response.json()

        sensors = remote.get('sensors', [])

        candidates = {
            'room_id':          remote.get('roomId'),
            'frequency':        str(remote.get('frequency', 10000)),
            'server_url':       static_config['webapp']['server_url'],

            # TODO right now only possible to register one arduino, refactor in the future
            'ble.target_name':  sensors[0].get('name'),
            'ble.char_uuid':    sensors[0]['readId']  if sensors else None,
            'ble.write_uuid':   sensors[0]['writeId'] if sensors else None,
            
            'max_temp':         remote['limits'].get('tempMax'),
            'min_temp':         remote['limits'].get('tempMin'),
            'max_moisture':     remote['limits'].get('humMax'),
            'min_moisture':     remote['limits'].get('humMin'),
            'max_co2':          remote['limits'].get('co2Max'),
        }

        for key, value in candidates.items():
            if value is None:
                continue
            existing = await db.get_config(key)
            if existing is None:  
                await db.set_config(key, value)

        logger.info(f"[Config] DB seeded from webapp for room {remote.get('roomId')}")

