import asyncio
import logging
import json
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

VIOLATION_THRESHOLD = 4  # consecutive bad/good readings required to trigger or resolve a violation

SENSOR_CHECKS = [
    # (json_field, db_limit_key, direction, webapp_measurement_type)
    ("temperature", "max_temp",     "max", "TEMPERATURE"),
    ("temperature", "min_temp",     "min", "TEMPERATURE"),
    ("humidity",    "max_moisture", "max", "HUMIDITY"),
    ("humidity",    "min_moisture", "min", "HUMIDITY"),
    ("co2",         "max_co2",      "max", "IAQ"),
]

def _warning_status(actual: float, limit: float, direction: str) -> str:
    """Classify a violation as GREEN / YELLOW / RED based on how far the value exceeds the limit.

    For 'max' limits: >25% over → RED, >10% → YELLOW, otherwise GREEN.
    For 'min' limits: <75% of limit → RED, <90% → YELLOW, otherwise GREEN.
    """
    if direction == "max":
        return "RED" if actual > limit * 1.25 else "YELLOW" if actual > limit * 1.10 else "GREEN"
    else:
        return "RED" if actual < limit * 0.75 else "YELLOW" if actual < limit * 0.90 else "GREEN"

def _violation_message(sensor_key: str, direction: str, actual: float, limit: float, status: str) -> str:
    """Build a human-readable violation description for the webapp payload."""
    direction_word = "above" if direction == "max" else "below"
    return (
        f"{sensor_key.capitalize()} {direction_word} limit: "
        f"{actual} ({'>' if direction == 'max' else '<'} {limit}) [{status}]"
    )

