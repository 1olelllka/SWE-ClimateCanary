import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from aiohttp import ClientResponseError
from app.config_manager import ConfigManager


# shared fixtures

@pytest.fixture
def mock_db():
    db = AsyncMock()
    db.get_config.return_value = "http://test-server:8080"
    return db

@pytest.fixture
def mock_auth():
    auth = MagicMock()
    auth.get_headers.return_value = {"Authorization": "Bearer tok"}
    auth.refresh_if_needed = AsyncMock()
    return auth

FULL_REMOTE = {
    "roomId": "room-1",
    "frequency": 60,
    "limits": {
        "tempMax": 30.0, "tempMin": 18.0,
        "humMax": 70.0,  "humMin": 30.0,
        "co2Max": 1000.0,
    },
    "occupancy": {"effectiveOccupancy": 10, "privacyMode": False},
    "sensors": [
        {"name": "S1", "readId": "char-uuid-1", "writeId": "write-uuid-1"},
        {"name": "S2", "readId": "char-uuid-2", "writeId": "write-uuid-2"},
    ],
}

def make_http_session(response_json, status=200, raise_for_status_effect=None):
    """Build a fake aiohttp session where session.get() is a sync-returning context manager."""
    mock_response = MagicMock()
    mock_response.status = status
    mock_response.raise_for_status = MagicMock(side_effect=raise_for_status_effect)
    mock_response.json = AsyncMock(return_value=response_json)
    mock_response.__aenter__ = AsyncMock(return_value=mock_response)
    mock_response.__aexit__ = AsyncMock(return_value=False)

    mock_session = MagicMock()
    mock_session.get = MagicMock(return_value=mock_response)
    mock_session.__aenter__ = AsyncMock(return_value=mock_session)
    mock_session.__aexit__ = AsyncMock(return_value=False)

    return mock_session, mock_response


# ConfigManager._fetch

class TestFetch:
    async def test_returns_json_on_success(self, mock_auth):
        mock_session, _ = make_http_session({"key": "val"})
        with patch("app.config_manager.aiohttp.ClientSession", return_value=mock_session):
            result = await ConfigManager._fetch("http://server/api/test", mock_auth)
        assert result == {"key": "val"}

    async def test_refreshes_token_on_401_and_retries(self, mock_auth):
        retry_response = MagicMock()
        retry_response.raise_for_status = MagicMock()
        retry_response.json = AsyncMock(return_value={"retried": True})
        retry_response.__aenter__ = AsyncMock(return_value=retry_response)
        retry_response.__aexit__ = AsyncMock(return_value=False)

        first_response = MagicMock()
        first_response.status = 401
        first_response.raise_for_status = MagicMock()
        first_response.json = AsyncMock(return_value={})
        first_response.__aenter__ = AsyncMock(return_value=first_response)
        first_response.__aexit__ = AsyncMock(return_value=False)

        mock_session = MagicMock()
        mock_session.get = MagicMock(side_effect=[first_response, retry_response])
        mock_session.__aenter__ = AsyncMock(return_value=mock_session)
        mock_session.__aexit__ = AsyncMock(return_value=False)

        with patch("app.config_manager.aiohttp.ClientSession", return_value=mock_session):
            result = await ConfigManager._fetch("http://server/api/test", mock_auth)

        mock_auth.refresh_if_needed.assert_awaited_once()
        assert result == {"retried": True}

    async def test_raises_on_non_401_error(self, mock_auth):
        mock_session, _ = make_http_session(
            {},
            status=500,
            raise_for_status_effect=ClientResponseError(
                request_info=MagicMock(), history=(), status=500
            ),
        )
        with patch("app.config_manager.aiohttp.ClientSession", return_value=mock_session):
            with pytest.raises(ClientResponseError):
                await ConfigManager._fetch("http://server/api/test", mock_auth)


# ConfigManager.fetch_and_seed

