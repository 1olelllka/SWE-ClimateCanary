import asyncio
import json
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from app.data_processor import DataProcessor, _warning_status, _violation_message, VIOLATION_THRESHOLD


# shared fixtures 

SENSOR = "S1"
WRITE_ID = "write-uuid-1"

GOOD_LIMITS = {
    "max_temp": 30.0, "min_temp": 18.0,
    "max_moisture": 70.0, "min_moisture": 30.0,
    "max_co2": 1000.0,
    "privacy_mode": 0.0,
}

GOOD_CONFIG = {"room_id": "room-1", "frequency": "60"}

GOOD_DATA = json.dumps({
    "temperature": 22.0,
    "humidity": 50.0,
    "iaq": 600.0,
})


@pytest.fixture
def mock_db():
    db = AsyncMock()
    db.get_all_limits.return_value = dict(GOOD_LIMITS)
    db.get_all_config.return_value = dict(GOOD_CONFIG)
    db.get_active_violations.return_value = []
    return db


@pytest.fixture
def queues():
    return {
        "out": asyncio.Queue(),
        "violation": asyncio.Queue(),
    }


@pytest.fixture
def processor(mock_db, queues):
    return DataProcessor(mock_db, queues["out"], queues["violation"])


async def run_one(processor, raw, sensor=SENSOR, write_id=WRITE_ID):
    """Push one message through run(), then cancel the loop."""
    q = asyncio.Queue()
    await q.put(raw)
    task = asyncio.create_task(processor.run(sensor, q, write_id))
    await q.join()          # wait until task_done() is called
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass


# pure helper function tests 

class TestWarningStatus:
    def test_max_green(self):
        assert _warning_status(30.0, 30.0, "max") == "GREEN" # exactly at limit

    def test_max_yellow(self):
        assert _warning_status(33.1, 30.0, "max") == "YELLOW"  # > 110% (33.0), < 125% (37.5)

    def test_max_red(self):
        assert _warning_status(38.0, 30.0, "max") == "RED" # > 125%

    def test_min_green(self):
        assert _warning_status(18.0, 18.0, "min") == "GREEN" # exactly at limit

    def test_min_yellow(self):
        assert _warning_status(16.0, 18.0, "min") == "YELLOW" # < 90%

    def test_min_red(self):
        assert _warning_status(12.0, 18.0, "min") == "RED" # < 75%


class TestViolationMessage:
    def test_max_direction(self):
        msg = _violation_message("temperature", "max", 35.0, 30.0, "YELLOW")
        assert "above" in msg
        assert "35.0" in msg
        assert "30.0" in msg
        assert "YELLOW" in msg

    def test_min_direction(self):
        msg = _violation_message("humidity", "min", 20.0, 30.0, "RED")
        assert "below" in msg
        assert "RED" in msg


# DataProcessor._init_sensor_state 

class TestInitSensorState:
    def test_initialises_streaks_to_zero(self, processor):
        processor._init_sensor_state(SENSOR)
        for key in ("max_temp", "min_temp", "max_moisture", "min_moisture", "max_co2"):
            assert processor._bad_streak[SENSOR][key] == 0
            assert processor._good_streak[SENSOR][key] == 0

    def test_does_not_reset_existing_state(self, processor):
        processor._init_sensor_state(SENSOR)
        processor._bad_streak[SENSOR]["max_temp"] = 3
        processor._init_sensor_state(SENSOR)   # called again
        assert processor._bad_streak[SENSOR]["max_temp"] == 3


# DataProcessor.run - message handling 

class TestRunMessageHandling:
    async def test_normal_measurement_stored_and_forwarded(self, processor, mock_db, queues):
        await run_one(processor, GOOD_DATA)

        mock_db.insert_measurement.assert_awaited_once()
        assert not queues["out"].empty()
        payload = queues["out"].get_nowait()
        assert payload["roomId"] == "room-1"
        assert payload["device"] == SENSOR
        assert any(r["type"] == "TEMPERATURE" for r in payload["readings"])
        assert any(r["type"] == "HUMIDITY"    for r in payload["readings"])
        assert any(r["type"] == "CO2"         for r in payload["readings"])

    async def test_bytes_input_decoded_correctly(self, processor, mock_db, queues):
        await run_one(processor, GOOD_DATA.encode("utf-8"))
        mock_db.insert_measurement.assert_awaited_once()

    async def test_malformed_json_does_not_crash(self, processor, mock_db):
        await run_one(processor, "not-json{{{")
        mock_db.insert_measurement.assert_not_called()

    async def test_discards_when_room_id_missing(self, processor, mock_db, queues):
        mock_db.get_all_config.return_value = {"room_id": None, "frequency": "60"}
        await run_one(processor, GOOD_DATA)
        mock_db.insert_measurement.assert_not_called()
        assert queues["out"].empty()

    async def test_discards_when_frequency_missing(self, processor, mock_db, queues):
        mock_db.get_all_config.return_value = {"room_id": "room-1", "frequency": None}
        await run_one(processor, GOOD_DATA)
        mock_db.insert_measurement.assert_not_called()

    async def test_discards_when_limit_is_null(self, processor, mock_db, queues):
        limits = dict(GOOD_LIMITS)
        limits["max_temp"] = None
        mock_db.get_all_limits.return_value = limits
        await run_one(processor, GOOD_DATA)
        mock_db.insert_measurement.assert_not_called()

    async def test_privacy_mode_skips_storage_but_checks_violations(self, processor, mock_db, queues):
        limits = dict(GOOD_LIMITS)
        limits["privacy_mode"] = 1.0
        mock_db.get_all_limits.return_value = limits
        await run_one(processor, GOOD_DATA)
        mock_db.insert_measurement.assert_not_called()
        assert queues["out"].empty()

    async def test_timestamp_computed_from_time_base_and_offset(self, processor, mock_db, queues):
        data = json.dumps({
            "temperature": 22.0, "humidity": 50.0, "iaq": 600.0,
            "time_base": "2026-01-01T10:00:00+01:00",
            "millis_offset": 5000,
        })
        await run_one(processor, data)
        call_kwargs = mock_db.insert_measurement.call_args.kwargs
        # 5000 ms = 5 seconds added to 10:00:00
        assert "10:00:05" in call_kwargs["timestamp"]

    async def test_readings_only_include_non_none_values(self, processor, mock_db, queues):
        data = json.dumps({"temperature": 22.0})   # no humidity or iaq
        await run_one(processor, data)
        payload = queues["out"].get_nowait()
        types = [r["type"] for r in payload["readings"]]
        assert types == ["TEMPERATURE"]


