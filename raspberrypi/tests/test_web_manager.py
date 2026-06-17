import asyncio
import pytest
from unittest.mock import AsyncMock, MagicMock, patch, call
from aiohttp import web
from aiohttp.test_utils import TestClient, TestServer

from app.web_manager import WebManager

@pytest.fixture
def mock_db():
    db = AsyncMock()
    db.get_config.return_value = None
    db.get_active_violations.return_value = []
    db.get_warning_id.return_value = None
    return db


@pytest.fixture
def mock_auth():
    auth = MagicMock()
    auth.get_headers.return_value = {"Authorization": "Bearer tok"}
    auth.refresh_if_needed = AsyncMock()
    return auth


@pytest.fixture
def web_manager(mock_db, mock_auth):
    return WebManager(
        db=mock_db,
        local_listen_port=8080,
        server_url="http://test-server",
        web_out_queue=asyncio.Queue(),
        web_violation_queue=asyncio.Queue(),
        auth=mock_auth,
        config_ready_event=asyncio.Event(),
        first_boot=False,
    )


@pytest.fixture
async def client(web_manager):
    app = web.Application()
    app.router.add_post('/api/limits', web_manager.handle_limits)
    app.router.add_post('/api/occupancy', web_manager.handle_occupancy)
    app.router.add_post('/api/sensors', web_manager.handle_sensors)
    app.router.add_post('/api/config', web_manager.handle_config)
    app.router.add_post('/api/retry-sensor', web_manager.handle_retry_sensor)

    def _noop_task(coro, **kwargs):
        coro.close()
        return MagicMock()

    with patch("app.web_manager.asyncio.create_task", side_effect=_noop_task):
        async with TestClient(TestServer(app)) as c:
            yield c


async def _drain_worker(coro, iterations=1):
    task = asyncio.create_task(coro)
    for _ in range(iterations * 20):
        await asyncio.sleep(0)
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass

class TestSpawnSensorTasks:
    """_spawn_sensor_tasks() creates BLEManager and DataProcessor tasks and registers them."""

    def _make_processor(self):
        proc = MagicMock()
        proc.run = MagicMock(return_value=asyncio.coroutine(lambda *a: None)())
        return proc

    async def test_creates_ble_manager_and_registers_task(self, web_manager, mock_db):
        """Happy path: BLEManager + Processor tasks are created and registered."""
        sensor = {"name": "S1", "write_uuid": "w-uuid", "address": "AA:BB:CC"}
        web_manager.scan_lock = asyncio.Lock()

        fake_ble = AsyncMock()
        fake_ble.run = AsyncMock(return_value=None)
        fake_proc_coro = AsyncMock(return_value=None)
        web_manager.processor = MagicMock()
        web_manager.processor.run = fake_proc_coro

        ble_task = MagicMock(spec=asyncio.Task)
        proc_task = MagicMock(spec=asyncio.Task)
        task_side_effects = [ble_task, proc_task]

        with (
            patch("app.web_manager.BLEManager", return_value=fake_ble),
            patch("app.web_manager.asyncio.create_task", side_effect=lambda c, **kw: (c.close() or task_side_effects.pop(0))),
        ):
            web_manager._spawn_sensor_tasks("S1", sensor)

        assert web_manager._ble_tasks["S1"] is ble_task
        assert ble_task in web_manager._running_tasks
        assert proc_task in web_manager._running_tasks

    async def test_reuses_existing_queues(self, web_manager):
        """If queues already exist for the sensor they must not be replaced."""
        sensor = {"name": "S1", "write_uuid": "w-uuid", "address": "AA:BB:CC"}
        web_manager.scan_lock = asyncio.Lock()
        web_manager.processor = MagicMock()
        web_manager.processor.run = AsyncMock(return_value=None)

        existing_q = {"proc": asyncio.Queue(), "inbox": asyncio.Queue()}
        web_manager.queues["S1"] = existing_q

        fake_ble = AsyncMock()
        fake_ble.run = AsyncMock(return_value=None)

        with (
            patch("app.web_manager.BLEManager", return_value=fake_ble) as MockBLE,
            patch("app.web_manager.asyncio.create_task", side_effect=lambda c, **kw: (c.close() or MagicMock())),
        ):
            web_manager._spawn_sensor_tasks("S1", sensor)
            _, kwargs_or_args = MockBLE.call_args[0], MockBLE.call_args
            passed_proc_q = MockBLE.call_args[0][2]

        assert passed_proc_q is existing_q["proc"]

    async def test_no_op_when_processor_not_set(self, web_manager):
        """Guard: if processor is None, no tasks should be created."""
        web_manager.processor = None
        web_manager.scan_lock = asyncio.Lock()

        with patch("app.web_manager.asyncio.create_task") as mock_ct:
            web_manager._spawn_sensor_tasks("S1", {"name": "S1", "write_uuid": "x"})

        mock_ct.assert_not_called()

    async def test_no_op_when_scan_lock_not_set(self, web_manager):
        """Guard: if scan_lock is None, no tasks should be created."""
        web_manager.processor = MagicMock()
        web_manager.scan_lock = None

        with patch("app.web_manager.asyncio.create_task") as mock_ct:
            web_manager._spawn_sensor_tasks("S1", {"name": "S1", "write_uuid": "x"})

        mock_ct.assert_not_called()

    async def test_creates_new_queues_when_none_exist(self, web_manager):
        """Fresh sensor with no pre-existing queues gets new ones allocated."""
        sensor = {"name": "S2", "write_uuid": "w-uuid-2", "address": "AA:BB:DD"}
        web_manager.scan_lock = asyncio.Lock()
        web_manager.processor = MagicMock()
        web_manager.processor.run = AsyncMock(return_value=None)

        fake_ble = AsyncMock()
        with (
            patch("app.web_manager.BLEManager", return_value=fake_ble),
            patch("app.web_manager.asyncio.create_task", side_effect=lambda c, **kw: (c.close() or MagicMock())),
        ):
            web_manager._spawn_sensor_tasks("S2", sensor)

        assert "S2" in web_manager.queues
        assert "proc" in web_manager.queues["S2"]
        assert "inbox" in web_manager.queues["S2"]


