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

                logger.info(f"[BLE] Looking for '{target_name}'...")

                self.disconnect_event.clear()

                device = await BleakScanner.find_device_by_filter(
                    lambda d, _: d.name and target_name in d.name,
                    timeout=15.0
                )

                if not device:
                    logger.warning(f"[BLE] '{target_name}' not found. Retrying in 10s...")
                    await asyncio.sleep(10)
                    continue

                logger.info(f"[BLE] Found '{target_name}'. Connecting...")

                async with BleakClient(
                    device,
                    timeout=30.0,
                    disconnected_callback=lambda _: self.disconnected_callback()
                ) as client:

                    self.client = client
                    self.disconnect_event.clear()

                    logger.info("[BLE] Successfully connected to Arduino!")
                    await self.db.log_event("BLE", f"Connected to {target_name}", "INFO")

                    await client.start_notify(char_uuid, self.notification_handler)

                    # Send current timestamp to Arduino immediately after connection
                    # TODO send frequency, should each arduino have individual frequency 
                    unix_ts = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()
                    time_command = f"TIME:{unix_ts}"
                    logger.info(f"[BLE] Sending time sync to Arduino: {time_command}")
                    await client.write_gatt_char(write_uuid, time_command.encode('utf-8'), response=False)

                    sender_task = asyncio.create_task(self._sender_task(write_uuid))

                    await self.disconnect_event.wait()

                    sender_task.cancel()
                    self.client = None

            except BleakError as e:
                logger.error(f"[BLE] Connection error: {e!r}")
                await self.db.log_event("BLE", f"Connection error: {e}", "ERROR")
                await asyncio.sleep(5)

            except Exception as e:
                logger.error(f"[BLE] Unexpected error: {e!r}")
                await asyncio.sleep(10)
