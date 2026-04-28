import asyncio
import logging
from bleak import BleakScanner, BleakClient
from bleak.exc import BleakError
from datetime import datetime
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

class BLEManager:
    def __init__(self, db, sensor: dict, processing_queue, ble_inbox):
        self.db = db
        self.sensor = sensor
        self.processing_queue = processing_queue  # Arduino -> Pi
        self.ble_inbox = ble_inbox                # Pi -> Arduino

        self.client = None
        self.disconnect_event = asyncio.Event()
        self.reconnect_event = asyncio.Event()

    @property
    def name(self) -> str:
        return self.sensor['name']

    def disconnected_callback(self):
        """Fired instantly by Bleak if the Arduino loses power or drops connection."""
        logger.warning("[BLE] Arduino disconnected unexpectedly!")
        self.disconnect_event.set()

    def notification_handler(self, data):
        """Triggered automatically whenever the Arduino sends sensor data."""
        message = data.decode('utf-8').strip()
        logger.debug(f"[Arduino -> Pi] Received: {message}")

        # thread-safe, works regardless of which thread Bleak calls this from
        asyncio.get_event_loop().call_soon_threadsafe(
                self.processing_queue.put_nowait, message
                )

    async def _sender_task(self, write_uuid: str):
        """Background task that exclusively handles sending data TO the Arduino."""
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
        """Main orchestrator for the BLE connection."""
        logger.info("[BLE] Starting manager...")

        while True:
            try:
                sensors = await self.db.get_sensors()
                sensor_cfg = next((s for s in sensors if s['name'] == self.name), None)

                if not sensor_cfg:
                    logger.warning(f"[BLE:{self.name}] Sensor removed from config. Stopping.")
                    return

                target_name = sensor_cfg['name']
                char_uuid   = sensor_cfg['char_uuid']
                write_uuid  = sensor_cfg['write_uuid']
                
                connected = False 
                for attempt in range (1,6):
                    logger.info(f"[BLE] Looking for '{target_name}'...")
                    self.disconnect_event.clear()

                    device = await BleakScanner.find_device_by_filter(
                            lambda d, _: d.name and target_name in d.name,
                            timeout=15.0
                            )

                    if not device:
                        logger.warning(f"[BLE] '{target_name}' not found (attempt {attempt}/5))")
                        if attempt < 5:
                            await asyncio.sleep(3)
                        continue

                    logger.info(f"[BLE] Found '{target_name}'. Connecting (attempt {attempt}/5)...")
                    
                    try:
                        async with BleakClient(
                                device,
                                timeout=30.0,
                                disconnected_callback=lambda _: self.disconnected_callback()
                                ) as client:

                            self.client = client
                            self.disconnect_event.clear()
                            connected = True 

                            logger.info(f"[BLE:{self.name}] connected!")
                            await self.db.log_event("BLE", f"Connected to {target_name}", "INFO")

                            await client.start_notify(char_uuid, self.notification_handler)

                            # Send current timestamp to Arduino immediately after connection
                            unix_ts = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()
                            await client.write_gatt_char(
                                write_uuid, f"TIME:{unix_ts}".encode('utf-8'), response=False
                                )
                            logger.info(f"[BLE:{self.name}] Time sync sent.")

                            sender_task = asyncio.create_task(self._sender_task(write_uuid))
                            await self.disconnect_event.wait()
                            sender_task.cancel()
                            self.client = None

                    except BleakError as e:
                        logger.error(f"[BLE:{self.name}] Connection error on attempt {attempt}: {e!r}")
                        await self.db.log_event("BLE", f"Connection error on {target_name}: {e}", "ERROR")
                        if attempt < 5:
                            await asyncio.sleep(5)
                    break

                if not connected:
                    logger.error(f"[BLE:{self.name}] All 5 attempts failed. Waiting for RECONNECT notify...")
                    await self.db.log_event("BLE", f"Failed to connect to {target_name} after 5 attempts", "ERROR")
                    self.reconnect_event.clear()
                    await self.reconnect_event.wait()
                    logger.info(f"[BLE:{self.name}] RECONNECT received. Retrying...")

            except Exception as e:
                logger.error(f"[BLE] Unexpected error: {e!r}")
                await asyncio.sleep(10)
