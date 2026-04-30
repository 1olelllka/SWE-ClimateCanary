# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "aiosqlite>=0.20.0",
#     "pyyaml>=6.0",
#     "bleak>=0.21.0",
#     "aiohttp>=3.9.0",
#     "requests>=2.31.0"
# ]
# ///

import asyncio
import logging
import sys

from auth_manager import AuthManager
from config_manager import ConfigManager
from db_manager import DatabaseManager
from data_processor import DataProcessor
from web_manager import WebManager
from ble_manager import BLEManager
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
        password=static_config['auth']['password'],
    )
    try:
        await auth.login()
    except Exception as e:
        logger.error(f"[Auth] Login failed: {e}. Cannot start without authentication.")
        sys.exit(1)

    config_ready_event = asyncio.Event()

    web_out_queue = asyncio.Queue()
    web_violation_queue = asyncio.Queue()

    web_manager = WebManager(
        static_config,
        db,
        web_out_queue,
        web_violation_queue,
        auth,
        config_ready_event,
    )

    server_task = asyncio.create_task(
        web_manager.run_local_server(), name="WebServer"
    )

    logger.info("[Main] Local server started. Waiting for /api/config from webapp...")
    await config_ready_event.wait()
    logger.info("[Main] /api/config received - continuing boot.")

    sensors = await db.get_sensors()

    if not sensors:
        logger.warning("[Main] No sensors found in DB after config seed. "
                       "BLE/Processor tasks will not be started.")

    processor = DataProcessor(db, web_out_queue, web_violation_queue)

    tasks: list[asyncio.Task[Any]] = [
        server_task,
        asyncio.create_task(web_manager.run_outgoing_data_worker(), name="WebOutgoingData"),
        asyncio.create_task(web_manager.run_outgoing_violation_worker(), name="WebOutgoingViolation"),
        asyncio.create_task(web_manager.run_outgoing_status_worker(), name="WebOutgoingStatus"),
        asyncio.create_task(web_manager.run_offline_sync_worker(), name="WebSync"),
    ]

    queues: dict[str, dict[str, asyncio.Queue]] = {
        sensor['name']: {
            'proc': asyncio.Queue(),
            'inbox': asyncio.Queue(),
        }
        for sensor in sensors
    }

    ble_managers: dict[str, BLEManager] = {
        sensor['name']: BLEManager(
            db,
            sensor,
            queues[sensor['name']]['proc'],
            queues[sensor['name']]['inbox'],
            web_manager.status_queue,
        )
        for sensor in sensors
    }

    web_manager.ble_managers = ble_managers

    for sensor_name, ble_manager in ble_managers.items():
        tasks.append(asyncio.create_task(
            ble_manager.run(),
            name=f"BLE:{sensor_name}",
        ))
        tasks.append(asyncio.create_task(
            processor.run(
                sensor_name,
                queues[sensor_name]['proc'],
                queues[sensor_name]['inbox'],
            ),
            name=f"Proc:{sensor_name}",
        ))

    try:
        await asyncio.gather(*tasks)
    except asyncio.CancelledError:
        logger.info("All tasks cancelled. Exiting cleanly.")
    finally:
        await db.close()
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
            logging.StreamHandler(sys.stdout),
        ],
    )

    try:
        asyncio.run(main(static_config))
    except KeyboardInterrupt:
        logging.info("Gateway shutdown requested by user.")
