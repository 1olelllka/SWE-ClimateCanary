import asyncio
import logging
import aiohttp
from aiohttp import web
from config_manager import ConfigManager

logger = logging.getLogger(__name__)

NOTIFY_HANDLERS = {
        "LIMIT_CHANGE": ConfigManager.handle_limit_change,
        "SENSOR_CHANGE": ConfigManager.handle_sensor_change,
        "OCCUPANCY_CHANGE": ConfigManager.handle_occupancy_change,
        "CONFIG_CHANGE": ConfigManager.handle_config_change,
        }

class WebManager:
    def __init__(self, static_config, db, web_out_queue, web_violation_queue, auth):
        self.static_config = static_config
        self.db = db
        self.web_out_queue = web_out_queue
        self.web_violation_queue = web_violation_queue
        self.auth = auth
        self.local_port = static_config['webapp']['local_listen_port']
        self.server_url = static_config['webapp']['server_url']

# Inbound endpoints (Webapp -> Pi)

    async def handle_notify(self, request):
        """Single entry point for all webapp-initiated updates.
        Webapp sends: {"type": "LIMIT_CHANGE"} (or SENSOR_CHANGE, etc.)
        Pi looks up the handler, fetches the updated data, and updates DB.
        """
        try:
            data = await request.json()
            notify_type = data.get("type", "").upper()
            handler = NOTIFY_HANDLERS.get(notify_type)

            if not handler:
                logger.warning(f"[Web -> Pi] Unknown notify type: '{notify_type}'")
                return web.json_response(
                        {"status": "error", "message": f"Unknown notify type: {notify_type}"},
                        status=400
                        )

            logger.info(f"[Web -> Pi] Notify received: {notify_type}")

            asyncio.create_task(self._handle_notify_task(notify_type, handler))

            return web.json_response({"status": "accepted", "type": notify_type})

        except Exception as e:
            logger.error(f"[WebManager] Error handling notify: {e}")
            return web.json_response({"status": "error", "message": str(e)}, status=500)

    async def _handle_notify_task(self, notify_type: str, handler):
        """Background task that calls the config handler and logs the result."""
        try:
            await handler(self.db, self.auth)
        except Exception as e:
            logger.error(f"[WebManager] Failed to handle notify '{notify_type}': {e}")
            await self.db.log_event("CONFIG", f"Failed to handle notify {notify_type}: {e}", "ERROR")

    async def run_local_server(self):
        """Runs the local REST API that the webapp calls."""
        app = web.Application()
        app.router.add_post('/notify', self.handle_notify)

        runner = web.AppRunner(app)
        await runner.setup()
        site = web.TCPSite(runner, '0.0.0.0', self.local_port)
        await site.start()

        logger.info(f"[WebManager] Local API listening on port {self.local_port}")
        while True:
            await asyncio.sleep(3600)

# Outbound workers (Pi -> Webapp)

    async def _post_with_auth(self, session, url: str, payload: dict) -> bool:
        """POST with automatic token refresh on 401. Returns True on success."""
        async with session.post(url, json=payload, headers=self.auth.get_headers()) as response:
            if response.status == 401:
                await self.auth.refresh_if_needed()
                async with session.post(url, json=payload, headers=self.auth.get_headers()) as retry:
                    return retry.status in (200, 201)
            return response.status in (200, 201)

    async def run_outgoing_data_worker(self):
        """Pushes sensor data from the queue to the Webapp."""
        logger.info("[WebManager] Outgoing data worker started.")
        timeout = aiohttp.ClientTimeout(total=10)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                payload = await self.web_out_queue.get()

                try:
                    api_url = f"{self.server_url}/api/sensor-data"

                    success = await self._post_with_auth(session, api_url, payload)

                    if not success:
                        logger.warning("[WebManager] Webapp rejected payload. Requeueing in 5s...")
                        await asyncio.sleep(5)
                        await self.web_out_queue.put(payload)

                except Exception as e:
                    logger.error(f"[WebManager] Network error: {e}")
                    await self.db.log_event("NETWORK", f"Failed to reach Webapp: {e}", "ERROR")
                    await asyncio.sleep(10)
                    await self.web_out_queue.put(payload)

                finally:
                    self.web_out_queue.task_done()

    async def run_outgoing_violation_worker(self):
        """Pushes violation reports from the queue to the webapp."""
        logger.info("[WebManager] Outgoing violation report worker started.")
        timeout = aiohttp.ClientTimeout(total=10)

        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                payload = await self.web_violation_queue.get()

                try:
                    api_url = f"{self.server_url}/api/violations"

                    success = await self._post_with_auth(session, api_url, payload)

                    if not success:
                        logger.warning("[WebManager] Webapp rejected payload. Requeueing in 5s...")
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
        """Periodically pushes unsynced system logs to the Webapp in bulk."""
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
                        "device": await self.db.get_config('ble.target_name'),
                        "logs":   unsynced_logs
                    }
    
                    success = await self._post_with_auth(session, log_api_url, payload)
    
                    if success:
                        ids = [entry["id"] for entry in unsynced_logs]
                        await self.db.mark_logs_synced(ids)
                        logger.info(f"[WebManager] Synced {len(ids)} offline logs in bulk.")
                    else:
                        logger.warning("[WebManager] Bulk log sync rejected by webapp. Will retry in 30s.")
    
                except Exception as e:
                    logger.warning(f"[WebManager] Offline sync error: {e}")
                    # Best-effort, never crash the worker