class TestFetchAndSeed:
    async def test_seeds_all_fields_from_remote(self, mock_db, mock_auth):
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=FULL_REMOTE)):
            await ConfigManager.fetch_and_seed("pi-id-1", "http://server", mock_db, mock_auth)

        mock_db.set_config.assert_any_call("raspberry_id", "pi-id-1")
        mock_db.set_config.assert_any_call("room_id", "room-1")
        mock_db.set_config.assert_any_call("frequency", "60")
        mock_db.set_limit.assert_any_call("max_temp", 30.0)
        mock_db.set_limit.assert_any_call("min_temp", 18.0)
        mock_db.set_limit.assert_any_call("max_moisture", 70.0)
        mock_db.set_limit.assert_any_call("min_moisture", 30.0)
        mock_db.set_limit.assert_any_call("max_co2", 1000.0)
        mock_db.set_limit.assert_any_call("current_occupancy", 10.0)
        mock_db.set_limit.assert_any_call("privacy_mode", 0.0)

    async def test_seeds_sensors(self, mock_db, mock_auth):
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=FULL_REMOTE)):
            await ConfigManager.fetch_and_seed("pi-id-1", "http://server", mock_db, mock_auth)

        mock_db.set_sensors.assert_awaited_once_with([
            {"name": "S1", "char_uuid": "char-uuid-1", "write_uuid": "write-uuid-1"},
            {"name": "S2", "char_uuid": "char-uuid-2", "write_uuid": "write-uuid-2"},
        ])

    async def test_continues_gracefully_when_remote_unreachable(self, mock_db, mock_auth):
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(side_effect=Exception("timeout"))):
            await ConfigManager.fetch_and_seed("pi-id-1", "http://server", mock_db, mock_auth)

        mock_db.set_config.assert_any_call("raspberry_id", "pi-id-1")
        mock_db.set_config.assert_any_call("server_url", "http://server")
        mock_db.set_limit.assert_not_called()

    async def test_sensors_without_name_are_skipped(self, mock_db, mock_auth):
        remote = {**FULL_REMOTE, "sensors": [
            {"name": None, "readId": "r1", "writeId": "w1"},
            {"name": "S1", "readId": "r2", "writeId": "w2"},
        ]}
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=remote)):
            await ConfigManager.fetch_and_seed("pi-id-1", "http://server", mock_db, mock_auth)

        sensors_call = mock_db.set_sensors.call_args[0][0]
        assert len(sensors_call) == 1
        assert sensors_call[0]["name"] == "S1"

    async def test_null_frequency_stored_as_none(self, mock_db, mock_auth):
        remote = {**FULL_REMOTE, "frequency": None}
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=remote)):
            await ConfigManager.fetch_and_seed("pi-id-1", "http://server", mock_db, mock_auth)
        mock_db.set_config.assert_any_call("frequency", None)

    async def test_privacy_mode_true_stored_as_1(self, mock_db, mock_auth):
        remote = {**FULL_REMOTE, "occupancy": {"effectiveOccupancy": 5, "privacyMode": True}}
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=remote)):
            await ConfigManager.fetch_and_seed("pi-id-1", "http://server", mock_db, mock_auth)
        mock_db.set_limit.assert_any_call("privacy_mode", 1.0)


# ConfigManager.handle_limit_change

class TestHandleLimitChange:
    async def test_updates_all_present_limits(self, mock_db):
        payload = {"tempMax": 30, "tempMin": 18, "humMax": 70, "humMin": 30, "co2Max": 1000}
        await ConfigManager.handle_limit_change(mock_db, payload)
        mock_db.set_limit.assert_any_call("max_temp", 30.0)
        mock_db.set_limit.assert_any_call("min_temp", 18.0)
        mock_db.set_limit.assert_any_call("max_moisture", 70.0)
        mock_db.set_limit.assert_any_call("min_moisture", 30.0)
        mock_db.set_limit.assert_any_call("max_co2", 1000.0)

    async def test_skips_null_fields(self, mock_db):
        await ConfigManager.handle_limit_change(mock_db, {"tempMax": 25, "tempMin": None})
        calls = [c[0][0] for c in mock_db.set_limit.call_args_list]
        assert "max_temp" in calls
        assert "min_temp" not in calls

    async def test_empty_payload_does_not_call_set_limit(self, mock_db):
        await ConfigManager.handle_limit_change(mock_db, {})
        mock_db.set_limit.assert_not_called()