class TestTaskSensorChangeAdd:
    """_task_sensor_change() handles SENSOR_ADD, SENSOR_DELETE, and FLUSH events."""

    def _live_task(self):
        t = MagicMock(spec=asyncio.Task)
        t.done.return_value = False
        return t

    def _dead_task(self):
        t = MagicMock(spec=asyncio.Task)
        t.done.return_value = True
        return t

    async def test_add_does_not_wake_already_set_event(self, web_manager, mock_db):
        """If reconnect_event is already set, don't call set() again."""
        sensor = {"name": "S1", "write_uuid": "w-1"}
        mock_db.get_sensors.return_value = [sensor]

        ble = MagicMock()
        ble.reconnect_event = MagicMock()
        ble.reconnect_event.is_set.return_value = True
        web_manager.ble_managers["S1"] = ble
        web_manager._ble_tasks["S1"] = self._live_task()

        with patch("app.web_manager.ConfigManager.handle_sensor_add", new=AsyncMock()):
            await web_manager._task_sensor_change("SENSOR_ADD", ["w-1"])

        ble.reconnect_event.set.assert_not_called()

    async def test_add_exception_does_not_propagate(self, web_manager):
        with patch("app.web_manager.ConfigManager.handle_sensor_add",
                   new=AsyncMock(side_effect=Exception("boom"))):
            await web_manager._task_sensor_change("SENSOR_ADD", ["id-1"])

    async def test_delete_sets_removal_event(self, web_manager, mock_db):
        """SENSOR_DELETE sets removal_event on the matching BLE manager."""
        ble = MagicMock()
        ble.sensor = {"char_uuid": "char-1", "write_uuid": "write-1"}
        web_manager.ble_managers = {"S1": ble}

        with patch("app.web_manager.ConfigManager.handle_sensor_delete", new=AsyncMock()):
            await web_manager._task_sensor_change("SENSOR_DELETE", ["char-1"])

        ble.removal_event.set.assert_called_once()

    async def test_flush_sets_removal_event_on_all(self, web_manager, mock_db):
        """FLUSH triggers removal_event.set() on every BLE manager."""
        ble1, ble2 = MagicMock(), MagicMock()
        web_manager.ble_managers = {"S1": ble1, "S2": ble2}

        with patch("app.web_manager.ConfigManager.handle_sensor_flush", new=AsyncMock()):
            await web_manager._task_sensor_change("FLUSH", [])

        ble1.removal_event.set.assert_called_once()
        ble2.removal_event.set.assert_called_once()


class TestTaskLimitChange:
    """_task_limit_change() delegates to ConfigManager and swallows/logs exceptions."""

    async def test_delegates_to_config_manager(self, web_manager):
        with patch("app.web_manager.ConfigManager.handle_limit_change",
                   new=AsyncMock()) as mock_handler:
            await web_manager._task_limit_change({"tempMax": 30})
        mock_handler.assert_awaited_once_with(web_manager.db, {"tempMax": 30})

    async def test_exception_is_swallowed_and_logged(self, web_manager, mock_db):
        with patch("app.web_manager.ConfigManager.handle_limit_change",
                   new=AsyncMock(side_effect=Exception("db error"))):
            await web_manager._task_limit_change({"tempMax": 30})
        mock_db.log_event.assert_awaited_once()



