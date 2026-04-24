import asyncio
import logging
import json
import random
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)

class MockDataGenerator:
    def __init__(self, config, processing_queue):
        self.config = config
        self.processing_queue = processing_queue

    async def generate_history(self, days=30, interval_minutes=60):
        """Generates historical mock data and puts it into the processing queue."""
        logger.info(f"[Mock Data] Starting generation of {days} days of historical data...")
        
        end_time = datetime.now()
        start_time = end_time - timedelta(days=days)
        
        current_time = start_time
        count = 0
        
        while current_time <= end_time:
            # Mimic Arduino JSON format
            data = {
                    "temperature": round(random.uniform(18.0, 26.0), 2),
                    "moisture": round(random.uniform(30.0, 60.0), 2),
                    "co2": round(random.uniform(400.0, 1200.0), 2),
                    "timestamp": current_time.isoformat()
                    }

            message = json.dumps(data)
            await self.processing_queue.put(message)
            
            current_time += timedelta(minutes=interval_minutes)
            count += 1
            
        logger.info(f"[Mock Data] Successfully added {count} mock data points to the processing queue.")

    async def run(self):
        await self.generate_history(days=30, interval_minutes=60)
