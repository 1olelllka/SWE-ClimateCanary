import asyncio
import logging
import aiohttp
from aiohttp import web
from config_manager import ConfigManager
from ble_manager import BLEManager

logger = logging.getLogger(__name__)

SENSOR_UPDATE_TYPES = {"SENSOR_DELETE", "SENSOR_ADD", "FLUSH"}

# Maps TipCreateDTO ViolationType + ViolatedSensor to an Arduino sensor name prefix.
TIP_SENSOR_MAP = {
    "TEMPERATURE": "TEMPERATURE",
    "HUMIDITY":    "MOISTURE",
    "CO2":         "IAQ",
}


class WebManager:
    def __init__(self, static_config, db, web_out_queue, web_violation_queue, auth,
                 config_ready_event: asyncio.Event):
        self.static_config = static_config
        self.db = db
        self.web_out_queue = web_out_queue
        self.web_violation_queue  = web_violation_queue
        self.auth = auth
        self.local_port = static_config['webapp']['local_listen_port']
        self.server_url = static_config['webapp']['server_url']
        self.ble_managers: dict[str, BLEManager] = {}
        self.status_queue: asyncio.Queue = asyncio.Queue()  # BLEManager -> WebManager

        self.config_ready_event = config_ready_event

# Webapp -> Pi

    async def handle_limits(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
        except Exception:
            return web.json_response({"status": "error", "message": "Invalid JSON"}, status=400)

        limit_fields = {"tempMin", "tempMax", "humMin", "humMax", "co2Max"}
        if not any(data.get(f) is not None for f in limit_fields):
            logger.warning("[Web -> Pi] /api/limits: no limit fields present in payload.")
            return web.json_response(
                {"status": "error", "message": "No limit fields provided"}, status=400
            )

        logger.info(f"[Web -> Pi] /api/limits received: {data}")
        asyncio.create_task(self._task_limit_change(data))
        return web.json_response({"status": "accepted"}, status=202)

    async def handle_occupancy(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
        except Exception:
            return web.json_response({"status": "error", "message": "Invalid JSON"}, status=400)

        effective = data.get('effectiveOccupancy')
        if effective is None or not isinstance(effective, int) or effective < 0:
            logger.warning(f"[Web -> Pi] /api/occupancy: invalid effectiveOccupancy={effective!r}")
            return web.json_response(
                {"status": "error", "message": "effectiveOccupancy must be a non-negative integer"},
                status=400
            )

        logger.info(f"[Web -> Pi] /api/occupancy received: {data}")
        asyncio.create_task(self._task_occupancy_change(data))
        return web.json_response({"status": "accepted"}, status=202)

    async def handle_sensors(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
        except Exception:
            return web.json_response({"status": "error", "message": "Invalid JSON"}, status=400)

        update_type = str(data.get('updateType', '')).upper()
        if update_type not in SENSOR_UPDATE_TYPES:
            logger.warning(f"[Web -> Pi] /api/sensors: unknown updateType={update_type!r}")
            return web.json_response(
                {"status": "error",
                 "message": f"Unknown updateType '{update_type}'. Expected one of: {SENSOR_UPDATE_TYPES}"},
                status=400
            )

        raw_ids = request.rel_url.query.get('sensorIds', '')
        sensor_ids = [sid.strip() for sid in raw_ids.split(',') if sid.strip()] if raw_ids else []

        if update_type in ("SENSOR_ADD", "SENSOR_DELETE") and not sensor_ids:
            return web.json_response(
                {"status": "error",
                 "message": f"sensorIds query param is required for {update_type}"},
                status=400
            )

        logger.info(f"[Web -> Pi] /api/sensors: {update_type}, ids={sensor_ids}")
        asyncio.create_task(self._task_sensor_change(update_type, sensor_ids))
        return web.json_response({"status": "accepted", "updateType": update_type}, status=202)

    async def handle_config(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
        except Exception:
            return web.json_response({"status": "error", "message": "Invalid JSON"}, status=400)

        pi_id = data.get('raspberryPi')
        if not pi_id:
            logger.warning("[Web -> Pi] /api/config: missing raspberryPi field.")
            return web.json_response(
                {"status": "error", "message": "raspberryPi field is required"}, status=400
            )

        logger.info(f"[Web -> Pi] /api/config received for pi_id={pi_id}")
        asyncio.create_task(self._task_config_change(str(pi_id)))
        return web.json_response({"status": "accepted"}, status=202)

    async def handle_retry_sensor(self, request: web.Request) -> web.Response:
        raw_ids = request.rel_url.query.get('sensorIds', '')
        sensor_ids = {sid.strip() for sid in raw_ids.split(',') if sid.strip()}

        if not sensor_ids:
            return web.json_response(
                {"status": "error", "message": "sensorIds query param is required"},
                status=400
            )

        triggered = []
        for name, ble in self.ble_managers.items():
            if ble.read_uuid in sensor_ids or ble.sensor.get('write_uuid') in sensor_ids:
                ble.reconnect_event.set()
                triggered.append(name)
                logger.info(f"[Web -> Pi] /api/retry-sensor: reconnect_event set for '{name}'")

        if not triggered:
            logger.warning(f"[Web -> Pi] /api/retry-sensor: no matching sensor for ids={sensor_ids}")
            return web.json_response(
                {"status": "error", "message": f"No sensor matched sensorIds={raw_ids}"},
                status=404
            )

        return web.json_response({"status": "accepted", "triggered": triggered}, status=202)

    async def handle_tip(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
        except Exception:
            return web.json_response({"status": "error", "message": "Invalid JSON"}, status=400)

        violation_type   = str(data.get('violationType', '')).upper()
        violated_sensor  = str(data.get('violatedSensor', ''))
        violation_status = str(data.get('violationStatus', '')).upper()
        message          = data.get('message', '')

        if not violation_type or not violated_sensor or not violation_status or not message:
            return web.json_response(
                {"status": "error", "message": "violationType, violatedSensor, violationStatus and message are required"},
                status=400
            )

        logger.info(
            f"[Web -> Pi] /api/tip received: type={violation_type}, sensor={violated_sensor}, "
            f"status={violation_status}, message={message!r}"
        )

        ble_msg = await self._build_ble_warn_message(violated_sensor, violation_type, violation_status, message)

        forwarded = []
        for name, ble in self.ble_managers.items():
            if name == violated_sensor:
                await ble.ble_inbox.put(ble_msg)
                forwarded.append(name)
                logger.info(f"[Web -> Pi] Tip forwarded to Arduino '{name}': {ble_msg}")

        if not forwarded:
            logger.warning(
                f"[Web -> Pi] /api/tip: no connected BLE manager matched sensor='{violated_sensor}'. "
                "Tip was not forwarded."
            )
            return web.json_response(
                {"status": "warning", "message": f"No active BLE manager for sensor '{violated_sensor}'"},
                status=404
            )

        return web.json_response({"status": "accepted", "forwarded_to": forwarded}, status=202)

    async def _build_ble_warn_message( self, sensor_name: str, violation_type: str, violation_status: str, tip: str,) -> str:
        type_to_limit_keys = {
            "TEMPERATURE": ["max_temp", "min_temp"],
            "HUMIDITY":    ["max_moisture", "min_moisture"],
            "CO2":         ["max_co2"],
        }
        limit_keys = type_to_limit_keys.get(violation_type, [])

        threshold = None
        warntext  = violation_type.lower()

        active = await self.db.get_active_violations(sensor_name)
        for v in active:
            if v['type'] in limit_keys:
                threshold = v['threshold_value']
                direction = "above" if v['type'].startswith("max_") else "below"
                warntext  = f"{violation_type.lower()} {direction} limit"
                break

        threshold_str = str(threshold) if threshold is not None else "N/A"
        return f"WARNTEXT:{warntext}THRESHOLD:{threshold_str}TIP:{tip}"

# Background tasks

    async def _task_limit_change(self, payload: dict):
        try:
            await ConfigManager.handle_limit_change(self.db, payload)
        except Exception as e:
            logger.error(f"[WebManager] _task_limit_change failed: {e}")
            await self.db.log_event("CONFIG", f"Failed to apply limit change: {e}", "ERROR")

    async def _task_occupancy_change(self, payload: dict):
        try:
            await ConfigManager.handle_occupancy_change(self.db, payload)
        except Exception as e:
            logger.error(f"[WebManager] _task_occupancy_change failed: {e}")
            await self.db.log_event("CONFIG", f"Failed to apply occupancy change: {e}", "ERROR")

    async def _task_sensor_change(self, update_type: str, sensor_ids: list[str]):
        try:
            if update_type == "SENSOR_ADD":
                await ConfigManager.handle_sensor_add(self.db, self.auth, sensor_ids)
            elif update_type == "SENSOR_DELETE":
                await ConfigManager.handle_sensor_delete(self.db, sensor_ids)
            elif update_type == "FLUSH":
                await ConfigManager.handle_sensor_flush(self.db)
        except Exception as e:
            logger.error(f"[WebManager] _task_sensor_change ({update_type}) failed: {e}")
            await self.db.log_event("CONFIG", f"Failed to handle sensor {update_type}: {e}", "ERROR")

    async def _task_config_change(self, pi_id: str):
        try:
            # Always refresh the auth token before a full config re-fetch
            await self.auth.refresh_if_needed()

            server_url = self.static_config['webapp']['server_url']
            await ConfigManager.fetch_and_seed(pi_id, server_url, self.db, self.auth)

            new_freq = await self.db.get_config('frequency')
            if new_freq:
                logger.info(f"[WebManager] Config re-seeded. Broadcasting FREQUENCY:{new_freq} to all Arduinos.")
                for ble in self.ble_managers.values():
                    await ble.ble_inbox.put(f"FREQUENCY:{new_freq}")

            # Unblock the boot gate (idempotent after first call)
            if not self.config_ready_event.is_set():
                self.config_ready_event.set()
                logger.info("[WebManager] config_ready_event set — boot gate unblocked.")

        except Exception as e:
            logger.error(f"[WebManager] _task_config_change failed: {e}")
            await self.db.log_event("CONFIG", f"Failed to handle config change: {e}", "ERROR")

# Local server

    async def run_local_server(self):
        app = web.Application()
        app.router.add_post('/api/limits', self.handle_limits)
        app.router.add_post('/api/occupancy', self.handle_occupancy)
        app.router.add_post('/api/sensors', self.handle_sensors)
        app.router.add_post('/api/config', self.handle_config)
        app.router.add_post('/api/retry-sensor', self.handle_retry_sensor)
        app.router.add_post('/api/tip', self.handle_tip)

        runner = web.AppRunner(app)
        await runner.setup()
        site = web.TCPSite(runner, '0.0.0.0', self.local_port)
        await site.start()

        logger.info(
            f"[WebManager] Local API listening on port {self.local_port} "
            f"(routes: /api/limits, /api/occupancy, /api/sensors, /api/config, /api/retry-sensor, /api/tip)"
        )
        while True:
            await asyncio.sleep(3600)

# Pi -> Webapp

    async def _post_with_auth(self, session, url: str, payload: dict) -> bool:
        async with session.post(url, json=payload, headers=self.auth.get_headers()) as response:
            if response.status == 401:
                await self.auth.refresh_if_needed()
                async with session.post(url, json=payload, headers=self.auth.get_headers()) as retry:
                    return retry.status in (200, 201)
            return response.status in (200, 201)

    async def run_outgoing_data_worker(self):
        logger.info("[WebManager] Outgoing data worker started.")
        timeout = aiohttp.ClientTimeout(total=10)

        failure_streak = 0
        OFFLINE_THRESHOLD = 3
        is_webapp_offline = False

        async def _broadcast_to_arduinos(command: str):
            for ble in self.ble_managers.values():
                await ble.ble_inbox.put(command)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                payload = await self.web_out_queue.get()

                try:
                    api_url = f"{self.server_url}/api/measurements"
                    success = await self._post_with_auth(session, api_url, payload)

                    if success:
                        failure_streak = 0
                        if is_webapp_offline:
                            is_webapp_offline = False
                            logger.info("[WebManager] Webapp back online — notifying Arduinos.")
                            await _broadcast_to_arduinos("ERROR:CLEAR")
                    else:
                        failure_streak += 1
                        logger.warning(
                            f"[WebManager] Webapp rejected measurement payload "
                            f"(streak {failure_streak}/{OFFLINE_THRESHOLD}). Requeueing in 5s..."
                        )
                        await asyncio.sleep(5)
                        await self.web_out_queue.put(payload)

                except Exception as e:
                    failure_streak += 1
                    logger.error(f"[WebManager] Network error (streak {failure_streak}/{OFFLINE_THRESHOLD}): {e}")
                    await self.db.log_event("NETWORK", f"Failed to reach Webapp: {e}", "ERROR")
                    await asyncio.sleep(10)
                    await self.web_out_queue.put(payload)

                finally:
                    if failure_streak >= OFFLINE_THRESHOLD and not is_webapp_offline:
                        is_webapp_offline = True
                        logger.warning("[WebManager] Webapp confirmed offline — notifying Arduinos.")
                        await _broadcast_to_arduinos("ERROR:WEBAPP_OFFLINE")

                    self.web_out_queue.task_done()

    async def run_outgoing_violation_worker(self):
        logger.info("[WebManager] Outgoing violation report worker started.")
        timeout = aiohttp.ClientTimeout(total=10)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                event = await self.web_violation_queue.get()

                try:
                    event_type = event.get("type")

                    if event_type == "violation_warning":
                        await self._handle_violation_warning(session, event)

                    elif event_type == "violation_resolve":
                        await self._handle_violation_resolve(session, event)

                    else:
                        logger.warning(f"[WebManager] Unknown violation event type: {event_type!r}")

                except Exception as e:
                    logger.error(f"[WebManager] Violation worker unexpected error: {e}")
                    await self.db.log_event("NETWORK", f"Violation worker error: {e}", "ERROR")
                    await asyncio.sleep(10)
                    await self.web_violation_queue.put(event)

                finally:
                    self.web_violation_queue.task_done()

    async def _handle_violation_warning(self, session, event: dict):
        api_url = f"{self.server_url}/api/warnings"
        sensor_name = event["sensor_name"]
        limit_key   = event["limit_key"]

        payload = {
            "roomId":           event["roomId"],
            "device":           event["device"],
            "measurementType":  event["measurement_type"],
            "status":           event["status"],
            "triggeredValue":   event["triggeredValue"],
            "activeLimitAtTime": event["activeLimitAtTime"],
            "message":          event["message"],
        }

        try:
            async with session.post(api_url, json=payload, headers=self.auth.get_headers()) as response:
                if response.status == 401:
                    await self.auth.refresh_if_needed()
                    async with session.post(api_url, json=payload, headers=self.auth.get_headers()) as retry:
                        retry.raise_for_status()
                        data = await retry.json()
                else:
                    response.raise_for_status()
                    data = await response.json()

            warning_id = str(data.get("id", ""))
            if warning_id:
                await self.db.save_warning_id(sensor_name, limit_key, warning_id)
                logger.info(
                    f"[WebManager] Warning posted for {sensor_name}/{limit_key}, "
                    f"status={payload['status']}, id={warning_id}"
                )
            else:
                logger.warning(f"[WebManager] /api/warnings response had no 'id' field: {data}")

        except Exception as e:
            logger.warning(
                f"[WebManager] Failed to post warning for {sensor_name}/{limit_key}: {e}. Requeueing in 5s..."
            )
            await asyncio.sleep(5)
            await self.web_violation_queue.put(event)

    async def _handle_violation_resolve(self, session, event: dict):
        sensor_name = event["sensor_name"]
        limit_key   = event["limit_key"]

        warning_id = await self.db.get_warning_id(sensor_name, limit_key)
        if not warning_id:
            logger.warning(
                f"[WebManager] Cannot resolve {sensor_name}/{limit_key}: no warning_id in DB. Skipping."
            )
            return

        api_url = f"{self.server_url}/api/warnings/{warning_id}"
        payload = {
            "roomId":           event["roomId"],
            "device":           event["device"],
            "measurementType":  event["measurement_type"],
            "status":           "GREEN",
            "triggeredValue":   event["triggeredValue"],
            "activeLimitAtTime": event["activeLimitAtTime"],
            "message":          event["message"],
        }

        try:
            async with session.patch(api_url, json=payload, headers=self.auth.get_headers()) as response:
                if response.status == 401:
                    await self.auth.refresh_if_needed()
                    async with session.patch(api_url, json=payload, headers=self.auth.get_headers()) as retry:
                        retry.raise_for_status()
                else:
                    response.raise_for_status()

            logger.info(f"[WebManager] Warning {warning_id} resolved (GREEN) for {sensor_name}/{limit_key}")

            # Notify the Arduino that the condition is back within limits.
            threshold = event["activeLimitAtTime"]
            ble_msg = f"RESOLVED:{threshold}"
            ble = self.ble_managers.get(sensor_name)
            if ble:
                await ble.ble_inbox.put(ble_msg)
                logger.info(f"[WebManager] Sent to Arduino '{sensor_name}': {ble_msg}")
            else:
                logger.warning(f"[WebManager] No active BLE manager for '{sensor_name}' — RESOLVED message not sent.")

        except Exception as e:
            logger.warning(
                f"[WebManager] Failed to resolve warning {warning_id}: {e}. Requeueing in 5s..."
            )
            await asyncio.sleep(5)
            await self.web_violation_queue.put(event)

    async def run_outgoing_status_worker(self):
        logger.info("[WebManager] Outgoing status worker started.")
        timeout = aiohttp.ClientTimeout(total=10)
        from datetime import datetime
        from zoneinfo import ZoneInfo

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                event = await self.status_queue.get()

                try:
                    read_uuid = event["read_uuid"]
                    status = event["status"]
                    sensor_name = event["sensor_name"]
                    timestamp = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

                    api_url = f"{self.server_url}/api/sensor-stations/{read_uuid}"
                    payload = {"status": status, "timestamp": timestamp}

                    async with session.patch(api_url, json=payload, headers=self.auth.get_headers()) as response:
                        if response.status == 401:
                            await self.auth.refresh_if_needed()
                            async with session.patch(api_url, json=payload, headers=self.auth.get_headers()) as retry:
                                retry.raise_for_status()
                        elif response.status not in (200, 201, 202, 204):
                            logger.warning(
                                f"[WebManager] PATCH sensor-stations/{read_uuid} returned {response.status}"
                            )
                        else:
                            logger.info(f"[WebManager] Sensor '{sensor_name}' status={status} sent to webapp.")

                except Exception as e:
                    logger.warning(
                        f"[WebManager] Failed to send status for {event.get('sensor_name')}: {e}. Requeueing in 5s..."
                    )
                    await asyncio.sleep(5)
                    await self.status_queue.put(event)

                finally:
                    self.status_queue.task_done()