class TestTaskOccupancyChange:
    """_task_occupancy_change() delegates to ConfigManager and swallows/logs exceptions."""

    async def test_delegates_to_config_manager(self, web_manager):
        with patch("app.web_manager.ConfigManager.handle_occupancy_change",
                   new=AsyncMock()) as mock_handler:
            await web_manager._task_occupancy_change({"effectiveOccupancy": 5})
        mock_handler.assert_awaited_once_with(web_manager.db, {"effectiveOccupancy": 5})

    async def test_exception_is_swallowed_and_logged(self, web_manager, mock_db):
        with patch("app.web_manager.ConfigManager.handle_occupancy_change",
                   new=AsyncMock(side_effect=RuntimeError("oops"))):
            await web_manager._task_occupancy_change({"effectiveOccupancy": 5})
        mock_db.log_event.assert_awaited_once()


class TestHandleTip:
    """_handle_tip() routes violation tip messages to the appropriate BLE inbox."""

    def _make_ble(self, write_uuid):
        ble = MagicMock()
        ble.sensor = {"write_uuid": write_uuid}
        ble.name = "S1"
        ble.ble_inbox = asyncio.Queue()
        return ble

    async def test_forwards_tip_to_matching_ble_inbox(self, web_manager, mock_db):
        ble = self._make_ble("w-uuid-1")
        web_manager.ble_managers = {"S1": ble}
        mock_db.get_active_violations.return_value = [
            {"type": "max_temp", "threshold_value": 28.0}
        ]

        data = {
            "measurementType": "TEMPERATURE",
            "writeId": "w-uuid-1",
            "status": "YELLOW",
            "tip": "Open a window",
        }
        await web_manager._handle_tip(data)

        msg = ble.ble_inbox.get_nowait()
        assert "Open a window" in msg

    async def test_no_matching_ble_does_not_raise(self, web_manager):
        web_manager.ble_managers = {}
        data = {
            "measurementType": "TEMPERATURE",
            "writeId": "unknown-uuid",
            "status": "YELLOW",
            "tip": "tip text",
        }
        await web_manager._handle_tip(data)

    async def test_missing_required_fields_skips_silently(self, web_manager):
        """Tip with missing fields should be a no-op."""
        await web_manager._handle_tip({"measurementType": "TEMPERATURE"})

    async def test_below_limit_direction_in_message(self, web_manager, mock_db):
        """A min_* violation should produce 'below limit' in the BLE message."""
        ble = self._make_ble("w-uuid-2")
        web_manager.ble_managers = {"S1": ble}
        mock_db.get_active_violations.return_value = [
            {"type": "min_temp", "threshold_value": 10.0}
        ]

        data = {
            "measurementType": "TEMPERATURE",
            "writeId": "w-uuid-2",
            "status": "YELLOW",
            "tip": "Heat the room",
        }
        await web_manager._handle_tip(data)

        msg = ble.ble_inbox.get_nowait()
        assert "below limit" in msg

    async def test_no_active_violations_uses_plain_type(self, web_manager, mock_db):
        """When there are no active violations the warntext falls back to the raw type."""
        ble = self._make_ble("w-uuid-3")
        web_manager.ble_managers = {"S1": ble}
        mock_db.get_active_violations.return_value = []

        data = {
            "measurementType": "CO2",
            "writeId": "w-uuid-3",
            "status": "RED",
            "tip": "Ventilate",
        }
        await web_manager._handle_tip(data)

        msg = ble.ble_inbox.get_nowait()
        assert "co2" in msg.lower()
        assert "N/A" in msg


class TestHandleRetrySensorRespawn:
    """handle_retry_sensor() respawns dead tasks or triggers reconnect for live ones."""

    async def test_dead_task_triggers_respawn(self, client, web_manager):
        ble = MagicMock()
        ble.read_uuid = "char-uuid-1"
        ble.sensor = {"write_uuid": "write-uuid-1"}
        ble.reconnect_event = MagicMock()
        web_manager.ble_managers = {"S1": ble}

        dead_task = MagicMock()
        dead_task.done.return_value = True
        web_manager._ble_tasks["S1"] = dead_task

        with patch.object(web_manager, "_spawn_sensor_tasks") as mock_spawn:
            resp = await client.post('/api/retry-sensor?sensorIds=char-uuid-1')

        assert resp.status == 202
        mock_spawn.assert_called_once_with("S1", ble.sensor)
        ble.reconnect_event.set.assert_not_called()

    async def test_no_ble_task_entry_triggers_respawn(self, client, web_manager):
        """_ble_tasks has no entry for the sensor → task_alive=False → respawn."""
        ble = MagicMock()
        ble.read_uuid = "char-uuid-1"
        ble.sensor = {"write_uuid": "write-uuid-1"}
        ble.reconnect_event = MagicMock()
        web_manager.ble_managers = {"S1": ble}

        with patch.object(web_manager, "_spawn_sensor_tasks") as mock_spawn:
            resp = await client.post('/api/retry-sensor?sensorIds=char-uuid-1')

        assert resp.status == 202
        mock_spawn.assert_called_once()

    async def test_live_task_sets_reconnect_event(self, client, web_manager):
        """A live BLE task should have reconnect_event.set() called, not spawn."""
        ble = MagicMock()
        ble.read_uuid = "char-uuid-2"
        ble.sensor = {"write_uuid": "write-uuid-2"}
        ble.reconnect_event = MagicMock()
        web_manager.ble_managers = {"S1": ble}

        live_task = MagicMock()
        live_task.done.return_value = False
        web_manager._ble_tasks["S1"] = live_task

        with patch.object(web_manager, "_spawn_sensor_tasks") as mock_spawn:
            resp = await client.post('/api/retry-sensor?sensorIds=char-uuid-2')

        assert resp.status == 202
        ble.reconnect_event.set.assert_called_once()
        mock_spawn.assert_not_called()

    async def test_unknown_sensor_returns_404(self, client, web_manager):
        """No matching sensor yields a 404."""
        web_manager.ble_managers = {}
        resp = await client.post('/api/retry-sensor?sensorIds=no-such-uuid')
        assert resp.status == 404

    async def test_missing_sensor_ids_returns_400(self, client, web_manager):
        """Missing sensorIds query param yields 400."""
        resp = await client.post('/api/retry-sensor')
        assert resp.status == 400


