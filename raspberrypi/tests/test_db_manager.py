import pytest
import json
from app.db_manager import DatabaseManager


# fixture: fresh in-memory db per test 

@pytest.fixture
async def db():
    manager = DatabaseManager(":memory:")
    await manager.connect()
    await manager.init_db()
    yield manager
    await manager.close()


# connect / init_db 

class TestConnect:
    """connect() and close() manage the SQLite connection lifecycle."""

    async def test_connect_sets_db(self, db):
        assert db.db is not None

    async def test_connect_is_idempotent(self, db):
        original = db.db
        await db.connect() # second call should be a no-op
        assert db.db is original

    async def test_close_sets_db_to_none(self, db):
        await db.close()
        assert db.db is None

    async def test_close_is_idempotent(self, db):
        await db.close()
        await db.close() # should not raise

    async def test_init_db_raises_when_not_connected(self):
        manager = DatabaseManager(":memory:")
        with pytest.raises(RuntimeError, match="not connected"):
            await manager.init_db()


# config 

class TestConfig:
    """Config CRUD: set_config / get_config / get_all_config."""

    async def test_set_and_get_config(self, db):
        await db.set_config("foo", "bar")
        assert await db.get_config("foo") == "bar"

    async def test_get_config_returns_none_for_missing_key(self, db):
        assert await db.get_config("nonexistent") is None

    async def test_set_config_overwrites_existing(self, db):
        await db.set_config("key", "v1")
        await db.set_config("key", "v2")
        assert await db.get_config("key") == "v2"

    async def test_set_config_stores_none_value(self, db):
        await db.set_config("key", None)
        assert await db.get_config("key") is None

    async def test_get_all_config_returns_all_entries(self, db):
        await db.set_config("a", "1")
        await db.set_config("b", "2")
        result = await db.get_all_config()
        assert result["a"] == "1"
        assert result["b"] == "2"

    async def test_get_all_config_empty(self, db):
        assert await db.get_all_config() == {}


# sensors 

SENSOR_A = {"name": "S1", "char_uuid": "char-1", "write_uuid": "write-1"}
SENSOR_B = {"name": "S2", "char_uuid": "char-2", "write_uuid": "write-2"}


class TestSensors:
    """Sensor CRUD: set, add, delete, and flush sensor rows."""

    async def test_get_sensors_empty_by_default(self, db):
        assert await db.get_sensors() == []

    async def test_set_and_get_sensors(self, db):
        await db.set_sensors([SENSOR_A, SENSOR_B])
        result = await db.get_sensors()
        assert len(result) == 2
        assert result[0]["name"] == "S1"
        assert result[1]["name"] == "S2"

    async def test_set_sensors_overwrites(self, db):
        await db.set_sensors([SENSOR_A])
        await db.set_sensors([SENSOR_B])
        result = await db.get_sensors()
        assert len(result) == 1
        assert result[0]["name"] == "S2"

    async def test_add_sensors_appends_new(self, db):
        await db.set_sensors([SENSOR_A])
        await db.add_sensors([SENSOR_B])
        result = await db.get_sensors()
        assert len(result) == 2

    async def test_add_sensors_skips_duplicates(self, db):
        await db.set_sensors([SENSOR_A])
        await db.add_sensors([SENSOR_A])   # same name
        result = await db.get_sensors()
        assert len(result) == 1

    async def test_delete_sensors_by_char_uuid(self, db):
        await db.set_sensors([SENSOR_A, SENSOR_B])
        await db.delete_sensors_by_ids(["char-1"])
        result = await db.get_sensors()
        assert len(result) == 1
        assert result[0]["name"] == "S2"

    async def test_delete_sensors_by_write_uuid(self, db):
        await db.set_sensors([SENSOR_A, SENSOR_B])
        await db.delete_sensors_by_ids(["write-2"])
        result = await db.get_sensors()
        assert len(result) == 1
        assert result[0]["name"] == "S1"

    async def test_delete_sensors_unknown_id_leaves_all(self, db):
        await db.set_sensors([SENSOR_A])
        await db.delete_sensors_by_ids(["unknown-id"])
        assert len(await db.get_sensors()) == 1

    async def test_delete_all_sensors(self, db):
        await db.set_sensors([SENSOR_A, SENSOR_B])
        await db.delete_all_sensors()
        assert await db.get_sensors() == []


# measurements 

class TestMeasurements:
    """insert_measurement() persists time-series sensor readings."""

    async def test_insert_measurement_persists(self, db):
        await db.insert_measurement("S1", 22.0, 50.0, 600.0, "2026-01-01T10:00:00")
        async with db.db.execute("SELECT * FROM measurements") as cur:
            rows = await cur.fetchall()
        assert len(rows) == 1
        row = dict(rows[0])
        assert row["sensor_name"] == "S1"
        assert row["temperature"] == 22.0
        assert row["moisture"] == 50.0
        assert row["co2"] == 600.0
        assert row["timestamp"] == "2026-01-01T10:00:00"

    async def test_insert_measurement_allows_null_values(self, db):
        await db.insert_measurement("S1", None, None, None, "2026-01-01T10:00:00")
        async with db.db.execute("SELECT temperature FROM measurements") as cur:
            row = await cur.fetchone()
        assert row[0] is None

    async def test_multiple_measurements_stored_independently(self, db):
        await db.insert_measurement("S1", 20.0, 40.0, 500.0, "2026-01-01T10:00:00")
        await db.insert_measurement("S1", 21.0, 41.0, 501.0, "2026-01-01T10:00:01")
        async with db.db.execute("SELECT COUNT(*) FROM measurements") as cur:
            count = (await cur.fetchone())[0]
        assert count == 2


