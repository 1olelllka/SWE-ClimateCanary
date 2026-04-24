# /// script
# requires-python = ">=3.9"
# dependencies = [
#     "aiosqlite>=0.20.0",
#     "pyyaml>=6.0",
#     "bleak>=0.21.0",
#     "aiohttp>=3.9.0"
# ]
# ///

import asyncio
import json
import random
import logging
import sys
from config_manager import ConfigManager
from db_manager import DatabaseManager
from data_processor import DataProcessor
from web_manager import WebManager

async def arduino_loop(queue):
    """Simulates the physical Arduino sending data over BLE."""
    while True:
        # Normal operating ranges
        temp = random.uniform(20.0, 24.0)
        moist = random.uniform(30.0, 45.0)
        co2 = random.uniform(400.0, 600.0)

        # 15% chance to trigger a limit violation spike
        if random.random() < 0.15:
            spike = random.choice(["T", "M", "C"])
            if spike == "T": temp = 35.5
            elif spike == "M": moist = 85.0
            else: co2 = 2500.0

        payload = {
            "temperature": round(temp, 2),
            "moisture": round(moist, 2),
            "co2": round(co2, 2)
        }
        
        await queue.put(json.dumps(payload))
        await asyncio.sleep(2) # Send every 2 seconds

async def ble_inbox_monitor(inbox):
    """Prints what the Arduino would receive (LED alerts, etc)."""
    while True:
        msg = await inbox.get()
        print(f">>> [Arduino Simulation] Physical LED/Display would show: {msg}")
        inbox.task_done()

async def start_sim():
    # Use the real config so it talks to the actual webapp
    try:
        config = ConfigManager.load("./config.yaml")
    except:
        config = ConfigManager.load("config.yaml")

    db = DatabaseManager(config['paths']['database'])
    await db.init_db()

    proc_q = asyncio.Queue()
    web_q = asyncio.Queue()
    ble_in = asyncio.Queue()

    processor = DataProcessor(db, config, proc_q, web_q, ble_in)
    web_m = WebManager(config, db, web_q)

    # Run the core system tasks + the simulator
    tasks = [
        asyncio.create_task(web_m.run_outgoing_worker()),
        asyncio.create_task(web_m.run_local_server()),
        asyncio.create_task(processor.run()),
        asyncio.create_task(arduino_loop(proc_q)),
        asyncio.create_task(ble_inbox_monitor(ble_in))
    ]

    print(f"Simulator Started. Target Webapp: {config['webapp']['api_url']}")
    await asyncio.gather(*tasks)

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
    try:
        asyncio.run(start_sim())
    except KeyboardInterrupt:
        print("\nSimulator stopped.")
