import asyncio
import logging

logger = logging.getLogger(__name__)

class DataProcessor:
    def __init__(self, db, config, processing_queue, web_out_queue):
        self.db = db
        self.config = config
        self.processing_queue = processing_queue
        self.web_out_queue = web_out_queue

    async def run(self):
        logger.info("[Processor] Dummy worker started. Waiting for Arduino data...")
        
        while True:
            raw_data = await self.processing_queue.get()
            
            try:
                logger.debug(f"[Processor] Received raw data: {raw_data}")
                
                # DUMMY LOGIC 
                processed_data = {
                    "device": self.config['ble']['target_name'],
                    "raw_payload": raw_data,
                    "status": "unprocessed_dummy_data"
                }
                
                await self.web_out_queue.put(processed_data)
                
            except asyncio.CancelledError:
                raise
            except Exception as e:
                logger.error(f"[Processor] Error handling data: {e}")
                
            finally:
                self.processing_queue.task_done()