class DataProcessor:
    """Parses raw Arduino readings, stores them locally, and detects limit violations."""

    def __init__(self, db, web_out_queue, web_violation_queue):
        self.db = db
        self.web_out_queue = web_out_queue           # -> WebManager measurement worker
        self.web_violation_queue = web_violation_queue  # -> WebManager violation worker

        self._bad_streak:  dict[str, dict[str, int]] = {}
        self._good_streak: dict[str, dict[str, int]] = {}
        # tracks last sent warning status per (sensor_name, limit_key) -> "GREEN"|"YELLOW"|"RED"|None
        self._active_warning: dict[str, dict[str, str | None]] = {}

    def _init_sensor_state(self, sensor_name: str):
        """Lazily initialise streak and warning-state dicts for a sensor on first use."""
        if sensor_name not in self._bad_streak:
            limit_keys = {limit_key for _, limit_key, _, _ in SENSOR_CHECKS}
            self._bad_streak[sensor_name]  = {k: 0 for k in limit_keys}
            self._good_streak[sensor_name] = {k: 0 for k in limit_keys}
            self._active_warning[sensor_name] = {k: None for k in limit_keys}

    def reset_sensor_state(self, sensor_name: str):
        """Reset all violation streaks for a sensor (called on sensor re-add or room change)."""
        limit_keys = {limit_key for _, limit_key, _, _ in SENSOR_CHECKS}
        self._bad_streak[sensor_name]     = {k: 0    for k in limit_keys}
        self._good_streak[sensor_name]    = {k: 0    for k in limit_keys}
        self._active_warning[sensor_name] = {k: None for k in limit_keys}

    async def run(self, sensor_name: str, processing_queue: asyncio.Queue, sensorWriteId):
        """Main processing loop for one sensor.

        Reads raw JSON from processing_queue, validates config, computes the accurate
        timestamp, checks violations, stores the measurement in the DB, and forwards
        the payload to web_out_queue. Discards readings silently while privacy_mode is active.
        """
        logger.info(f"[Processor:{sensor_name}] Worker started.")
        self._init_sensor_state(sensor_name)

        while True:
            try:
                raw_data = await processing_queue.get()
            except asyncio.CancelledError:
                logger.info(f"[Processor:{sensor_name}] Worker stopped.")
                return

            try:
                limits = await self.db.get_all_limits()
                config = await self.db.get_all_config()
                room_id = config.get('room_id')
                freq = config.get('frequency')

                if room_id is None or freq is None:
                    logger.warning(
                        f"[Processor:{sensor_name}] Config has null values (room_id={room_id}, frequency={freq}) - discarding measurement until valid config arrives."
                    )
                    continue

                null_limits = [k for k in ('max_temp','min_temp','max_moisture','min_moisture','max_co2') if limits.get(k) is None]
                if null_limits:
                    logger.warning(
                        f"[Processor:{sensor_name}] Limits have null values {null_limits} - discarding measurement until valid config arrives."
                    )
                    continue

                if isinstance(raw_data, bytes):
                    raw_data = raw_data.decode('utf-8')
                data = json.loads(raw_data)

                # Reconstruct the precise sample timestamp from the Arduino's millis offset.
                # Falls back to current time when the Arduino sends no clock fields.
                time_base = data.get('time_base')
                offset_ms = data.get('millis_offset')

                if time_base and offset_ms:
                    timestamp = (datetime.fromisoformat(time_base) + timedelta(milliseconds=int(offset_ms))).isoformat()
                else:
                    timestamp = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

                privacy = limits.get('privacy_mode', 1.0)

                if privacy:
                    logger.warning(f"[Processor:{sensor_name}] Privacy Mode active. Discarding.")
                    # Still check violations so alerts fire even without storing the raw value.
                    await self._check_violations(sensor_name, data, limits, room_id, timestamp, sensorWriteId)
                    continue

                logger.info(f"[Arduino -> Pi] Received: {raw_data}")

                await self._check_violations(sensor_name, data, limits, room_id, timestamp, sensorWriteId)

                await self.db.insert_measurement(
                    sensor_name=sensor_name,
                    temp=data.get('temperature'),
                    moisture=data.get('humidity'),
                    co2=data.get('iaq'),
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
                if data.get('humidity') is not None:
                    webapp_payload["readings"].append({"type": "HUMIDITY", "value": data['humidity']})
                if data.get('iaq') is not None:
                    webapp_payload["readings"].append({"type": "CO2", "value": data['iaq']})
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
        sensorWriteId,
    ):
        """Streak-based violation detector for all SENSOR_CHECKS on a single reading.

        A violation is reported only after VIOLATION_THRESHOLD consecutive bad readings,
        and only when the status level changes (GREEN → YELLOW → RED), suppressing duplicates.
        Resolution is reported after VIOLATION_THRESHOLD consecutive clean readings.
        """
        any_newly_resolved = False

        for sensor_key, limit_key, direction, measurement_type in SENSOR_CHECKS:
            val = data.get(sensor_key)
            limit = limits.get(limit_key)

            if val is None or limit is None:
                continue

            over_limit = (val > limit) if direction == 'max' else (val < limit)

            if over_limit:
                self._bad_streak[sensor_name][limit_key] = min(
                    self._bad_streak[sensor_name][limit_key] + 1, VIOLATION_THRESHOLD
                )
                self._good_streak[sensor_name][limit_key] = 0

                bad = self._bad_streak[sensor_name][limit_key]
                logger.debug(
                    f"[Processor:{sensor_name}] {sensor_key} {'above' if direction == 'max' else 'below'} "
                    f"{direction} limit: {val} {'>' if direction == 'max' else '<'} {limit} (streak {bad}/{VIOLATION_THRESHOLD})"
                )

                if bad >= VIOLATION_THRESHOLD:
                    status = _warning_status(val, limit, direction)
                    last_sent = self._active_warning[sensor_name][limit_key]

                    if last_sent != status:
                        await self.db.register_violation(sensor_name, limit_key, limit, val)

                        message = _violation_message(sensor_key, direction, val, limit, status)
                        violation_report = {
                            "type": "violation_warning",
                            "roomId": room_id,
                            "sensor_name": sensor_name,
                            "limit_key": limit_key,
                            "measurement_type": measurement_type,
                            "status": status,
                            "triggeredValue": val,
                            "activeLimitAtTime": limit,
                            "message": message,
                            "sensorWriteId": sensorWriteId,
                        }
                        await self.web_violation_queue.put(violation_report)
                        self._active_warning[sensor_name][limit_key] = status

                        logger.info(
                            f"[Processor:{sensor_name}] Violation {'escalated to' if last_sent else 'confirmed for'} "
                            f"{limit_key}: {val} {'>' if direction == 'max' else '<'} {limit} "
                            f"[{last_sent or 'new'} -> {status}]"
                        )
                    else:
                        logger.debug(
                            f"[Processor:{sensor_name}] Violation for {limit_key} ongoing at {status}, suppressing duplicate."
                        )

            else:
                self._good_streak[sensor_name][limit_key] += 1
                self._bad_streak[sensor_name][limit_key] = 0

                good = self._good_streak[sensor_name][limit_key]

                if good == VIOLATION_THRESHOLD:
                    active = await self.db.get_active_violations(sensor_name)
                    active_keys = {v['type'] for v in active}

                    if limit_key in active_keys:
                        await self.db.resolve_violation(sensor_name, limit_key)
                        any_newly_resolved = True

                        self._active_warning[sensor_name][limit_key] = None

                        resolve_report = {
                            "type": "violation_resolve",
                            "roomId": room_id,
                            "device": sensor_name,
                            "sensor_name": sensor_name,
                            "measurement_type": measurement_type,
                            "limit_key": limit_key,
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
