# /// script
# requires-python = ">=3.9"
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
import signal
import sys

from config_manager import ConfigManager
from db_manager import DatabaseManager
from data_processor import DataProcessor
from web_manager import WebManager
from ble_manager import BLEManager
from mock_data import MockDataGenerator

logger = logging.getLogger(__name__)

async def main(config):
    logger.info("Starting IoT Gateway")
    
    db = DatabaseManager(config['paths']['database'])
    await db.init_db()
    
    processing_queue = asyncio.Queue()
    web_out_queue = asyncio.Queue()
    violation_out_queue = asyncio.Queue() # New queue for urgent alerts
    ble_inbox = asyncio.Queue()
    
    processor = DataProcessor(db, config, processing_queue, web_out_queue, violation_out_queue, ble_inbox)
    web_manager = WebManager(config, db, web_out_queue, violation_out_queue)
    ble_manager = BLEManager(config, db, processing_queue, ble_inbox)
    # mock_gen = MockDataGenerator(config, processing_queue)

    tasks = [
        asyncio.create_task(web_manager.run_local_server(), name="WebServer"),
        asyncio.create_task(web_manager.run_outgoing_worker(), name="WebOutgoing"),
        asyncio.create_task(web_manager.run_violation_worker(), name="WebViolation"),
        asyncio.create_task(web_manager.run_offline_sync_worker(), name="WebSync"),
        asyncio.create_task(processor.run(), name="DataProcessor"),
        asyncio.create_task(ble_manager.run(), name="BLEConnection"),
        # asyncio.create_task(mock_gen.run(), name="MockDataGenerator")
    ]

    try:
        await asyncio.gather(*tasks)
    except asyncio.CancelledError:
        logger.info("All tasks cancelled. Exiting cleanly.")
    finally:
        logger.info("Gateway Stopped")


if __name__ == "__main__":
    try:
        config = ConfigManager.load()
    except Exception as e:
        print(f"Failed to load config: {e}")
        sys.exit(1)
        
    # Ensure log directory exists
    log_file = config['paths'].get('log_file', 'logs/gateway.log')
    os.makedirs(os.path.dirname(log_file), exist_ok=True)

    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file),
            logging.StreamHandler(sys.stdout)
        ]
    )
    
    try:
        asyncio.run(main(config))
    except KeyboardInterrupt:
        logging.info("Gateway shutdown requested by user via terminal.")
    except Exception as e:
        logging.critical(f"Unexpected crash in main loop: {e}", exc_info=True)
        sys.exit(1)