class TestRunOutgoingDataWorker:
    """run_outgoing_data_worker() POSTs measurements, requeues on failure, and refreshes auth on 401."""

    def _mock_response(self, status=200):
        resp = AsyncMock()
        resp.status = status
        resp.__aenter__ = AsyncMock(return_value=resp)
        resp.__aexit__ = AsyncMock(return_value=False)
        return resp

    def _make_session(self, post_response):
        session = AsyncMock()
        session.post = MagicMock(return_value=post_response)
        session.close = AsyncMock()
        session.__aenter__ = AsyncMock(return_value=session)
        session.__aexit__ = AsyncMock(return_value=False)
        return session

    async def test_successful_post_drains_queue(self, web_manager):
        payload = {"temp": 22}
        await web_manager.web_out_queue.put(payload)

        resp = self._mock_response(200)
        session = self._make_session(resp)

        with patch("app.web_manager.aiohttp.ClientSession", return_value=session):
            await _drain_worker(web_manager.run_outgoing_data_worker())

        assert web_manager.web_out_queue.empty()

    async def test_failed_post_requeues_payload(self, web_manager):
        """A non-2xx response should requeue the payload."""
        payload = {"temp": 22}
        await web_manager.web_out_queue.put(payload)

        resp = self._mock_response(500)
        session = self._make_session(resp)

        with (
            patch("app.web_manager.aiohttp.ClientSession", return_value=session),
            patch("app.web_manager.asyncio.sleep", new=AsyncMock()),
        ):
            await _drain_worker(web_manager.run_outgoing_data_worker(), iterations=2)

        assert not web_manager.web_out_queue.empty()

    async def test_401_triggers_token_refresh_on_data_worker(self, web_manager, mock_auth):
        """A 401 from the measurements endpoint must trigger auth refresh."""
        await web_manager.web_out_queue.put({"temp": 22})

        ok_resp = self._mock_response(200)
        unauth_resp = self._mock_response(401)

        call_n = {"n": 0}

        def _post_side(*a, **kw):
            call_n["n"] += 1
            return unauth_resp if call_n["n"] == 1 else ok_resp

        session = AsyncMock()
        session.post = MagicMock(side_effect=_post_side)
        session.close = AsyncMock()

        with patch("app.web_manager.aiohttp.ClientSession", return_value=session):
            await _drain_worker(web_manager.run_outgoing_data_worker(), iterations=1)

        mock_auth.refresh_if_needed.assert_awaited()


class TestRunOutgoingViolationWorker:
    """run_outgoing_violation_worker() dispatches violation events and requeues on failure."""

    async def _run_one(self, web_manager, event):
        await web_manager.web_violation_queue.put(event)
        task = asyncio.create_task(web_manager.run_outgoing_violation_worker())
        for _ in range(20):
            await asyncio.sleep(0)
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass

    async def test_warning_event_calls_handle_violation_warning(self, web_manager):
        event = {"type": "violation_warning", "sensor_name": "S1", "limit_key": "max_temp"}

        with patch.object(web_manager, "_handle_violation_warning", new=AsyncMock()) as mock_handler:
            await self._run_one(web_manager, event)

        mock_handler.assert_awaited_once()

    async def test_resolve_event_calls_handle_violation_resolve(self, web_manager):
        event = {"type": "violation_resolve", "sensor_name": "S1", "limit_key": "max_temp"}

        with patch.object(web_manager, "_handle_violation_resolve", new=AsyncMock()) as mock_handler:
            await self._run_one(web_manager, event)

        mock_handler.assert_awaited_once()

    async def test_unknown_event_type_does_not_raise(self, web_manager):
        event = {"type": "totally_unknown"}
        await self._run_one(web_manager, event)  # must not raise

    async def test_exception_requeues_event(self, web_manager):
        event = {"type": "violation_warning"}

        with (
            patch.object(web_manager, "_handle_violation_warning",
                         new=AsyncMock(side_effect=Exception("network down"))),
            patch("app.web_manager.asyncio.sleep", new=AsyncMock()),
        ):
            await self._run_one(web_manager, event)

        assert not web_manager.web_violation_queue.empty()

    async def test_queue_is_drained_after_processing(self, web_manager):
        """task_done() must be called so the queue reports as drained."""
        event = {"type": "violation_resolve", "sensor_name": "S1", "limit_key": "k"}

        with patch.object(web_manager, "_handle_violation_resolve", new=AsyncMock()):
            await self._run_one(web_manager, event)

        assert web_manager.web_violation_queue.empty()


