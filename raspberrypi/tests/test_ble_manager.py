import asyncio
import pytest
from unittest.mock import AsyncMock, MagicMock, patch, call
from bleak.exc import BleakError

# helpers 

def make_fake_client(is_connected=True):
    """Returns a mock BleakClient that behaves like an async context manager."""
    client = MagicMock()
    client.is_connected = is_connected
    client.start_notify = AsyncMock()
    client.write_gatt_char = AsyncMock()

    client.__aenter__ = AsyncMock(return_value=client)
    client.__aexit__ = AsyncMock(return_value=False)
    return client


# sync / property tests (no event loop needed)

class TestProperties:
    def test_name_returns_sensor_name(self, ble_manager):
        assert ble_manager.name == "G1T4:S3M2"

    def test_read_uuid_returns_char_uuid(self, ble_manager):
        assert ble_manager.read_uuid == "0000ffe1-0000-1000-8000-00805f9b34fb"


class TestDisconnectedCallback:
    def test_sets_disconnect_event(self, ble_manager):
        assert not ble_manager.disconnect_event.is_set()
        ble_manager.disconnected_callback(MagicMock())
        assert ble_manager.disconnect_event.is_set()


class TestNotificationHandler:
    def test_decoded_message_put_on_queue(self, ble_manager, queues):
        raw = b"  hello world  "
        ble_manager.notification_handler(sender=None, data=raw)
        # call_soon_threadsafe schedules put_nowait; drain the queue
        asyncio.get_event_loop().run_until_complete(asyncio.sleep(0))
        assert not queues["processing"].empty()
        assert queues["processing"].get_nowait() == "hello world"

    def test_strips_whitespace(self, ble_manager, queues):
        ble_manager.notification_handler(None, b"\n  42.5 \r\n")
        asyncio.get_event_loop().run_until_complete(asyncio.sleep(0))
        assert queues["processing"].get_nowait() == "42.5"


# async tests 

class TestSenderTask:
    async def test_sends_command_to_arduino(self, ble_manager, queues):
        fake_client = make_fake_client()
        ble_manager.client = fake_client
        write_uuid = "0000ffe2-0000-1000-8000-00805f9b34fb"

        await queues["inbox"].put("FREQUENCY:30")

        # run sender for just long enough to process one item, then disconnect
        async def stop_after_send():
            await asyncio.sleep(0.05)
            ble_manager.client.is_connected = False

        await asyncio.gather(
            ble_manager._sender_task(write_uuid),
            stop_after_send(),
        )

        fake_client.write_gatt_char.assert_awaited_once_with(
            write_uuid, b"FREQUENCY:30", response=False
        )

    async def test_sets_disconnect_event_on_write_failure(self, ble_manager, queues):
        fake_client = make_fake_client()
        fake_client.write_gatt_char.side_effect = BleakError("write failed")
        ble_manager.client = fake_client

        await queues["inbox"].put("CMD")
        await ble_manager._sender_task("some-uuid")

        assert ble_manager.disconnect_event.is_set()

    async def test_timeout_does_not_crash_loop(self, ble_manager):
        fake_client = make_fake_client()
        ble_manager.client = fake_client
    
        iteration = 0
    
        async def fake_wait_for(coro, timeout):
            coro.close()  
            nonlocal iteration
            iteration += 1
            if iteration >= 3:
                ble_manager.client.is_connected = False  
            raise asyncio.TimeoutError
    
        with patch("app.ble_manager.asyncio.wait_for", side_effect=fake_wait_for):
            await ble_manager._sender_task("uuid")
    
        assert iteration >= 3  


