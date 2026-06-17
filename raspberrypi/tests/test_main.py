"""Tests for app/main.py

Strategy: main() is a pure orchestration function - it wires collaborators
together and reacts to their outputs.  We mock every external collaborator
(DatabaseManager, AuthManager, WebManager, BLEManager, DataProcessor) with
AsyncMock / MagicMock instances and assert that main() drives them correctly
under each scenario.

We never touch the real filesystem, network, or SQLite.
"""

import asyncio
import sys
import pytest
from unittest.mock import AsyncMock, MagicMock, patch, call
PATCHES = {
    "DatabaseManager": "main.DatabaseManager",
    "AuthManager":     "main.AuthManager",
    "WebManager":      "main.WebManager",
    "BLEManager":      "main.BLEManager",
    "DataProcessor":   "main.DataProcessor",
}


def make_db(*, configure_done="1", initial_config_done="1",
            server_url="https://example.com", username="user",
            password="pass", port="8080", sensors=None):
    """Return a fully-wired AsyncMock DatabaseManager."""
    db = AsyncMock()

    config_map = {
        "configure_done":    configure_done,
        "initial_config_done": initial_config_done,
        "server_url":        server_url,
        "auth_username":     username,
        "auth_password":     password,
        "local_listen_port": port,
    }
    db.get_config.side_effect = lambda key: config_map.get(key)
    db.get_sensors.return_value = sensors if sensors is not None else []
    return db


def make_web_manager():
    wm = AsyncMock()
    wm.status_queue = asyncio.Queue()
    wm._ble_tasks = {}
    wm.run_local_server = AsyncMock(return_value=None)
    wm.run_outgoing_data_worker = AsyncMock(return_value=None)
    wm.run_outgoing_violation_worker = AsyncMock(return_value=None)
    wm.run_outgoing_status_worker = AsyncMock(return_value=None)
    return wm


def noop_task(name="noop"):
    """An asyncio.Task that completes immediately - safe to gather."""
    async def _noop():
        pass
    return asyncio.create_task(_noop(), name=name)

class MainRunner:
    """Patches all collaborators, runs main(), captures SystemExit.

    Pass override_ble / override_processor to inject a custom factory/mock
    for those two collaborators; otherwise sensible no-op defaults are used.
    All patches are applied inside run() so there is no double-patching issue
    when callers add their own patch() context managers.
    """

    def __init__(self, db_mock, web_mock, *,
                 auth_mock=None,
                 override_ble=None,       # callable(*args) → ble mock, or a mock class
                 override_processor=None, # AsyncMock instance to use as DataProcessor
                 ):
        self.db_mock = db_mock
        self.web_mock = web_mock
        self.auth_mock = auth_mock or AsyncMock()
        self.override_ble = override_ble
        self.override_processor = override_processor
        self.exit_code = None

    async def run(self, db_path="fake.sqlite"):
        import main as main_mod

        def _ble_cls(*args, **kwargs):
            if self.override_ble:
                return self.override_ble(*args, **kwargs)
            ble = AsyncMock()
            ble.run = AsyncMock(return_value=None)
            return ble

        proc_instance = self.override_processor or AsyncMock()
        proc_instance.run = getattr(proc_instance, "run", AsyncMock(return_value=None))

        with (
            patch("main.DatabaseManager", MagicMock(return_value=self.db_mock)),
            patch("main.AuthManager",     MagicMock(return_value=self.auth_mock)),
            patch("main.WebManager",      MagicMock(return_value=self.web_mock)),
            patch("main.BLEManager",      _ble_cls),
            patch("main.DataProcessor",   MagicMock(return_value=proc_instance)),
        ):
            try:
                await main_mod.main(db_path)
            except SystemExit as e:
                self.exit_code = e.code


class TestDbInit:
    """main() connects to the DB on startup and closes it on every exit path."""

    @pytest.mark.asyncio
    async def test_connects_and_inits_db(self):
        db = make_db()
        web = make_web_manager()
        # web tasks must complete so gather() returns
        web.run_local_server            = AsyncMock(return_value=None)
        web.run_outgoing_data_worker    = AsyncMock(return_value=None)
        web.run_outgoing_violation_worker = AsyncMock(return_value=None)
        web.run_outgoing_status_worker  = AsyncMock(return_value=None)

        runner = MainRunner(db, web)
        await runner.run()

        db.connect.assert_awaited_once()
        db.init_db.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_close_called_on_normal_exit(self):
        db = make_db()
        web = make_web_manager()
        runner = MainRunner(db, web)
        await runner.run()
        db.close.assert_awaited()