class TestHandleViolationWarning:
    """_handle_violation_warning() POSTs a new warning and saves the server-assigned ID."""

    def _make_session(self, status=200, response_json=None):
        data = response_json or {"id": "warn-123", "tip": "Open window",
                                 "measurementType": "TEMPERATURE",
                                 "writeId": "w-uuid", "status": "YELLOW"}
        resp = AsyncMock()
        resp.status = status
        resp.json = AsyncMock(return_value=data)
        resp.raise_for_status = MagicMock()
        resp.__aenter__ = AsyncMock(return_value=resp)
        resp.__aexit__ = AsyncMock(return_value=False)
        session = AsyncMock()
        session.post = MagicMock(return_value=resp)
        return session

    async def test_saves_warning_id_on_success(self, web_manager, mock_db):
        session = self._make_session(200)
        event = {
            "type": "violation_warning",
            "sensor_name": "S1", "limit_key": "max_temp",
            "roomId": "room-1", "measurement_type": "TEMPERATURE",
            "status": "YELLOW", "triggeredValue": 32,
            "activeLimitAtTime": 30, "message": "too hot",
            "sensorWriteId": "w-uuid",
        }
        with patch.object(web_manager, "_handle_tip", new=AsyncMock()):
            await web_manager._handle_violation_warning(session, event)

        mock_db.save_warning_id.assert_awaited_once_with("S1", "max_temp", "warn-123")

    async def test_no_id_in_response_skips_save(self, web_manager, mock_db):
        session = self._make_session(200, response_json={"noId": True})
        event = {
            "type": "violation_warning",
            "sensor_name": "S1", "limit_key": "max_temp",
            "roomId": "r", "measurement_type": "TEMPERATURE",
            "status": "YELLOW", "triggeredValue": 32,
            "activeLimitAtTime": 30, "message": "hot",
            "sensorWriteId": "w-uuid",
        }
        with patch.object(web_manager, "_handle_tip", new=AsyncMock()):
            await web_manager._handle_violation_warning(session, event)

        mock_db.save_warning_id.assert_not_awaited()

    async def test_network_error_requeues(self, web_manager):
        session = AsyncMock()
        session.post = MagicMock(side_effect=Exception("timeout"))
        event = {
            "type": "violation_warning",
            "sensor_name": "S1", "limit_key": "max_temp",
            "roomId": "r", "measurement_type": "TEMPERATURE",
            "status": "YELLOW", "triggeredValue": 32,
            "activeLimitAtTime": 30, "message": "hot",
            "sensorWriteId": "w-uuid",
        }
        with patch("app.web_manager.asyncio.sleep", new=AsyncMock()):
            await web_manager._handle_violation_warning(session, event)

        assert not web_manager.web_violation_queue.empty()

    async def test_401_triggers_token_refresh(self, web_manager, mock_auth):
        """A 401 response must call auth.refresh_if_needed before retrying."""
        ok_resp = AsyncMock()
        ok_resp.status = 200
        ok_resp.json = AsyncMock(return_value={"id": "warn-123"})
        ok_resp.raise_for_status = MagicMock()
        ok_resp.__aenter__ = AsyncMock(return_value=ok_resp)
        ok_resp.__aexit__ = AsyncMock(return_value=False)

        unauth_resp = AsyncMock()
        unauth_resp.status = 401
        unauth_resp.__aenter__ = AsyncMock(return_value=unauth_resp)
        unauth_resp.__aexit__ = AsyncMock(return_value=False)

        call_n = {"n": 0}

        def _post(*a, **kw):
            call_n["n"] += 1
            return unauth_resp if call_n["n"] == 1 else ok_resp

        session = AsyncMock()
        session.post = MagicMock(side_effect=_post)

        event = {
            "type": "violation_warning",
            "sensor_name": "S1", "limit_key": "max_temp",
            "roomId": "r", "measurement_type": "TEMPERATURE",
            "status": "YELLOW", "triggeredValue": 32,
            "activeLimitAtTime": 30, "message": "hot",
            "sensorWriteId": "w-uuid",
        }
        with patch.object(web_manager, "_handle_tip", new=AsyncMock()):
            await web_manager._handle_violation_warning(session, event)

        mock_auth.refresh_if_needed.assert_awaited()

    async def test_empty_warning_id_string_skips_save(self, web_manager, mock_db):
        """An id of '' (empty string) should not trigger save_warning_id."""
        session = self._make_session(200, response_json={"id": ""})
        event = {
            "type": "violation_warning",
            "sensor_name": "S1", "limit_key": "max_temp",
            "roomId": "r", "measurement_type": "TEMPERATURE",
            "status": "YELLOW", "triggeredValue": 32,
            "activeLimitAtTime": 30, "message": "hot",
            "sensorWriteId": "w-uuid",
        }
        with patch.object(web_manager, "_handle_tip", new=AsyncMock()):
            await web_manager._handle_violation_warning(session, event)

        mock_db.save_warning_id.assert_not_awaited()


