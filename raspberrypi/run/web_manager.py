import asyncio
import logging
import aiohttp
import json
import os
import sys
from aiohttp import web
from config_manager import ConfigManager

logger = logging.getLogger(__name__)

class WebManager:
    def __init__(self, config, db, web_out_queue, violation_out_queue):
        self.config = config
        self.db = db
        self.web_out_queue = web_out_queue
        self.violation_out_queue = violation_out_queue
        
        self.api_url = self.config['webapp'].get('api_url')
        self.alerts_url = self.config['webapp'].get('alerts_url')
        self.local_port = self.config['webapp'].get('local_listen_port', 8080)
...
    async def run_violation_worker(self):
        """Pushes violation alerts from the queue to the Webapp."""
        if not self.alerts_url:
            logger.warning("[WebManager] No alerts_url configured. Violation alerts will be dropped.")
            while True:
                await self.violation_out_queue.get()
                self.violation_out_queue.task_done()

        logger.info(f"[WebManager] Violation alert worker started (Target: {self.alerts_url})")
        headers = {"Content-Type": "application/json"}
        timeout = aiohttp.ClientTimeout(total=10)
        
        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                payload = await self.violation_out_queue.get()
                
                try:
                    logger.info(f"[WebManager] Sending violation alert to {self.alerts_url}...")
                    async with session.post(self.alerts_url, json=payload, headers=headers) as response:
                        if response.status in (200, 201):
                            logger.info(f"[WebManager] Violation alert accepted (HTTP {response.status})")
                        else:
                            logger.error(f"[WebManager] Webapp rejected alert (HTTP {response.status}).")
                            await asyncio.sleep(5)
                            await self.violation_out_queue.put(payload)
                
                except Exception as e:
                    logger.error(f"[WebManager] Network error sending alert: {e}")
                    await asyncio.sleep(10)
                    await self.violation_out_queue.put(payload) 
                    
                finally:
                    self.violation_out_queue.task_done()

    async def handle_config_update(self, request):
        """Webapp sends new full configuration here.
           If received, we save it and EXIT the process so Docker restarts us.
        """
        try:
            new_config_data = await request.json()
            logger.info("[Web -> Pi] Received remote config update request.")
            
            for section, values in new_config_data.items():
                if section in self.config and isinstance(values, dict):
                    self.config[section].update(values)
                else:
                    self.config[section] = values
            
            ConfigManager.save(self.config)
            
            logger.warning("[WebManager] Config updated. Restarting gateway in 3 seconds...")
            
            async def shutdown_soon():
                await asyncio.sleep(3)
                logger.info("[WebManager] Exiting for restart (Exit Code 42)")
                os._exit(42) # hard exit to ensure all threads stop immediately

            asyncio.create_task(shutdown_soon())
            
            return web.json_response({"status": "success", "message": "Config updated. Restarting..."})
            
        except Exception as e:
            logger.error(f"[WebManager] Error updating config: {e}")
            return web.json_response({"status": "error", "message": str(e)}, status=500)

    async def handle_limit_update(self, request):
        """Webapp sends new limits here (e.g., {"key": "max_temp", "value": 26.5})"""
        try:
            data = await request.json()
            key = data.get("key")
            value = data.get("value")
            
            if key and value is not None:
                # Instantly save to database so the DataProcessor uses it on the next reading
                await self.db.set_limit(key, float(value))
                logger.info(f"[Web -> Pi] Dynamically updated limit: {key} = {value}")
                return web.json_response({"status": "success", "message": f"Limit {key} updated"})
            
            return web.json_response({"status": "error", "message": "Missing 'key' or 'value'"}, status=400)
        except Exception as e:
            logger.error(f"[WebManager] Error updating limit: {e}")
            return web.json_response({"status": "error", "message": str(e)}, status=500)

    async def handle_occupancy_update(self, request):
        """Webapp frequently updates current or max occupancy here.
           Expects JSON like: {"current": 12, "max": 20} or just one of them.
        """
        try:
            data = await request.json()
            current_occ = data.get("current")
            max_occ = data.get("max")
            
            updated = []
            
            if current_occ is not None:
                await self.db.set_limit("current_occupancy", float(current_occ))
                updated.append(f"current={current_occ}")
                
            if max_occ is not None:
                await self.db.set_limit("max_occupancy", float(max_occ))
                updated.append(f"max={max_occ}")
                
            if updated:
                logger.info(f"[Web -> Pi] Occupancy updated: {', '.join(updated)}")
                return web.json_response({"status": "success", "message": f"Updated: {', '.join(updated)}"})
                
            return web.json_response({"status": "error", "message": "Missing 'current' or 'max'"}, status=400)
            
        except Exception as e:
            logger.error(f"[WebManager] Error updating occupancy: {e}")
            return web.json_response({"status": "error", "message": str(e)}, status=500)

    async def run_local_server(self):
        """Runs the background API listening for the Webapp."""
        app = web.Application()
        app.router.add_post('/api/limits', self.handle_limit_update)
        app.router.add_post('/api/occupancy', self.handle_occupancy_update)
        app.router.add_post('/api/config/update', self.handle_config_update)
        
        runner = web.AppRunner(app)
        await runner.setup()
        site = web.TCPSite(runner, '0.0.0.0', self.local_port)
        await site.start()
        
        logger.info(f"[WebManager] Local API listening on port {self.local_port}")
        while True:
            await asyncio.sleep(3600) # Keep server running safely

    async def run_outgoing_worker(self):
        """Pushes real-time sensor data from the queue to the Webapp."""
        logger.info("[WebManager] Outgoing worker started.")
        headers = {"Content-Type": "application/json"}
        timeout = aiohttp.ClientTimeout(total=10)
        
        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                payload = await self.web_out_queue.get()
                
                try:
                    logger.info(f"[WebManager] Sending sensor payload to Webapp...")
                    logger.debug(f"[WebManager] Payload details:\n{json.dumps(payload, indent=2)}")

                    async with session.post(self.api_url, json=payload, headers=headers) as response:
                        if response.status in (200, 201):
                            logger.info(f"[WebManager] Payload accepted by Webapp (HTTP {response.status})")
                        else:
                            logger.warning(f"[WebManager] Webapp rejected data (HTTP {response.status}).")
                            
                            await asyncio.sleep(5)
                            await self.web_out_queue.put(payload)
                
                except Exception as e:
                    logger.error(f"[WebManager] Network error reaching Webapp: {e}")
                    await self.db.log_event("NETWORK", f"Failed to reach Webapp: {e}", "ERROR")
                    await asyncio.sleep(10)
                    await self.web_out_queue.put(payload) 
                    
                finally:
                    self.web_out_queue.task_done()

    async def run_offline_sync_worker(self):
        """Periodically pushes unsynced system errors to the Webapp."""
        logger.info("[WebManager] Offline sync worker started.")
        headers = {"Content-Type": "application/json"}
        timeout = aiohttp.ClientTimeout(total=10)
        log_api_url = self.api_url.replace("/sensor-data", "/logs") 
        
        async with aiohttp.ClientSession(timeout=timeout) as session:
            while True:
                await asyncio.sleep(30)
                try:
                    unsynced_logs = await self.db.get_unsynced_logs()
                    for log_entry in unsynced_logs:
                        payload = dict(log_entry)
                        payload["device"] = self.config['ble']['target_name']
                        
                        async with session.post(log_api_url, json=payload, headers=headers) as response:
                            if response.status in (200, 201):
                                await self.db.mark_log_synced(log_entry["id"])
                                logger.info(f"[WebManager] Successfully synced offline log ID {log_entry['id']}")
                except Exception as e:
                    logger.info(f"[WebManager] Offline sync cycle failed, will retry: {e}")
                    pass
