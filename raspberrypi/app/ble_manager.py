import asyncio
import logging
from bleak import BleakScanner, BleakClient
from bleak.exc import BleakError
from datetime import datetime
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

class BLEManager:
    def __init__(self, db, sensor: dict, processing_queue, ble_inbox, status_queue, scan_lock: asyncio.Lock):
        self.db = db
        self.sensor = sensor
        self.processing_queue = processing_queue  # Arduino -> Pi
        self.ble_inbox = ble_inbox                # Pi -> Arduino
        self.status_queue = status_queue          # Pi -> WebManager (ONLINE/OFFLINE events)

        self.client = None
        self.disconnect_event = asyncio.Event()
        self.reconnect_event = asyncio.Event()
        self.removal_event = asyncio.Event()
        self._loop = asyncio.get_running_loop()
        self.scan_lock = scan_lock

    @property
    def name(self) -> str:
        return self.sensor['name']

    @property
    def read_uuid(self) -> str:
        return self.sensor['char_uuid']

    def disconnected_callback(self, client):
        logger.warning("[BLE] Arduino disconnected!")
        self.disconnect_event.set()

    def notification_handler(self, sender, data):
        message = data.decode('utf-8').strip()

        # thread-safe, works regardless of which thread Bleak calls this from
        self._loop.call_soon_threadsafe(self.processing_queue.put_nowait, message)

    async def _sender_task(self, write_uuid: str):
        logger.info("[BLE] Sender task started. Ready to transmit commands.")

        while self.client and self.client.is_connected:
            try:
                command = await asyncio.wait_for(self.ble_inbox.get(), timeout=1.0)

                logger.info(f"[Pi -> Arduino] Transmitting: {command}")
                command_bytes = command.encode('utf-8')

                await self.client.write_gatt_char(write_uuid, command_bytes, response=False)
                self.ble_inbox.task_done()

            except asyncio.TimeoutError:
                continue
            except Exception as e:
                logger.error(f"[BLE] Failed to send command to Arduino: {e}")
                self.disconnect_event.set()
                break

    async def run(self):
        logger.info("[BLE] Starting manager...")

        is_reported_offline = False 

        while True:
            try:
                sensors = await self.db.get_sensors()
                sensor_cfg = next((s for s in sensors if s['name'] == self.name), None)

                if not sensor_cfg:
                    logger.warning(f"[BLE:{self.name}] Sensor removed from config. Stopping.")
                    return
                
                self.sensor = sensor_cfg

                target_name = sensor_cfg['name']
                char_uuid = sensor_cfg['char_uuid']
                write_uuid = sensor_cfg['write_uuid']

                connected = False 
                for attempt in range (1,6):

                    sensors = await self.db.get_sensors()
                    sensor_cfg = next((s for s in sensors if s['name'] == self.name), None)
                    if not sensor_cfg:
                        logger.warning(f"[BLE:{self.name}] Sensor removed from config. Stopping.")
                        return

                    self.sensor = sensor_cfg 
                    
                    logger.info(f"[BLE] Looking for '{target_name}'...")
                    self.disconnect_event.clear()

                    async with self.scan_lock:
                        device = await BleakScanner.find_device_by_filter(
                                lambda d, _: d.name and d.name.startswith("G1T4:") and target_name in d.name,
                                timeout=15.0,
                                )

                    if not device:
                        logger.warning(f"[BLE] '{target_name}' not found (attempt {attempt}/5)")
                        if attempt < 5:
                            await asyncio.sleep(3)
                        continue

                    logger.info(f"[BLE] Found '{target_name}'. Connecting (attempt {attempt}/5)...")

                    try:
                        async with BleakClient(
                                device,
                                timeout=30.0,
                                disconnected_callback=lambda client: self.disconnected_callback(client)
                                ) as client:

                            self.client = client
                            self.disconnect_event.clear()

                            logger.info(f"[BLE:{self.name}] connected!")
                            await self.db.log_event("BLE", f"Connected to {target_name}", "INFO")

                            await client.start_notify(char_uuid, self.notification_handler)

                            await self.status_queue.put({
                                "read_uuid": self.read_uuid,
                                "sensor_name": self.name,
                                "status": "ONLINE",
                            })
                            is_reported_offline = False


                            # Send current timestamp and initial frequency to Arduino immediately after connection

                            unix_ts = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()
                            await client.write_gatt_char( write_uuid, f"TIME:{unix_ts}".encode('utf-8'), response=False)
                            logger.info(f"[BLE:{self.name}] Time sync sent.")

                            freq = await self.db.get_config('frequency')
                            if freq is not None:
                                await client.write_gatt_char( write_uuid, f"FREQUENCY:{freq}".encode('utf-8'), response=False)
                                logger.info(f"[BLE:{self.name}] Frequency sync sent.")
                            else:
                                logger.warning(f"[BLE:{self.name}] Frequency is null - skipping frequency sync.")

                            sender_task = asyncio.create_task(self._sender_task(write_uuid))
                            connected = True 
                            _, pending = await asyncio.wait(
                                [
                                    asyncio.create_task(self.disconnect_event.wait()),
                                    asyncio.create_task(self.removal_event.wait()),
                                ],
                                return_when=asyncio.FIRST_COMPLETED
                            )
                            for task in pending:
                                task.cancel()
    
                            sender_task.cancel()
                            self.client = None
    
                            if self.removal_event.is_set():
                                logger.warning(f"[BLE:{self.name}] Sensor deleted from config. Disconnecting.")
                                await self.db.log_event("BLE", f"{target_name} removed - disconnecting.", "INFO")
                                await self.status_queue.put({
                                    "read_uuid": self.read_uuid,
                                    "sensor_name": self.name,
                                    "status": "OFFLINE",
                                })
                                is_reported_offline = True
                                self.removal_event.clear()
                                connected = False 
                            
                            break

                    except BleakError as e:
                        logger.error(f"[BLE:{self.name}] Connection error on attempt {attempt}: {e!r}")
                        await self.db.log_event("BLE", f"Connection error on {target_name}: {e}", "ERROR")
                        if attempt < 5:
                            await asyncio.sleep(5)

                if not connected:
                    logger.error(f"[BLE:{self.name}] All 5 attempts failed. Waiting for RECONNECT notify...")
                    await self.db.log_event("BLE", f"Failed to connect to {target_name} after 5 attempts", "ERROR")

                    if not is_reported_offline:
                        await self.status_queue.put({
                            "read_uuid": self.read_uuid,
                            "sensor_name": self.name,
                            "status": "OFFLINE",
                        })
                        is_reported_offline = True

                    self.reconnect_event.clear()
                    await self.reconnect_event.wait()
                    logger.info(f"[BLE:{self.name}] RECONNECT received. Retrying...")

            except Exception as e:
                logger.error(f"[BLE] Unexpected error: {e!r}")
                await asyncio.sleep(10)
