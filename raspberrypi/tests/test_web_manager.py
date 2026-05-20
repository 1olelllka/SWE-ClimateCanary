import asyncio
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from aiohttp import web
from aiohttp.test_utils import TestClient, TestServer
from app.web_manager import WebManager


# fixtures 

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

    # patch create_task so background tasks don't actually run during handler tests
    with patch("app.web_manager.asyncio.create_task", side_effect=_noop_task):
        async with TestClient(TestServer(app)) as c:
            yield c


# handle_limits 

class TestHandleLimits:
    async def test_valid_payload_returns_202(self, client):
        resp = await client.post('/api/limits', json={"tempMax": 30})
        assert resp.status == 202
        data = await resp.json()
        assert data["status"] == "accepted"

    async def test_missing_all_limit_fields_returns_400(self, client):
        resp = await client.post('/api/limits', json={"unrelated": "field"})
        assert resp.status == 400

    async def test_invalid_json_returns_400(self, client):
        resp = await client.post('/api/limits', data="not-json",
                                 headers={"Content-Type": "application/json"})
        assert resp.status == 400

    async def test_all_limit_fields_accepted(self, client):
        payload = {"tempMin": 18, "tempMax": 30, "humMin": 30, "humMax": 70, "co2Max": 1000}
        resp = await client.post('/api/limits', json=payload)
        assert resp.status == 202


# handle_occupancy 

class TestHandleOccupancy:
    async def test_valid_payload_returns_202(self, client):
        resp = await client.post('/api/occupancy',
                                 json={"effectiveOccupancy": 5, "privacyMode": False})
        assert resp.status == 202

    async def test_missing_effective_occupancy_returns_400(self, client):
        resp = await client.post('/api/occupancy', json={"privacyMode": False})
        assert resp.status == 400

    async def test_negative_occupancy_returns_400(self, client):
        resp = await client.post('/api/occupancy', json={"effectiveOccupancy": -1})
        assert resp.status == 400

    async def test_non_integer_occupancy_returns_400(self, client):
        resp = await client.post('/api/occupancy', json={"effectiveOccupancy": 3.5})
        assert resp.status == 400

    async def test_zero_occupancy_is_valid(self, client):
        resp = await client.post('/api/occupancy', json={"effectiveOccupancy": 0})
        assert resp.status == 202

    async def test_invalid_json_returns_400(self, client):
        resp = await client.post('/api/occupancy', data="bad",
                                 headers={"Content-Type": "application/json"})
        assert resp.status == 400


# handle_sensors 

class TestHandleSensors:
    async def test_sensor_add_with_ids_returns_202(self, client):
        resp = await client.post('/api/sensors?sensorIds=id-1',
                                 json={"updateType": "SENSOR_ADD"})
        assert resp.status == 202

    async def test_sensor_delete_with_ids_returns_202(self, client):
        resp = await client.post('/api/sensors?sensorIds=id-1',
                                 json={"updateType": "SENSOR_DELETE"})
        assert resp.status == 202

    async def test_flush_without_ids_returns_202(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "FLUSH"})
        assert resp.status == 202

    async def test_unknown_update_type_returns_400(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "UNKNOWN"})
        assert resp.status == 400

    async def test_sensor_add_without_ids_returns_400(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "SENSOR_ADD"})
        assert resp.status == 400

    async def test_sensor_delete_without_ids_returns_400(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "SENSOR_DELETE"})
        assert resp.status == 400

    async def test_update_type_is_case_insensitive(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "flush"})
        assert resp.status == 202

    async def test_response_includes_update_type(self, client):
        resp = await client.post('/api/sensors', json={"updateType": "FLUSH"})
        data = await resp.json()
        assert data["updateType"] == "FLUSH"

    async def test_invalid_json_returns_400(self, client):
        resp = await client.post('/api/sensors', data="bad",
                                 headers={"Content-Type": "application/json"})
        assert resp.status == 400


# handle_config 

class TestHandleConfig:
    async def test_valid_payload_returns_202(self, client):
        resp = await client.post('/api/config', json={"raspberryPi": "pi-id-1"})
        assert resp.status == 202

    async def test_missing_raspberry_pi_field_returns_400(self, client):
        resp = await client.post('/api/config', json={"other": "field"})
        assert resp.status == 400

    async def test_invalid_json_returns_400(self, client):
        resp = await client.post('/api/config', data="bad",
                                 headers={"Content-Type": "application/json"})
        assert resp.status == 400


# handle_retry_sensor

class TestHandleRetrySensor:
    def _make_ble(self, read_uuid, write_uuid):
        ble = MagicMock()
        ble.read_uuid = read_uuid
        ble.sensor = {"write_uuid": write_uuid}
        ble.reconnect_event = MagicMock()
        return ble

    async def test_matching_by_read_uuid_triggers_reconnect(self, client, web_manager):
        ble = self._make_ble("char-uuid-1", "write-uuid-1")
        web_manager.ble_managers = {"S1": ble}
        resp = await client.post('/api/retry-sensor?sensorIds=char-uuid-1')
        assert resp.status == 202
        ble.reconnect_event.set.assert_called_once()

    async def test_matching_by_write_uuid_triggers_reconnect(self, client, web_manager):
        ble = self._make_ble("char-uuid-1", "write-uuid-1")
        web_manager.ble_managers = {"S1": ble}
        resp = await client.post('/api/retry-sensor?sensorIds=write-uuid-1')
        assert resp.status == 202
        ble.reconnect_event.set.assert_called_once()

    async def test_no_matching_sensor_returns_404(self, client, web_manager):
        web_manager.ble_managers = {}
        resp = await client.post('/api/retry-sensor?sensorIds=unknown-id')
        assert resp.status == 404

    async def test_missing_sensor_ids_returns_400(self, client):
        resp = await client.post('/api/retry-sensor')
        assert resp.status == 400

    async def test_response_includes_triggered_sensor(self, client, web_manager):
        ble = self._make_ble("char-uuid-1", "write-uuid-1")
        web_manager.ble_managers = {"S1": ble}
        resp = await client.post('/api/retry-sensor?sensorIds=char-uuid-1')
        data = await resp.json()
        assert "S1" in data["triggered"]