# DataProcessor._check_violations - streak logic 

class TestCheckViolations:

    def _make_limits(self, **overrides):
        return {**GOOD_LIMITS, **overrides}

    async def test_bad_streak_increments_on_over_limit(self, processor):
        processor._init_sensor_state(SENSOR)
        limits = self._make_limits(max_temp=20.0)   # 22 > 20 → over limit
        data = {"temperature": 22.0}
        await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert processor._bad_streak[SENSOR]["max_temp"] == 1

    async def test_violation_fired_exactly_at_threshold(self, processor, mock_db, queues):
        processor._init_sensor_state(SENSOR)
        limits = self._make_limits(max_temp=20.0)
        data = {"temperature": 22.0}

        for i in range(VIOLATION_THRESHOLD - 1):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
            assert queues["violation"].empty(), f"Should not fire before threshold (call {i+1})"

        await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert not queues["violation"].empty()
        report = queues["violation"].get_nowait()
        assert report["type"] == "violation_warning"
        assert report["limit_key"] == "max_temp"

    async def test_bad_streak_capped_at_threshold(self, processor, mock_db):
        processor._init_sensor_state(SENSOR)
        limits = self._make_limits(max_temp=20.0)
        data = {"temperature": 22.0}
        for _ in range(VIOLATION_THRESHOLD + 5):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert processor._bad_streak[SENSOR]["max_temp"] == VIOLATION_THRESHOLD

    async def test_good_reading_resets_bad_streak(self, processor):
        processor._init_sensor_state(SENSOR)
        processor._bad_streak[SENSOR]["max_temp"] = 3
        limits = self._make_limits(max_temp=30.0)
        data = {"temperature": 22.0}   # within limit
        await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert processor._bad_streak[SENSOR]["max_temp"] == 0

    async def test_resolve_fired_after_threshold_clean_readings(self, processor, mock_db, queues):
        processor._init_sensor_state(SENSOR)
        mock_db.get_active_violations.return_value = [{"type": "max_temp"}]
        limits = self._make_limits(max_temp=30.0)
        data = {"temperature": 22.0}   # within limit

        for i in range(VIOLATION_THRESHOLD - 1):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
            assert queues["violation"].empty(), f"Should not resolve before threshold (call {i+1})"

        await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert not queues["violation"].empty()
        report = queues["violation"].get_nowait()
        assert report["type"] == "violation_resolve"
        assert report["limit_key"] == "max_temp"
        mock_db.resolve_violation.assert_awaited_once_with(SENSOR, "max_temp")

    async def test_resolve_not_fired_if_no_active_violation(self, processor, mock_db, queues):
        processor._init_sensor_state(SENSOR)
        mock_db.get_active_violations.return_value = []  # nothing active
        limits = self._make_limits(max_temp=30.0)
        data = {"temperature": 22.0}
        for _ in range(VIOLATION_THRESHOLD):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert queues["violation"].empty()

    async def test_good_streak_capped_at_threshold(self, processor, mock_db):
        processor._init_sensor_state(SENSOR)
        mock_db.get_active_violations.return_value = []
        limits = self._make_limits(max_temp=30.0)
        data = {"temperature": 22.0}
        for _ in range(VIOLATION_THRESHOLD + 5):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert processor._good_streak[SENSOR]["max_temp"] == VIOLATION_THRESHOLD

    async def test_min_limit_violation(self, processor, mock_db, queues):
        processor._init_sensor_state(SENSOR)
        limits = self._make_limits(min_temp=20.0)
        data = {"temperature": 15.0}   # 15 < 20 → violation
        for _ in range(VIOLATION_THRESHOLD):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        report = queues["violation"].get_nowait()
        assert report["limit_key"] == "min_temp"

    async def test_missing_sensor_value_skipped(self, processor, mock_db, queues):
        processor._init_sensor_state(SENSOR)
        limits = self._make_limits()
        data = {}   # no temperature, humidity, iaq
        await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        assert queues["violation"].empty()
        assert processor._bad_streak[SENSOR]["max_temp"] == 0

    async def test_violation_report_contains_correct_fields(self, processor, mock_db, queues):
        processor._init_sensor_state(SENSOR)
        limits = self._make_limits(max_temp=20.0)
        data = {"temperature": 38.0}   # > 125% of 20 → RED
        for _ in range(VIOLATION_THRESHOLD):
            await processor._check_violations(SENSOR, data, limits, "room-1", "ts", WRITE_ID)
        report = queues["violation"].get_nowait()
        assert report["status"] == "RED"
        assert report["triggeredValue"] == 38.0
        assert report["activeLimitAtTime"] == 20.0
        assert report["measurement_type"] == "TEMPERATURE"
        assert report["sensorWriteId"] == WRITE_ID
        assert report["roomId"] == "room-1"
