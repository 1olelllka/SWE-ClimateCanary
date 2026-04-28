import asyncio
import logging
import json
from datetime import datetime
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

class DataProcessor:
    """Processes incoming sensor data from all Arduinos.
    Each BLEManager puts messages onto its own processing_queue.
    DataProcessor spawns one worker coroutine per queue so each
    Arduino is processed independently.
    """

    def __init__(self, db, web_out_queue, web_violation_queue):
        self.db = db
        self.web_out_queue = web_out_queue
        self.web_violation_queue = web_violation_queue

    async def run(self, sensor_name: str, processing_queue: asyncio.Queue, ble_inbox: asyncio.Queue):
        """Worker loop for a single Arduino's queue.
        Called once per sensor — main creates one task per sensor.
        """
        logger.info(f"[Processor:{sensor_name}] Worker started.")

        while True:
            raw_data = await processing_queue.get()

            try:
                if isinstance(raw_data, bytes):
                    raw_data = raw_data.decode('utf-8')
                data = json.loads(raw_data)

                limits  = await self.db.get_all_limits()
                room_id = await self.db.get_config('room_id')

                current_occ = limits.get('current_occupancy', 0)
                max_occ = limits.get('max_occupancy')

                if max_occ is not None and current_occ > max_occ:
                    logger.warning(f"[Processor:{sensor_name}] Occupancy exceeded ({current_occ}/{max_occ}). Discarding.")
                    continue

                timestamp = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

                if not data.get('timestamp'):
                    data['timestamp'] = timestamp

                sensor_limit_map = {
                    "temperature": "max_temp",
                    "moisture":    "max_moisture",
                    "co2":         "max_co2"
                }

                any_violation = False
                for sensor_key, limit_key in sensor_limit_map.items():
                    val = data.get(sensor_key)
                    limit = limits.get(limit_key)

                    if val is not None and limit is not None and val > limit:
                        any_violation = True

                        await self.db.register_violation(sensor_name, sensor_key, limit, val)

                        violation_report = {
                            "type":            "violation_warning",
                            "device":          sensor_name,
                            "roomId":          room_id,
                            "timestamp":       timestamp,
                            "limit_reached":   sensor_key,
                            "violation_delta": round(val - limit, 2),
                            "actual_value":    val,
                            "threshold":       limit
                        }
                        await self.web_violation_queue.put(violation_report)
                        await ble_inbox.put(f"ALERT:{sensor_key.upper()}")
                        logger.warning(f"[Processor:{sensor_name}] {sensor_key} violation: {val} > {limit}")

                if not any_violation:
                    active_violations = await self.db.get_active_violations(sensor_name)
                    if active_violations:
                        for v in active_violations:
                            await self.db.resolve_violation(sensor_name, v['type'])
                        await ble_inbox.put("ALERT:OFF")
                        logger.info(f"[Processor:{sensor_name}] All violations resolved.")

                await self.db.insert_measurement(
                    sensor_name=sensor_name,
                    temp=data.get('temperature'),
                    moisture=data.get('moisture'),
                    co2=data.get('co2'),
                    timestamp=data.get('timestamp')
                )

                webapp_payload = {
                    "roomId":    room_id,
                    "device":    sensor_name,
                    "timestamp": timestamp,
                    "readings":  []
                }

                if data.get('temperature') is not None:
                    webapp_payload["readings"].append({"type": "TEMPERATURE", "value": data['temperature']})
                if data.get('moisture') is not None:
                    webapp_payload["readings"].append({"type": "HUMIDITY",    "value": data['moisture']})
                if data.get('co2') is not None:
                    webapp_payload["readings"].append({"type": "CO2",         "value": data['co2']})

                await self.web_out_queue.put(webapp_payload)

            except json.JSONDecodeError:
                logger.error(f"[Processor:{sensor_name}] Malformed JSON: {raw_data}")
            except Exception as e:
                logger.error(f"[Processor:{sensor_name}] Unexpected error: {e}", exc_info=True)
            finally:
                processing_queue.task_done()