class TestRunLoop:
    async def test_exits_when_sensor_removed_from_db(self, ble_manager, mock_db):
        """If get_sensors() returns nothing, run() should return immediately."""
        mock_db.get_sensors.return_value = []

        # run() has a `while True` but returns on missing sensor_cfg
        await asyncio.wait_for(ble_manager.run(), timeout=1.0)

    async def test_device_not_found_reports_offline_after_5_attempts(
        self, ble_manager, mock_db, queues
    ):
        with patch("app.ble_manager.BleakScanner.find_device_by_filter", new_callable=AsyncMock, return_value=None), \
             patch("asyncio.sleep", new_callable=AsyncMock):

            async def trigger_reconnect():
                await asyncio.sleep(0)
                ble_manager.reconnect_event.set()
                await asyncio.sleep(0)
                # cancel the outer task after one cycle
                raise asyncio.CancelledError

            with pytest.raises(asyncio.CancelledError):
                await asyncio.gather(ble_manager.run(), trigger_reconnect())

        status = queues["status"].get_nowait()
        assert status["status"] == "OFFLINE"
        assert status["sensor_name"] == ble_manager.name

    async def test_successful_connect_sends_online_status(
        self, ble_manager, mock_db, queues
    ):
        fake_client = make_fake_client()

        with patch("app.ble_manager.BleakScanner.find_device_by_filter", new_callable=AsyncMock, return_value=MagicMock()), \
             patch("app.ble_manager.BleakClient", return_value=fake_client), \
             patch("asyncio.sleep", new_callable=AsyncMock):

            # Trigger disconnect_event to make run() exit the connected state cleanly
            async def trigger_disconnect():
                await asyncio.sleep(0)
                ble_manager.disconnect_event.set()

            # run() will loop again after disconnect; cancel on second iteration
            call_count = 0
            original_get_sensors = mock_db.get_sensors

            async def get_sensors_limited():
                nonlocal call_count
                call_count += 1
                if call_count > 3:
                    return []           # sensor "removed" → run() returns
                return await original_get_sensors()

            mock_db.get_sensors = get_sensors_limited

            await asyncio.gather(ble_manager.run(), trigger_disconnect())

        status = queues["status"].get_nowait()
        assert status["status"] == "ONLINE"
        assert status["sensor_name"] == ble_manager.name

    async def test_time_and_frequency_written_on_connect(
        self, ble_manager, mock_db, queues
    ):
        fake_client = make_fake_client()
        mock_db.get_config.return_value = "30"   # frequency = 30

        with patch("app.ble_manager.BleakScanner.find_device_by_filter", new_callable=AsyncMock, return_value=MagicMock()), \
             patch("app.ble_manager.BleakClient", return_value=fake_client), \
             patch("asyncio.sleep", new_callable=AsyncMock):

            async def trigger_disconnect():
                await asyncio.sleep(0)
                ble_manager.disconnect_event.set()

            call_count = 0
            async def get_sensors_limited():
                nonlocal call_count
                call_count += 1
                return [] if call_count > 3 else [ble_manager.sensor]

            mock_db.get_sensors = get_sensors_limited
            await asyncio.gather(ble_manager.run(), trigger_disconnect())

        calls = fake_client.write_gatt_char.call_args_list
        write_uuid = ble_manager.sensor["write_uuid"]

        # First write must be TIME:..., second must be FREQUENCY:30
        assert calls[0][0][0] == write_uuid
        assert calls[0][0][1].startswith(b"TIME:")
        assert calls[1][0][1] == b"FREQUENCY:30"

    async def test_bleak_error_retries_and_logs(self, ble_manager, mock_db, queues):
        fake_client = make_fake_client()
        fake_client.__aenter__.side_effect = BleakError("connection refused")

        with patch("app.ble_manager.BleakScanner.find_device_by_filter", new_callable=AsyncMock, return_value=MagicMock()), \
             patch("app.ble_manager.BleakClient", return_value=fake_client), \
             patch("asyncio.sleep", new_callable=AsyncMock):

            async def set_reconnect():
                await asyncio.sleep(0)
                ble_manager.reconnect_event.set()
                await asyncio.sleep(0)
                raise asyncio.CancelledError

            with pytest.raises(asyncio.CancelledError):
                await asyncio.gather(ble_manager.run(), set_reconnect())

        mock_db.log_event.assert_any_call(
            "BLE",
            pytest.approx("Failed to connect to G1T4:S3M2 after 5 attempts", abs=0),
            "ERROR",
        )
