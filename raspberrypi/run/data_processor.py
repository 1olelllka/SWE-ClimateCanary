import asyncio
import logging
import json
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

VIOLATION_THRESHOLD = 4

# Each sensor key maps to (limit_db_key, direction, measurement_type)
# direction: 'max' = violation when value > limit, 'min' = violation when value < limit
SENSOR_CHECKS = [
    ("temperature", "max_temp",     "max", "TEMPERATURE"),
    ("temperature", "min_temp",     "min", "TEMPERATURE"),
    ("moisture",    "max_moisture", "max", "HUMIDITY"),
    ("moisture",    "min_moisture", "min", "HUMIDITY"),
    ("co2",         "max_co2",      "max", "CO2"),
]

def _warning_status(actual: float, limit: float, direction: str) -> str:
    """
    Returns RED if the value exceeds the limit by more than 20%, ORANGE otherwise.
    Works for both 'max' (over-limit) and 'min' (under-limit) directions.
    """
    if direction == "max":
        return "RED" if actual > limit * 1.20 else "ORANGE"
    else:
        # under-limit: violation when actual < limit
        # 20% below the limit => actual < limit * 0.80
        return "RED" if actual < limit * 0.80 else "ORANGE"

def _violation_message(sensor_key: str, direction: str, actual: float, limit: float, status: str) -> str:
    direction_word = "above" if direction == "max" else "below"
    return (
        f"{sensor_key.capitalize()} {direction_word} limit: "
        f"{actual} ({'>' if direction == 'max' else '<'} {limit}) [{status}]"
    )


class DataProcessor:

    def __init__(self, db, web_out_queue, web_violation_queue):
        self.db = db
        self.web_out_queue = web_out_queue
        self.web_violation_queue = web_violation_queue

        self._bad_streak:  dict[str, dict[str, int]] = {}
        self._good_streak: dict[str, dict[str, int]] = {}

    def _init_sensor_state(self, sensor_name: str):
        if sensor_name not in self._bad_streak:
            limit_keys = {limit_key for _, limit_key, _, _ in SENSOR_CHECKS}
            self._bad_streak[sensor_name]  = {k: 0 for k in limit_keys}
            self._good_streak[sensor_name] = {k: 0 for k in limit_keys}

    async def run(self, sensor_name: str, processing_queue: asyncio.Queue):
        logger.info(f"[Processor:{sensor_name}] Worker started.")
        self._init_sensor_state(sensor_name)

        while True:
            raw_data = await processing_queue.get()

            try:
                if isinstance(raw_data, bytes):
                    raw_data = raw_data.decode('utf-8')
                data = json.loads(raw_data)

                limits = await self.db.get_all_limits()
                room_id = await self.db.get_config('room_id')

                current_occ = limits.get('current_occupancy', 0)
                max_occ = limits.get('max_occupancy')

                if max_occ is not None and current_occ > max_occ:
                    logger.warning(f"[Processor:{sensor_name}] Occupancy exceeded ({current_occ}/{max_occ}). Discarding.")
                    continue

                time_base = data.get('time_base')
                offset_ms = data.get('millis_offset')

                if time_base and offset_ms:
                    timestamp = (datetime.fromisoformat(time_base) + timedelta(milliseconds=int(offset_ms))).isoformat()
                else:
                    timestamp = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

                await self._check_violations(sensor_name, data, limits, room_id, timestamp)

                await self.db.insert_measurement(
                    sensor_name=sensor_name,
                    temp=data.get('temperature'),
                    moisture=data.get('moisture'),
                    co2=data.get('co2'),
                    timestamp=timestamp
                )

                webapp_payload = {
                    "roomId": room_id,
                    "device": sensor_name,
                    "timestamp": timestamp,
                    "readings": []
                }
                if data.get('temperature') is not None:
                    webapp_payload["readings"].append({"type": "TEMPERATURE", "value": data['temperature']})
                if data.get('moisture') is not None:
                    webapp_payload["readings"].append({"type": "HUMIDITY", "value": data['moisture']})
                if data.get('co2') is not None:
                    webapp_payload["readings"].append({"type": "CO2", "value": data['co2']})

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
        room_id: str,
        timestamp: str,
    ):
        any_newly_resolved = False

        for sensor_key, limit_key, direction, measurement_type in SENSOR_CHECKS:
            val = data.get(sensor_key)
            limit = limits.get(limit_key)

            if val is None or limit is None:
                continue

            over_limit = (val > limit) if direction == 'max' else (val < limit)

            if over_limit:
                self._bad_streak[sensor_name][limit_key] += 1
                self._good_streak[sensor_name][limit_key] = 0

                bad = self._bad_streak[sensor_name][limit_key]
                logger.debug(
                    f"[Processor:{sensor_name}] {sensor_key} {'above' if direction == 'max' else 'below'} "
                    f"{direction} limit: {val} {'>' if direction == 'max' else '<'} {limit} (streak {bad}/{VIOLATION_THRESHOLD})"
                )

                if bad == VIOLATION_THRESHOLD:
                    self._good_streak[sensor_name][limit_key] = 0
                    await self.db.register_violation(sensor_name, limit_key, limit, val)

                    status = _warning_status(val, limit, direction)
                    message = _violation_message(sensor_key, direction, val, limit, status)

                    violation_report = {
                        "type": "violation_warning",
                        "roomId": room_id,
                        "device": sensor_name,
                        "sensor_name": sensor_name,
                        "measurement_type": measurement_type,
                        "limit_key": limit_key,
                        "status": status,
                        "triggeredValue": val,
                        "activeLimitAtTime": limit,
                        "message": message,
                        "timestamp": timestamp,
                    }
                    await self.web_violation_queue.put(violation_report)

                    logger.warning(
                        f"[Processor:{sensor_name}] Violation confirmed for {limit_key}: "
                        f"{val} {'>' if direction == 'max' else '<'} {limit} "
                        f"after {VIOLATION_THRESHOLD} consecutive readings. Status={status}"
                    )

                elif bad > VIOLATION_THRESHOLD:
                    self._bad_streak[sensor_name][limit_key] = VIOLATION_THRESHOLD

            else:
                self._good_streak[sensor_name][limit_key] += 1
                self._bad_streak[sensor_name][limit_key]   = 0

                good = self._good_streak[sensor_name][limit_key]

                if good == VIOLATION_THRESHOLD:
                    active = await self.db.get_active_violations(sensor_name)
                    active_keys = {v['type'] for v in active}

                    if limit_key in active_keys:
                        await self.db.resolve_violation(sensor_name, limit_key)
                        any_newly_resolved = True

                        resolve_report = {
                            "type": "violation_resolve",
                            "roomId": room_id,
                            "device": sensor_name,
                            "sensor_name": sensor_name,
                            "measurement_type": measurement_type,
                            "limit_key": limit_key,
                            "status": "GREEN",
                            "triggeredValue": val,
                            "activeLimitAtTime": limit,
                            "message": f"{sensor_key.capitalize()} back within limits.",
                            "timestamp": timestamp,
                        }
                        await self.web_violation_queue.put(resolve_report)

                        logger.info(
                            f"[Processor:{sensor_name}] Violation for {limit_key} resolved "
                            f"after {VIOLATION_THRESHOLD} consecutive clean readings."
                        )

                elif good > VIOLATION_THRESHOLD:
                    self._good_streak[sensor_name][limit_key] = VIOLATION_THRESHOLD

        if any_newly_resolved:
            remaining = await self.db.get_active_violations(sensor_name)
            if not remaining:
                logger.info(f"[Processor:{sensor_name}] All violations resolved.")