# limits 

class TestLimits:
    """Limit CRUD: set_limit / get_limit / get_all_limits."""

    async def test_set_and_get_limit(self, db):
        await db.set_limit("max_temp", 30.0)
        assert await db.get_limit("max_temp") == 30.0

    async def test_get_limit_returns_none_for_missing(self, db):
        assert await db.get_limit("nonexistent") is None

    async def test_set_limit_overwrites(self, db):
        await db.set_limit("max_temp", 30.0)
        await db.set_limit("max_temp", 35.0)
        assert await db.get_limit("max_temp") == 35.0

    async def test_set_limit_stores_none(self, db):
        await db.set_limit("max_temp", None)
        assert await db.get_limit("max_temp") is None

    async def test_get_all_limits(self, db):
        await db.set_limit("max_temp", 30.0)
        await db.set_limit("min_temp", 18.0)
        result = await db.get_all_limits()
        assert result["max_temp"] == 30.0
        assert result["min_temp"] == 18.0

    async def test_get_all_limits_empty(self, db):
        assert await db.get_all_limits() == {}


# violations 

class TestViolations:
    """Violation lifecycle: register, resolve, and query active violations per sensor."""

    async def test_register_violation_creates_active_entry(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        active = await db.get_active_violations("S1")
        assert len(active) == 1
        assert active[0]["type"] == "max_temp"
        assert active[0]["threshold_value"] == 30.0
        assert active[0]["actual_value"] == 35.0
        assert active[0]["is_active"] == 1

    async def test_register_violation_is_idempotent(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.register_violation("S1", "max_temp", 30.0, 36.0)  # duplicate active
        active = await db.get_active_violations("S1")
        assert len(active) == 1   # second insert ignored by WHERE NOT EXISTS

    async def test_register_different_types_both_stored(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.register_violation("S1", "max_co2", 1000.0, 1200.0)
        active = await db.get_active_violations("S1")
        assert len(active) == 2

    async def test_resolve_violation_marks_inactive(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.resolve_violation("S1", "max_temp")
        active = await db.get_active_violations("S1")
        assert len(active) == 0

    async def test_resolve_violation_sets_resolved_at(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.resolve_violation("S1", "max_temp")
        async with db.db.execute(
            "SELECT resolved_at, is_active FROM limit_violations WHERE sensor_name='S1'"
        ) as cur:
            row = dict(await cur.fetchone())
        assert row["is_active"] == 0
        assert row["resolved_at"] is not None

    async def test_get_active_violations_only_returns_active(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.register_violation("S1", "min_temp", 18.0, 15.0)
        await db.resolve_violation("S1", "max_temp")
        active = await db.get_active_violations("S1")
        assert len(active) == 1
        assert active[0]["type"] == "min_temp"

    async def test_get_active_violations_isolated_per_sensor(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.register_violation("S2", "max_temp", 30.0, 35.0)
        assert len(await db.get_active_violations("S1")) == 1
        assert len(await db.get_active_violations("S2")) == 1

    async def test_new_violation_allowed_after_resolve(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.resolve_violation("S1", "max_temp")
        await db.register_violation("S1", "max_temp", 30.0, 36.0)  # new active
        active = await db.get_active_violations("S1")
        assert len(active) == 1


# warning_id 

class TestWarningId:
    """save_warning_id / get_warning_id track server-assigned warning IDs per violation."""

    async def test_save_and_get_warning_id(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.save_warning_id("S1", "max_temp", "warn-abc")
        assert await db.get_warning_id("S1", "max_temp") == "warn-abc"

    async def test_get_warning_id_returns_none_when_missing(self, db):
        assert await db.get_warning_id("S1", "max_temp") is None

    async def test_get_warning_id_returns_latest(self, db):
        await db.register_violation("S1", "max_temp", 30.0, 35.0)
        await db.resolve_violation("S1", "max_temp")
        await db.save_warning_id("S1", "max_temp", "warn-old")
        await db.register_violation("S1", "max_temp", 30.0, 36.0)
        await db.save_warning_id("S1", "max_temp", "warn-new")
        assert await db.get_warning_id("S1", "max_temp") == "warn-new"


# log_event 

class TestLogEvent:
    """log_event() persists log entries at any severity level without raising."""

    async def test_info_level_does_not_raise(self, db):
        await db.log_event("TEST", "info message", "INFO")

    async def test_error_level_does_not_raise(self, db):
        await db.log_event("TEST", "error message", "ERROR")

    async def test_warn_level_does_not_raise(self, db):
        await db.log_event("TEST", "warn message", "WARNING")

    async def test_default_level_does_not_raise(self, db):
        await db.log_event("TEST", "default message")