class TestHandleViolationResolve:
    """_handle_violation_resolve() PATCHes the warning to resolved and notifies the sensor via BLE."""

    def _make_session(self, status=200):
        resp = AsyncMock()
        resp.status = status
        resp.raise_for_status = MagicMock()
        resp.__aenter__ = AsyncMock(return_value=resp)
        resp.__aexit__ = AsyncMock(return_value=False)
        session = AsyncMock()
        session.patch = MagicMock(return_value=resp)
        return session

    async def test_resolves_and_notifies_ble(self, web_manager, mock_db):
        mock_db.get_warning_id.return_value = "warn-abc"
        ble_inbox = asyncio.Queue()
        ble = MagicMock()
        ble.ble_inbox = ble_inbox
        web_manager.ble_managers = {"S1": ble}

        session = self._make_session(200)
        event = {
            "type": "violation_resolve",
            "sensor_name": "S1", "limit_key": "max_temp",
            "measurement_type": "TEMPERATURE",
        }
        await web_manager._handle_violation_resolve(session, event)

        msg = ble_inbox.get_nowait()
        assert msg == "RESOLVED:TEMPERATURE"

    async def test_no_warning_id_skips_patch(self, web_manager, mock_db):
        mock_db.get_warning_id.return_value = None
        session = self._make_session()
        event = {
            "type": "violation_resolve",
            "sensor_name": "S1", "limit_key": "max_temp",
            "measurement_type": "TEMPERATURE",
        }
        await web_manager._handle_violation_resolve(session, event)
        session.patch.assert_not_called()

    async def test_network_error_requeues(self, web_manager, mock_db):
        mock_db.get_warning_id.return_value = "warn-xyz"
        session = AsyncMock()
        session.patch = MagicMock(side_effect=Exception("conn error"))
        event = {
            "type": "violation_resolve",
            "sensor_name": "S1", "limit_key": "max_temp",
            "measurement_type": "TEMPERATURE",
        }
        with patch("app.web_manager.asyncio.sleep", new=AsyncMock()):
            await web_manager._handle_violation_resolve(session, event)

        assert not web_manager.web_violation_queue.empty()

    async def test_no_ble_manager_for_sensor_does_not_raise(self, web_manager, mock_db):
        mock_db.get_warning_id.return_value = "warn-abc"
        web_manager.ble_managers = {}
        session = self._make_session(200)
        event = {
            "type": "violation_resolve",
            "sensor_name": "S1", "limit_key": "max_temp",
            "measurement_type": "TEMPERATURE",
        }
        await web_manager._handle_violation_resolve(session, event)

    async def test_401_triggers_token_refresh(self, web_manager, mock_db, mock_auth):
        """A 401 on PATCH must call auth.refresh_if_needed."""
        mock_db.get_warning_id.return_value = "warn-401"

        ok_resp = AsyncMock()
        ok_resp.status = 200
        ok_resp.raise_for_status = MagicMock()
        ok_resp.__aenter__ = AsyncMock(return_value=ok_resp)
        ok_resp.__aexit__ = AsyncMock(return_value=False)

        unauth_resp = AsyncMock()
        unauth_resp.status = 401
        unauth_resp.__aenter__ = AsyncMock(return_value=unauth_resp)
        unauth_resp.__aexit__ = AsyncMock(return_value=False)

        call_n = {"n": 0}

        def _patch(*a, **kw):
            call_n["n"] += 1
            return unauth_resp if call_n["n"] == 1 else ok_resp

        session = AsyncMock()
        session.patch = MagicMock(side_effect=_patch)

        event = {
            "type": "violation_resolve",
            "sensor_name": "S1", "limit_key": "max_temp",
            "measurement_type": "TEMPERATURE",
        }
        await web_manager._handle_violation_resolve(session, event)
        mock_auth.refresh_if_needed.assert_awaited()