# ConfigManager.handle_occupancy_change

class TestHandleOccupancyChange:
    async def test_updates_both_fields(self, mock_db):
        await ConfigManager.handle_occupancy_change(
            mock_db, {"effectiveOccupancy": 8, "privacyMode": True}
        )
        mock_db.set_limit.assert_any_call("current_occupancy", 8.0)
        mock_db.set_limit.assert_any_call("privacy_mode", 1.0)

    async def test_privacy_false_stored_as_0(self, mock_db):
        await ConfigManager.handle_occupancy_change(
            mock_db, {"effectiveOccupancy": None, "privacyMode": False}
        )
        mock_db.set_limit.assert_any_call("privacy_mode", 0.0)

    async def test_empty_payload_does_not_call_set_limit(self, mock_db):
        await ConfigManager.handle_occupancy_change(mock_db, {})
        mock_db.set_limit.assert_not_called()


# ConfigManager.handle_sensor_add

class TestHandleSensorAdd:
    async def test_adds_sensor_from_remote(self, mock_db, mock_auth):
        mock_db.get_config.return_value = "http://server"
        remote_sensor = {"name": "S3", "readId": "r3", "writeId": "w3"}
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=remote_sensor)):
            await ConfigManager.handle_sensor_add(mock_db, mock_auth, ["sensor-id-3"])
        mock_db.add_sensors.assert_awaited_once_with([
            {"name": "S3", "char_uuid": "r3", "write_uuid": "w3"}
        ])

    async def test_raises_when_server_url_missing(self, mock_db, mock_auth):
        mock_db.get_config.return_value = None
        with pytest.raises(RuntimeError, match="server_url missing"):
            await ConfigManager.handle_sensor_add(mock_db, mock_auth, ["id"])


# ConfigManager.handle_sensor_delete / flush

class TestHandleSensorDelete:
    async def test_deletes_by_ids(self, mock_db):
        await ConfigManager.handle_sensor_delete(mock_db, ["id-1", "id-2"])
        mock_db.delete_sensors_by_ids.assert_awaited_once_with(["id-1", "id-2"])


class TestHandleSensorFlush:
    async def test_deletes_all_sensors(self, mock_db):
        await ConfigManager.handle_sensor_flush(mock_db)
        mock_db.delete_all_sensors.assert_awaited_once()


# ConfigManager.handle_config_change

class TestHandleConfigChange:
    async def test_updates_room_and_frequency(self, mock_db, mock_auth):
        mock_db.get_config.return_value = "http://server"
        remote = {"roomId": "room-99", "frequency": 30}
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=remote)):
            result = await ConfigManager.handle_config_change(mock_db, mock_auth, "pi-1")
        mock_db.set_config.assert_any_call("room_id", "room-99")
        mock_db.set_config.assert_any_call("frequency", "30")
        assert result == "30"

    async def test_returns_none_when_frequency_missing(self, mock_db, mock_auth):
        mock_db.get_config.return_value = "http://server"
        remote = {"roomId": "room-1", "frequency": None}
        with patch.object(ConfigManager, "_fetch", new=AsyncMock(return_value=remote)):
            result = await ConfigManager.handle_config_change(mock_db, mock_auth, "pi-1")
        assert result is None

    async def test_raises_when_server_url_missing(self, mock_db, mock_auth):
        mock_db.get_config.return_value = None
        with pytest.raises(RuntimeError, match="server_url missing"):
            await ConfigManager.handle_config_change(mock_db, mock_auth, "pi-1")
