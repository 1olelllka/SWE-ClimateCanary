import asyncio
import logging
import json
from datetime import datetime

logger = logging.getLogger(__name__)

class DataProcessor:
    def __init__(self, db, config, processing_queue, web_out_queue, ble_inbox):
        self.db = db
        self.config = config
        self.processing_queue = processing_queue
        self.web_out_queue = web_out_queue
        self.ble_inbox = ble_inbox

    async def run(self):
        logger.info("[Processor] Data processing worker started. Monitoring for occupancy and all sensor limits...")
        
        while True:
            raw_data = await self.processing_queue.get()
            
            try:
                if isinstance(raw_data, bytes):
                    raw_data = raw_data.decode('utf-8')
                data = json.loads(raw_data)
                
                limits = await self.db.get_all_limits()
                
                current_occ = limits.get('current_occupancy', 0)
                max_occ = limits.get('max_occupancy')
                
                if max_occ is not None and current_occ > max_occ:
                    logger.warning(f"[Processor] Occupancy limit exceeded ({current_occ}/{max_occ}). Data discarded.")
                    continue

                timestamp = datetime.now().isoformat()
                device_name = self.config['ble']['target_name']
                
                data['timestamp'] = timestamp
                data['device'] = device_name
                
                sensor_limit_map = {
                    "temperature": "max_temp",
                    "moisture": "max_moisture",
                    "co2": "max_co2"
                }
                
                any_violation = False
                for sensor_key, limit_key in sensor_limit_map.items():
                    val = data.get(sensor_key)
                    limit = limits.get(limit_key)
                    
                    if val is not None and limit is not None and val > limit:
                        any_violation = True
                        
                        await self.db.register_violation(sensor_key, limit, val)
                        
                        violation_report = {
                            "type": "violation_warning",
                            "device": device_name,
                            "timestamp": timestamp,
                            "limit_reached": sensor_key,
                            "violation_delta": round(val - limit, 2),
                            "actual_value": val,
                            "threshold": limit
                        }
                        await self.web_out_queue.put(violation_report)
                        
                        await self.ble_inbox.put(f"ALERT:{sensor_key.upper()}")
                        
                        logger.warning(f"[Processor] {sensor_key.upper()} violation detected: {val} > {limit}")
                
                if not any_violation:
                    active_violations = await self.db.get_active_violations()
                    if active_violations:
                        for v in active_violations:
                            await self.db.resolve_violation(v['type'])
                        await self.ble_inbox.put("ALERT:OFF")
                        logger.info("[Processor] All violations resolved. Arduino notified.")

                await self.db.insert_measurement(
                    temp=data.get('temperature'),
                    moisture=data.get('moisture'),
                    co2=data.get('co2'),
                    timestamp=timestamp
                )

                webapp_payload = {
                    "roomId": self.config.get('room_id', '3aae450a-3206-4ca7-8f4a-1981c388b1f5'),
                    "timestamp": timestamp,
                    "readings": []
                }
                
                if data.get('temperature') is not None:
                    webapp_payload["readings"].append({
                        "type": "TEMPERATURE",
                        "value": data['temperature']
                    })
                    
                if data.get('moisture') is not None:
                    webapp_payload["readings"].append({
                        "type": "HUMIDITY",
                        "value": data['moisture']
                    })
                    
                if data.get('co2') is not None:
                    webapp_payload["readings"].append({
                        "type": "CO2",
                        "value": data['co2']
                    })

                await self.web_out_queue.put(webapp_payload)
                
            except json.JSONDecodeError:
                logger.error(f"[Processor] Received malformed JSON from Arduino: {raw_data}")
            except Exception as e:
                logger.error(f"[Processor] Unexpected processing error: {e}", exc_info=True)
                
            finally:
                self.processing_queue.task_done()
