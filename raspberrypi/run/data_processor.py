import asyncio
import logging
import json
from datetime import datetime
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

VIOLATION_THRESHOLD = 4

SENSOR_LIMIT_MAP = {
    "temperature": "max_temp",
    "moisture":    "max_moisture",
    "co2":         "max_co2",
}

class DataProcessor:
    """Processes incoming sensor data from all Arduinos.
    Each BLEManager puts messages onto its own processing_queue.
    DataProcessor spawns one worker coroutine per queue so each
    Arduino is processed independently.

    Violation debounce:
      - A violation is only fired after VIOLATION_THRESHOLD consecutive
        readings that exceed the limit for the same sensor key.
      - An active violation is only cleared after VIOLATION_THRESHOLD
        consecutive clean readings for that sensor key.
      Both counters are tracked per (sensor_name, sensor_key) in memory.
    """

    def __init__(self, db, web_out_queue, web_violation_queue):
        self.db = db
        self.web_out_queue = web_out_queue
        self.web_violation_queue = web_violation_queue

        # Simpler to keep two separate dicts for clarity, count up to VIOLATION_THRESHOLD:
        self._bad_streak:  dict[str, dict[str, int]] = {}   
        self._good_streak: dict[str, dict[str, int]] = {}  

    def _init_sensor_state(self, sensor_name: str):
        if sensor_name not in self._bad_streak:
            self._bad_streak[sensor_name]  = {k: 0 for k in SENSOR_LIMIT_MAP}
            self._good_streak[sensor_name] = {k: 0 for k in SENSOR_LIMIT_MAP}

    async def run(self, sensor_name: str, processing_queue: asyncio.Queue, ble_inbox: asyncio.Queue):
        """Worker loop for a single Arduino's queue.
        Called once per sensor; main creates one task per sensor.
        """
        logger.info(f"[Processor:{sensor_name}] Worker started.")
        self._init_sensor_state(sensor_name)

        while True:
            raw_data = await processing_queue.get()

            try:
                if isinstance(raw_data, bytes):
                    raw_data = raw_data.decode('utf-8')
                data = json.loads(raw_data)

                limits  = await self.db.get_all_limits()
                tips    = await self.db.get_all_tips()
                room_id = await self.db.get_config('room_id')

                current_occ = limits.get('current_occupancy', 0)
                max_occ     = limits.get('max_occupancy')

                if max_occ is not None and current_occ > max_occ:
                    logger.warning(f"[Processor:{sensor_name}] Occupancy exceeded ({current_occ}/{max_occ}). Discarding.")
                    continue

                timestamp = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()
                if not data.get('timestamp'):
                    data['timestamp'] = timestamp

                await self._check_violations(sensor_name, data, limits, tips, room_id, timestamp, ble_inbox)

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

    async def _check_violations(
        self,
        sensor_name: str,
        data: dict,
        limits: dict,
        tips: dict,
        room_id: str,
        timestamp: str,
        ble_inbox: asyncio.Queue,
    ):
        """violation check.
        Tips are sent to the Arduino BLE inbox whenever a violation is first fired.
        An ALERT:OFF is sent when all violations for this sensor are resolved.
        """
        any_newly_fired    = False
        any_newly_resolved = False

        for sensor_key, limit_key in SENSOR_LIMIT_MAP.items():
            val   = data.get(sensor_key)
            limit = limits.get(limit_key)

            if val is None or limit is None:
                continue

            over_limit = val > limit

            if over_limit:
                self._bad_streak[sensor_name][sensor_key]  += 1
                self._good_streak[sensor_name][sensor_key]  = 0

                bad = self._bad_streak[sensor_name][sensor_key]
                logger.debug(f"[Processor:{sensor_name}] {sensor_key} over limit: {val} > {limit} (streak {bad}/{VIOLATION_THRESHOLD})")

                if bad == VIOLATION_THRESHOLD:
                    await self.db.register_violation(sensor_name, sensor_key, limit, val)

                    violation_report = {
                            "type":            "violation_warning",
                            "device":          sensor_name,
                            "roomId":          room_id,
                            "timestamp":       timestamp,
                            "limit_reached":   sensor_key,
                            "violation_delta": round(val - limit, 2),
                            "actual_value":    val,
                            "threshold":       limit,
                            }
                    await self.web_violation_queue.put(violation_report)
                    any_newly_fired = True

                    tip = tips.get(sensor_key)
                    if tip:
                        await ble_inbox.put(f"TIP:{tip}")
                        logger.info(f"[Processor:{sensor_name}] Tip sent for {sensor_key}: {tip}")

                    logger.warning(
                        f"[Processor:{sensor_name}] Violation confirmed for {sensor_key}: "
                        f"{val} > {limit} after {VIOLATION_THRESHOLD} consecutive readings."
                    )

                elif bad > VIOLATION_THRESHOLD:
                    # Already active - keep counter capped so it doesn't grow unboundedly
                    self._bad_streak[sensor_name][sensor_key] = VIOLATION_THRESHOLD

            else:
                self._good_streak[sensor_name][sensor_key] += 1
                self._bad_streak[sensor_name][sensor_key]   = 0

                good = self._good_streak[sensor_name][sensor_key]

                if good == VIOLATION_THRESHOLD:
                    # Check if there was an active violation to resolve
                    active = await self.db.get_active_violations(sensor_name)
                    active_keys = {v['type'] for v in active}

                    if sensor_key in active_keys:
                        await self.db.resolve_violation(sensor_name, sensor_key)
                        any_newly_resolved = True
                        logger.info(
                            f"[Processor:{sensor_name}] Violation for {sensor_key} resolved "
                            f"after {VIOLATION_THRESHOLD} consecutive clean readings."
                        )

                elif good > VIOLATION_THRESHOLD:
                    self._good_streak[sensor_name][sensor_key] = VIOLATION_THRESHOLD

        # Send ALERT commands once per reading cycle, not per sensor key
        if any_newly_fired:
            await ble_inbox.put(f"ALERT:ON")

        if any_newly_resolved:
            # Only send ALERT:OFF if all violations for this sensor are now cleared
            remaining = await self.db.get_active_violations(sensor_name)
            if not remaining:
                await ble_inbox.put("ALERT:OFF")
                logger.info(f"[Processor:{sensor_name}] All violations resolved — ALERT:OFF sent.")