class TestConfigureDoneWait:
    """main() polls configure_done until it is '1' before proceeding."""

    @pytest.mark.asyncio
    async def test_waits_until_configure_done(self):
        """If configure_done starts as '0', main should poll until it's '1'."""
        db = make_db()
        responses = ["0", "0", "1"]
        idx = 0

        original_side_effect = db.get_config.side_effect

        async def patched_get_config(key):
            nonlocal idx
            if key == "configure_done":
                val = responses[min(idx, len(responses) - 1)]
                idx += 1
                return val
            return await asyncio.coroutine(lambda k: original_side_effect(k))(key)

        # Rebuild with sequential responses for configure_done
        call_counts = {"configure_done": 0}
        seq = ["0", "0", "1"]

        config_map = {
            "initial_config_done": "1",
            "server_url":          "https://example.com",
            "auth_username":       "user",
            "auth_password":       "pass",
            "local_listen_port":   "8080",
        }

        async def get_config(key):
            if key == "configure_done":
                i = call_counts["configure_done"]
                call_counts["configure_done"] += 1
                return seq[min(i, len(seq) - 1)]
            return config_map.get(key)

        db.get_config.side_effect = get_config

        web = make_web_manager()

        with patch("main.asyncio.sleep", new_callable=AsyncMock):
            runner = MainRunner(db, web)
            await runner.run()

        # Must have polled at least 3 times (0, 0, 1)
        assert call_counts["configure_done"] >= 3


class TestMissingCredentials:
    """main() exits with code 1 when any required credential is absent from the DB."""

    @pytest.mark.asyncio
    async def test_exits_if_server_url_missing(self):
        db = make_db(server_url=None)
        runner = MainRunner(db, make_web_manager())
        await runner.run()
        assert runner.exit_code == 1

    @pytest.mark.asyncio
    async def test_exits_if_username_missing(self):
        db = make_db(username=None)
        runner = MainRunner(db, make_web_manager())
        await runner.run()
        assert runner.exit_code == 1

    @pytest.mark.asyncio
    async def test_exits_if_password_missing(self):
        db = make_db(password=None)
        runner = MainRunner(db, make_web_manager())
        await runner.run()
        assert runner.exit_code == 1

    @pytest.mark.asyncio
    async def test_db_closed_before_exit_on_missing_creds(self):
        db = make_db(server_url=None)
        runner = MainRunner(db, make_web_manager())
        await runner.run()
        db.close.assert_awaited()


class TestAuthLogin:
    """main() calls auth.login() and retries on transient failures."""

    @pytest.mark.asyncio
    async def test_succeeds_on_first_attempt(self):
        db = make_db()
        auth = AsyncMock()
        auth.login = AsyncMock()
        web = make_web_manager()

        runner = MainRunner(db, web, auth_mock=auth)
        await runner.run()

        auth.login.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_retries_and_succeeds_on_third_attempt(self):
        db = make_db()
        auth = AsyncMock()
        call_count = 0

        async def flaky_login():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise ConnectionError("timeout")

        auth.login = flaky_login
        web = make_web_manager()

        with patch("main.asyncio.sleep", new_callable=AsyncMock):
            runner = MainRunner(db, web, auth_mock=auth)
            await runner.run()

        assert call_count == 3
        assert runner.exit_code is None  # did not exit


class TestBootMode:
    """main() waits for a config_ready_event on first boot; skips waiting on subsequent starts."""

    @pytest.mark.asyncio
    async def test_first_boot_waits_for_config_event(self):
        """On first boot the server starts and main waits for config_ready_event."""
        db = make_db(initial_config_done="0")  # first boot
        web = make_web_manager()
        event_was_awaited = False

        original_event_class = asyncio.Event

        class TrackedEvent(original_event_class):
            async def wait(self):
                nonlocal event_was_awaited
                event_was_awaited = True
                self.set()  # unblock immediately in tests

        with patch("main.asyncio.Event", TrackedEvent):
            runner = MainRunner(db, web)
            await runner.run()

        assert event_was_awaited

    @pytest.mark.asyncio
    async def test_normal_boot_sets_event_immediately(self):
        """On non-first boot config_ready_event is pre-set; wait() must not block."""
        db = make_db(initial_config_done="1")
        web = make_web_manager()
        wait_called = False

        original_event_class = asyncio.Event

        class SpyEvent(original_event_class):
            async def wait(self):
                nonlocal wait_called
                wait_called = True
                await super().wait()

        with patch("main.asyncio.Event", SpyEvent):
            runner = MainRunner(db, web)
            await runner.run()

        # Event is pre-set so wait() returns instantly - but it may still be called
        # The important assertion is that main() did NOT hang.
        assert runner.exit_code is None

