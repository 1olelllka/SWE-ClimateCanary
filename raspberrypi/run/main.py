# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "aiosqlite>=0.20.0",
#     "pyyaml>=6.0",
#     "bleak>=0.21.0",
#     "aiohttp>=3.9.0"
# ]
# ///
import asyncio
import logging
import sys

from auth_manager   import AuthManager
from config_manager import ConfigManager
from db_manager     import DatabaseManager
from data_processor import DataProcessor
from web_manager    import WebManager
from ble_manager    import BLEManager
from typing import Any

logger = logging.getLogger(__name__)


async def main(static_config: dict):
    logger.info("Starting IoT Gateway")

    db = DatabaseManager(static_config['paths']['database'])
    await db.connect()
    await db.init_db()

    auth = AuthManager(
        server_url=static_config['webapp']['server_url'],
        username=static_config['auth']['username'],
        password=static_config['auth']['password']
    )
    try:
        await auth.login()
    except Exception as e:
        logger.error(f"[Auth] Login failed: {e}. Cannot start without authentication.")
        sys.exit(1)

    try:
        await ConfigManager.fetch_and_seed(static_config, db, auth)
    except Exception as e:
        logger.warning(
            f"[Config] Could not reach webapp on startup: {e}. "
            "Continuing with existing DB config — Pi must have run before."
        )

    sensors = await db.get_sensors()

    web_out_queue = asyncio.Queue()
    web_violation_queue = asyncio.Queue()
    processor = DataProcessor(db, web_out_queue, web_violation_queue)
    web_manager = WebManager(static_config, db, web_out_queue, web_violation_queue, auth)


    tasks: list[asyncio.Task[Any]] = [
        asyncio.create_task(web_manager.run_local_server(), name="WebServer"),
        asyncio.create_task(web_manager.run_outgoing_data_worker(), name="WebOutgoingData"),
        asyncio.create_task(web_manager.run_outgoing_violation_worker(), name="WebOutgoingViolation"),
        asyncio.create_task(web_manager.run_offline_sync_worker(), name="WebSync"),
    ]
    
    for sensor in sensors:
        proc_queue = asyncio.Queue()
        ble_queue = asyncio.Queue()

        ble_manager = BLEManager(db, sensor, proc_queue, ble_queue)
        
        # one ble manager per arduino 
        tasks.append(asyncio.create_task(ble_manager.run(), name=f"BLE:{sensor['name']}"))
        # one processor worker per arduino (same DataProcessor instance)
        tasks.append(asyncio.create_task(processor.run(sensor['name'], proc_queue, ble_queue), name=f"Proc:{sensor['name']}"))

    try:
        await asyncio.gather(*tasks)
    except asyncio.CancelledError:
        logger.info("All tasks cancelled. Exiting cleanly.")
    finally:
        logger.info("Gateway stopped.")


if __name__ == "__main__":
    try:
        static_config = ConfigManager.load("/home/pi/run/conf.yaml")
    except Exception as e:
        print(f"Failed to load config: {e}")
        sys.exit(1)

    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(static_config['paths']['log_file']),
            logging.StreamHandler(sys.stdout)
        ]
    )

    try:
        asyncio.run(main(static_config))
    except KeyboardInterrupt:
        logging.info("Gateway shutdown requested by user.")
