import asyncio
import logging
import aiohttp
from aiohttp import web
from config_manager import ConfigManager
from ble_manager import BLEManager

logger = logging.getLogger(__name__)

SENSOR_UPDATE_TYPES = {"SENSOR_DELETE", "SENSOR_ADD", "FLUSH"}


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
                logger.info(f"[WebManager] Config re-seeded. Broadcasting FREQ:{new_freq} to all Arduinos.")
                for ble in self.ble_managers.values():
                    await ble.ble_inbox.put(f"FREQ:{new_freq}")

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

        runner = web.AppRunner(app)
        await runner.setup()
        site = web.TCPSite(runner, '0.0.0.0', self.local_port)
        await site.start()

        logger.info(f"[WebManager] Local API listening on port {self.local_port} "
                    f"(routes: /api/limits, /api/occupancy, /api/sensors, /api/config)")
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
                    api_url = f"{self.server_url}/api/sensor-data"
                    success = await self._post_with_auth(session, api_url, payload)

                    if success:
                        failure_streak = 0
                        if is_webapp_offline:
                            is_webapp_offline = False
                            logger.info("[WebManager] Webapp back online — notifying Arduinos.")
                            await _broadcast_to_arduinos("WEBAPP:ONLINE")
                    else:
                        failure_streak += 1
                        logger.warning(
                            f"[WebManager] Webapp rejected payload "
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
                        await _broadcast_to_arduinos("WEBAPP:OFFLINE")

                    self.web_out_queue.task_done()

    async def run_outgoing_violation_worker(self):
        logger.info("[WebManager] Outgoing violation report worker started.")
        timeout = aiohttp.ClientTimeout(total=10)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                payload = await self.web_violation_queue.get()

                try:
                    api_url = f"{self.server_url}/api/violations"
                    success = await self._post_with_auth(session, api_url, payload)

                    if not success:
                        logger.warning("[WebManager] Webapp rejected violation payload. Requeueing in 5s...")
                        await asyncio.sleep(5)
                        await self.web_violation_queue.put(payload)

                except Exception as e:
                    logger.error(f"[WebManager] Network error: {e}")
                    await self.db.log_event("NETWORK", f"Failed to reach Webapp: {e}", "ERROR")
                    await asyncio.sleep(10)
                    await self.web_violation_queue.put(payload)

                finally:
                    self.web_violation_queue.task_done()

    async def run_offline_sync_worker(self):
        logger.info("[WebManager] Offline sync worker started.")
        timeout = aiohttp.ClientTimeout(total=10)
        log_api_url = f"{self.server_url}/api/logs/bulk"

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                await asyncio.sleep(3600)
                try:
                    unsynced_logs = await self.db.get_unsynced_logs()
                    if not unsynced_logs:
                        continue

                    payload = {
                        "device": await self.db.get_config('raspberry_id'),
                        "logs": unsynced_logs,
                    }

                    success = await self._post_with_auth(session, log_api_url, payload)

                    if success:
                        ids = [entry["id"] for entry in unsynced_logs]
                        await self.db.mark_logs_synced(ids)
                        logger.info(f"[WebManager] Synced {len(ids)} offline logs in bulk.")
                    else:
                        logger.warning("[WebManager] Bulk log sync rejected by webapp. Will retry next cycle.")

                except Exception as e:
                    logger.warning(f"[WebManager] Offline sync error: {e}")
                    # Best-effort, never crash the worker