SENSOR_A = {"name": "sensorA", "write_uuid": "uuid-a", "address": "AA:BB:CC"}
SENSOR_B = {"name": "sensorB", "write_uuid": "uuid-b", "address": "DD:EE:FF"}


class TestSensorWiring:
    """main() creates one BLEManager and one DataProcessor task per registered sensor."""

    @pytest.mark.asyncio
    async def test_no_sensors_does_not_raise(self):
        db = make_db(sensors=[])
        runner = MainRunner(db, make_web_manager())
        await runner.run()
        assert runner.exit_code is None

    @pytest.mark.asyncio
    async def test_ble_manager_created_per_sensor(self):
        db = make_db(sensors=[SENSOR_A, SENSOR_B])
        web = make_web_manager()
        created_names = []

        def ble_factory(db, sensor, proc_q, inbox_q, status_q, scan_lock):
            created_names.append(sensor["name"])
            ble = AsyncMock()
            ble.run = AsyncMock(return_value=None)
            return ble

        runner = MainRunner(db, web, override_ble=ble_factory)
        await runner.run()

        assert sorted(created_names) == ["sensorA", "sensorB"]

    @pytest.mark.asyncio
    async def test_processor_run_called_per_sensor(self):
        db = make_db(sensors=[SENSOR_A, SENSOR_B])
        web = make_web_manager()
        proc = AsyncMock()
        proc.run = AsyncMock(return_value=None)

        runner = MainRunner(db, web, override_processor=proc)
        await runner.run()

        assert proc.run.await_count == 2
        call_args = [c.args[0] for c in proc.run.await_args_list]
        assert sorted(call_args) == ["sensorA", "sensorB"]

    @pytest.mark.asyncio
    async def test_ble_tasks_registered_on_web_manager(self):
        """web_manager._ble_tasks must contain a task per sensor."""
        db = make_db(sensors=[SENSOR_A])
        web = make_web_manager()

        runner = MainRunner(db, web)
        await runner.run()

        assert "sensorA" in web._ble_tasks

    @pytest.mark.asyncio
    async def test_local_listen_port_defaults_to_8080_when_none(self):
        """If local_listen_port is missing from DB it must default to 8080."""
        db = make_db(port=None)
        captured_port = {}

        def capture_web(*args, **kwargs):
            # second positional arg is local_listen_port
            captured_port["port"] = args[1] if len(args) > 1 else kwargs.get("local_listen_port")
            return make_web_manager()

        with patch("main.WebManager", side_effect=capture_web):
            runner = MainRunner(db, make_web_manager())
            # re-run with capture patch
            import main as main_mod
            with (
                patch("main.DatabaseManager", MagicMock(return_value=db)),
                patch("main.AuthManager",     MagicMock(return_value=AsyncMock())),
                patch("main.DataProcessor",   MagicMock(return_value=AsyncMock())),
                patch("main.BLEManager",      MagicMock(return_value=AsyncMock())),
            ):
                try:
                    await main_mod.main("fake.sqlite")
                except SystemExit:
                    pass

        assert captured_port.get("port") == 8080

class TestCancelledError:
    """main() closes the DB cleanly when cancelled via asyncio.CancelledError."""

    @pytest.mark.asyncio
    async def test_cancelled_error_closes_db(self):
        db = make_db()
        web = make_web_manager()

        # Make gather raise CancelledError
        async def raise_cancelled(*args, **kwargs):
            raise asyncio.CancelledError()

        with patch("main.asyncio.gather", side_effect=raise_cancelled):
            runner = MainRunner(db, web)
            await runner.run()

        db.close.assert_awaited()
        assert runner.exit_code is None  # CancelledError is not a sys.exit
