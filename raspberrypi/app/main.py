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
from db_manager import DatabaseManager
from data_processor import DataProcessor
from web_manager import WebManager
from ble_manager import BLEManager
from typing import Any

logger = logging.getLogger(__name__)


async def main(db_path: str):
    logger.info("Starting IoT Gateway")

    db = DatabaseManager(db_path)
    await db.connect()
    await db.init_db()

    configure_done = await db.get_config('configure_done')
    if configure_done != '1':
        logger.warning(f"[Main] configure_done flag not set, watiting for ./configure script to set initial config.")
        while configure_done != '1':
            await asyncio.sleep(10)
            configure_done = await db.get_config('configure_done')
        logger.info(f"[Main] configure_done detected. Continuing boot.")

    server_url = await db.get_config('server_url')
    username = await db.get_config('auth_username')
    password = await db.get_config('auth_password')
    local_listen_port = int(await db.get_config("local_listen_port") or "8080")

    if not server_url or not username or not password:
        logger.error(f"[Main] server_url, username or password missing from db, restart config.")
        await db.close()
        sys.exit(1)

    auth = AuthManager( server_url=server_url, username=username, password=password)
    
    for attempt in range(1, 6):
        try:
            await auth.login()
            break
        except Exception as e:
            logger.warning(f"[Auth] Login attempt {attempt}/5 failed: {e}")
            if attempt == 5:
                logger.error("[Auth] All 5 login attempts failed. Cannot start without authentication.")
                await db.close()
                sys.exit(1)
            await asyncio.sleep(5)

    initial_config_done = await db.get_config('initial_config_done')
    first_boot = initial_config_done != '1'

    config_ready_event = asyncio.Event()

    if not first_boot:
        logger.info(f"[Main] initial_config_done flag set, skipping wait for /api/config and continuing with existing db config.")
        config_ready_event.set()
    else:
        logger.info(f"[Main] First boot - will wait for initial /api/config from webapp.")

    web_out_queue = asyncio.Queue()
    web_violation_queue = asyncio.Queue()

    web_manager = WebManager(
        db,
        local_listen_port,
        server_url,
        web_out_queue,
        web_violation_queue,
        auth,
        config_ready_event,
        first_boot=first_boot,
    )

    server_task = asyncio.create_task(
        web_manager.run_local_server(), name="WebServer"
    )

    if first_boot:
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
    ]

    queues: dict[str, dict[str, asyncio.Queue]] = {
        sensor['name']: {
            'proc': asyncio.Queue(),
            'inbox': asyncio.Queue(),
        }
        for sensor in sensors
    }

    scan_lock = asyncio.Lock()

    ble_managers: dict[str, BLEManager] = {
        sensor['name']: BLEManager(
            db,
            sensor,
            queues[sensor['name']]['proc'],
            queues[sensor['name']]['inbox'],
            web_manager.status_queue,
            scan_lock,
        )
        for sensor in sensors
    }

    web_manager.ble_managers = ble_managers

    web_manager.processor = processor
    web_manager.queues = queues
    web_manager.scan_lock = scan_lock

    for sensor in sensors:
        sensor_name = sensor['name']
        write_uuid = sensor['write_uuid']
        ble_manager = ble_managers[sensor_name]

        tasks.append(asyncio.create_task(
            ble_manager.run(),
            name=f"BLE:{sensor_name}",
        ))
        tasks.append(asyncio.create_task(
            processor.run(
                sensor_name,
                queues[sensor_name]['proc'],
                write_uuid
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
    
    db_path = "/home/pi/data/production.sqlite"
    log_file = "/home/pi/logs/raspberrypi.log"

    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file),
            logging.StreamHandler(sys.stdout),
        ],
    )

    try:
        asyncio.run(main(db_path))
    except KeyboardInterrupt:
        logging.info("Gateway shutdown requested by user.")