class TestRunOutgoingStatusWorker:
    """run_outgoing_status_worker() PATCHes sensor online/offline status to the server."""

    async def _run_one(self, web_manager, event):
        await web_manager.status_queue.put(event)
        task = asyncio.create_task(web_manager.run_outgoing_status_worker())
        for _ in range(20):
            await asyncio.sleep(0)
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass

    def _make_session(self, status=200):
        resp = AsyncMock()
        resp.status = status
        resp.raise_for_status = MagicMock()
        resp.__aenter__ = AsyncMock(return_value=resp)
        resp.__aexit__ = AsyncMock(return_value=False)
        cm = AsyncMock()
        cm.__aenter__ = AsyncMock(return_value=AsyncMock(
            patch=MagicMock(return_value=resp)
        ))
        cm.__aexit__ = AsyncMock(return_value=False)
        return cm

    async def test_successful_patch_drains_queue(self, web_manager, mock_auth):
        event = {"read_uuid": "r-uuid", "status": "ONLINE", "sensor_name": "S1"}

        resp = AsyncMock()
        resp.status = 200
        resp.__aenter__ = AsyncMock(return_value=resp)
        resp.__aexit__ = AsyncMock(return_value=False)

        session = AsyncMock()
        session.patch = MagicMock(return_value=resp)
        session.__aenter__ = AsyncMock(return_value=session)
        session.__aexit__ = AsyncMock(return_value=False)

        with patch("app.web_manager.aiohttp.ClientSession", return_value=session):
            await self._run_one(web_manager, event)

        assert web_manager.status_queue.empty()

    async def test_unexpected_status_logs_warning(self, web_manager, mock_auth, caplog):
        import logging
        event = {"read_uuid": "r-uuid", "status": "ONLINE", "sensor_name": "S1"}

        resp = AsyncMock()
        resp.status = 500
        resp.__aenter__ = AsyncMock(return_value=resp)
        resp.__aexit__ = AsyncMock(return_value=False)

        session = AsyncMock()
        session.patch = MagicMock(return_value=resp)
        session.__aenter__ = AsyncMock(return_value=session)
        session.__aexit__ = AsyncMock(return_value=False)

        with (
            patch("app.web_manager.aiohttp.ClientSession", return_value=session),
            caplog.at_level(logging.WARNING, logger="app.web_manager"),
        ):
            await self._run_one(web_manager, event)

        assert any("500" in r.message for r in caplog.records)

    async def test_network_error_requeues_event(self, web_manager, mock_auth):
        event = {"read_uuid": "r-uuid", "status": "ONLINE", "sensor_name": "S1"}

        session = AsyncMock()
        session.patch = MagicMock(side_effect=Exception("timeout"))
        session.__aenter__ = AsyncMock(return_value=session)
        session.__aexit__ = AsyncMock(return_value=False)

        with (
            patch("app.web_manager.aiohttp.ClientSession", return_value=session),
            patch("app.web_manager.asyncio.sleep", new=AsyncMock()),
        ):
            await self._run_one(web_manager, event)

        assert not web_manager.status_queue.empty()

    async def test_401_triggers_token_refresh(self, web_manager, mock_auth):
        event = {"read_uuid": "r-uuid", "status": "ONLINE", "sensor_name": "S1"}

        ok_resp = AsyncMock()
        ok_resp.status = 200
        ok_resp.raise_for_status = MagicMock()
        ok_resp.__aenter__ = AsyncMock(return_value=ok_resp)
        ok_resp.__aexit__ = AsyncMock(return_value=False)

        unauth_resp = AsyncMock()
        unauth_resp.status = 401
        unauth_resp.__aenter__ = AsyncMock(return_value=unauth_resp)
        unauth_resp.__aexit__ = AsyncMock(return_value=False)

        call_n = {"n": 0}

        def _patch(*a, **kw):
            call_n["n"] += 1
            return unauth_resp if call_n["n"] == 1 else ok_resp

        session = AsyncMock()
        session.patch = MagicMock(side_effect=_patch)
        session.__aenter__ = AsyncMock(return_value=session)
        session.__aexit__ = AsyncMock(return_value=False)

        with patch("app.web_manager.aiohttp.ClientSession", return_value=session):
            await self._run_one(web_manager, event)

        mock_auth.refresh_if_needed.assert_awaited()

# Helper to wait for a queue to be processed without infinite blocking
async def wait_for_queue_empty(queue, timeout=2):
    for _ in range(int(timeout / 0.05)):
        if queue.empty():
            return True
        await asyncio.sleep(0.05)
    return False