#  _build_ble_warn_message 

class TestBuildBleWarnMessage:
    async def test_message_format_with_active_violation(self, web_manager, mock_db):
        mock_db.get_active_violations.return_value = [
            {"type": "max_temp", "threshold_value": 30.0}
        ]
        msg = await web_manager._build_ble_warn_message("S1", "TEMPERATURE", "YELLOW", "Open window")
        assert msg.startswith("WARNTEXT:")
        assert "THRESHOLD:30.0" in msg
        assert "TIP:Open window" in msg
        assert "STATUS:YELLOW" in msg

    async def test_message_direction_above_for_max(self, web_manager, mock_db):
        mock_db.get_active_violations.return_value = [
            {"type": "max_temp", "threshold_value": 30.0}
        ]
        msg = await web_manager._build_ble_warn_message("S1", "TEMPERATURE", "RED", "tip")
        assert "above" in msg

    async def test_message_direction_below_for_min(self, web_manager, mock_db):
        mock_db.get_active_violations.return_value = [
            {"type": "min_temp", "threshold_value": 18.0}
        ]
        msg = await web_manager._build_ble_warn_message("S1", "TEMPERATURE", "RED", "tip")
        assert "below" in msg

    async def test_fallback_threshold_when_no_active_violation(self, web_manager, mock_db):
        mock_db.get_active_violations.return_value = []
        msg = await web_manager._build_ble_warn_message("S1", "TEMPERATURE", "GREEN", "tip")
        assert "THRESHOLD:N/A" in msg


# _task_config_change 

class TestTaskConfigChange:
    async def test_first_boot_sets_config_ready_event(self, mock_db, mock_auth):
        event = asyncio.Event()
        wm = WebManager(
            db=mock_db,
            local_listen_port=8080,
            server_url="http://server",
            web_out_queue=asyncio.Queue(),
            web_violation_queue=asyncio.Queue(),
            auth=mock_auth,
            config_ready_event=event,
            first_boot=True,
        )
        mock_db.get_config.return_value = "60"

        with patch("app.web_manager.ConfigManager.fetch_and_seed", new=AsyncMock()):
            await wm._task_config_change("pi-id-1")

        assert event.is_set()
        mock_db.set_config.assert_any_call("initial_config_done", "1")

    async def test_non_first_boot_does_not_set_event(self, web_manager, mock_db):
        mock_db.get_config.return_value = "60"
        with patch("app.web_manager.ConfigManager.fetch_and_seed", new=AsyncMock()):
            await web_manager._task_config_change("pi-id-1")
        assert not web_manager.config_ready_event.is_set()

    async def test_broadcasts_frequency_to_all_ble_managers(self, web_manager, mock_db):
        mock_db.get_config.return_value = "30"
        inbox = asyncio.Queue()
        ble = MagicMock()
        ble.ble_inbox = inbox
        web_manager.ble_managers = {"S1": ble}

        with patch("app.web_manager.ConfigManager.fetch_and_seed", new=AsyncMock()):
            await web_manager._task_config_change("pi-id-1")

        assert inbox.get_nowait() == "FREQUENCY:30"

    async def test_exception_does_not_propagate(self, web_manager):
        with patch("app.web_manager.ConfigManager.fetch_and_seed",
                   new=AsyncMock(side_effect=Exception("boom"))):
            await web_manager._task_config_change("pi-id-1")   # should not raise


# _task_sensor_change 

class TestTaskSensorChange:
    async def test_sensor_delete_sets_removal_event(self, web_manager, mock_db):
        ble = MagicMock()
        ble.sensor = {"char_uuid": "char-1", "write_uuid": "write-1"}
        ble.removal_event = MagicMock()
        web_manager.ble_managers = {"S1": ble}

        with patch("app.web_manager.ConfigManager.handle_sensor_delete", new=AsyncMock()):
            await web_manager._task_sensor_change("SENSOR_DELETE", ["char-1"])

        ble.removal_event.set.assert_called_once()

    async def test_flush_sets_removal_event_on_all(self, web_manager, mock_db):
        ble1, ble2 = MagicMock(), MagicMock()
        ble1.removal_event = MagicMock()
        ble2.removal_event = MagicMock()
        web_manager.ble_managers = {"S1": ble1, "S2": ble2}

        with patch("app.web_manager.ConfigManager.handle_sensor_flush", new=AsyncMock()):
            await web_manager._task_sensor_change("FLUSH", [])

        ble1.removal_event.set.assert_called_once()
        ble2.removal_event.set.assert_called_once()

    async def test_exception_does_not_propagate(self, web_manager):
        with patch("app.web_manager.ConfigManager.handle_sensor_delete",
                   new=AsyncMock(side_effect=Exception("boom"))):
            await web_manager._task_sensor_change("SENSOR_DELETE", ["id-1"])  # should not raise