class TestEndpointValidations:
    """HTTP endpoints return 4xx on invalid, missing, or malformed request data."""

    async def test_sensors_delete_and_flush(self, client):
        # Coverage for S_DELETE and FLUSH branches
        for t in ["SENSOR_DELETE", "FLUSH"]:
            url = '/api/sensors?sensorIds=s1' if t == "SENSOR_DELETE" else '/api/sensors'
            resp = await client.post(url, json={"updateType": t})
            assert resp.status == 202

    async def test_config_post_success(self, client, web_manager):
        # This covers the trigger of the background task
        with patch.object(web_manager, '_task_config_change', return_value=AsyncMock()):
            resp = await client.post('/api/config', json={"raspberryPi": "pi-1", "frequency": 60})
            assert resp.status == 202

    async def test_limits_invalid_json_returns_400(self, client):
        resp = await client.post('/api/limits', data=b'not-json', headers={'Content-Type': 'application/json'})
        assert resp.status == 400

    async def test_limits_no_limit_fields_returns_400(self, client):
        resp = await client.post('/api/limits', json={"irrelevant": "data"})
        assert resp.status == 400

    async def test_occupancy_invalid_json_returns_400(self, client):
        resp = await client.post('/api/occupancy', data=b'not-json', headers={'Content-Type': 'application/json'})
        assert resp.status == 400

    async def test_occupancy_missing_field_returns_400(self, client):
        resp = await client.post('/api/occupancy', json={"other": "field"})
        assert resp.status == 400

    async def test_occupancy_negative_value_returns_400(self, client):
        resp = await client.post('/api/occupancy', json={"effectiveOccupancy": -1})
        assert resp.status == 400

    async def test_sensors_invalid_json_returns_400(self, client):
        resp = await client.post('/api/sensors', data=b'not-json', headers={'Content-Type': 'application/json'})
        assert resp.status == 400

    async def test_sensors_unknown_update_type_returns_400(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "UNKNOWN_TYPE"})
        assert resp.status == 400

    async def test_sensors_add_without_ids_returns_400(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "SENSOR_ADD"})
        assert resp.status == 400

    async def test_sensors_delete_without_ids_returns_400(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "SENSOR_DELETE"})
        assert resp.status == 400

    async def test_config_invalid_json_returns_400(self, client):
        resp = await client.post('/api/config', data=b'not-json', headers={'Content-Type': 'application/json'})
        assert resp.status == 400

    async def test_config_missing_pi_id_returns_400(self, client):
        resp = await client.post('/api/config', json={"frequency": 60})
        assert resp.status == 400


class TestConfigTaskEdges:
    """_task_config_change() handles non-first-boot, removed sensors, and fetch errors."""

    async def test_config_change_not_first_boot(self, web_manager, mock_db):
        web_manager._first_boot = False
        mock_db.get_config.return_value = "100"
        
        with patch("app.web_manager.ConfigManager.fetch_and_seed", new=AsyncMock()):
            await web_manager._task_config_change("pi-1")
            
        # Should NOT call set_config for initial_config_done if first_boot is False
        mock_db.set_config.assert_not_called()

    async def test_config_change_removes_sensor_no_longer_in_config(self, web_manager, mock_db):
        """Sensors absent from the re-seeded config have their removal_event set immediately."""
        mock_db.get_config.return_value = None   # no room_id or frequency to broadcast
        mock_db.get_sensors.return_value = []    # sensor no longer present

        ble = MagicMock()
        ble.removal_event = asyncio.Event()
        ble.ble_inbox = asyncio.Queue()
        web_manager.ble_managers = {"S1": ble}

        with patch("app.web_manager.ConfigManager.fetch_and_seed", new=AsyncMock()):
            await web_manager._task_config_change("pi-1")

        assert ble.removal_event.is_set()

    async def test_config_fetch_exception_logging(self, web_manager, mock_db):
        # Trigger the 'except Exception as e' block in _task_config_change
        with patch("app.web_manager.ConfigManager.fetch_and_seed", side_effect=RuntimeError("API Down")):
            await web_manager._task_config_change("pi-1")
            
        # Verify it logged to DB
        mock_db.log_event.assert_called_with("CONFIG", "Failed to handle config change: API Down", "ERROR")

class TestLocalServerLoop:
    """run_local_server() starts aiohttp, serves indefinitely, and cleans up on cancel."""

    async def test_run_local_server_cancel(self, web_manager):
        # Covers the startup lines and the infinite sleep loop
        runner = AsyncMock()
        site = AsyncMock()
        with (
            patch("app.web_manager.web.AppRunner", return_value=runner),
            patch("app.web_manager.web.TCPSite", return_value=site),
            patch("app.web_manager.asyncio.sleep", side_effect=asyncio.CancelledError)
        ):
            try:
                await web_manager.run_local_server()
            except asyncio.CancelledError:
                pass
        
        runner.setup.assert_called_once()
        site.start.assert_called_once()
