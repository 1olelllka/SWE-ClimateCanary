# /// script
# requires-python = ">=3.9"
# dependencies = [
#     "requests>=2.31.0",
#     "pyyaml>=6.0"
# ]
# ///

import requests
import os
import sys
import logging
import time
from config_manager import ConfigManager

# Configure basic logging for bootstrap phase
logging.basicConfig(level=logging.INFO, format='%(asctime)s - [Bootstrap] - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def bootstrap():
    config = ConfigManager.load()
    
    room_id = config.get('system', {}).get('room_id')
    api_url = config.get('webapp', {}).get('api_url')

    if not api_url or not room_id:
        logger.error("Bootstrap failed: Missing API_URL or ROOM_ID in environment/config.")
        sys.exit(1)

    # Check if we already have BLE config. If so, we might skip bootstrap unless forced.
    if config.get('ble', {}).get('char_uuid'):
        logger.info("BLE configuration already exists. Skipping remote bootstrap.")
        return

    # Derive base URL from api_url if needed, or use a dedicated provision endpoint
    # Let's assume the endpoint is: {BASE_URL}/api/provision/{ROOM_ID}
    # Example: if api_url is http://1.2.3.4/api/measurements -> base is http://1.2.3.4
    base_url = api_url.rsplit('/api/', 1)[0]
    provision_url = f"{base_url}/api/provision/{room_id}"

    logger.info(f"Attempting to fetch remote config from {provision_url}...")

    max_retries = 10
    for attempt in range(max_retries):
        try:
            response = requests.get(provision_url, timeout=10)
            if response.status_code == 200:
                remote_data = response.json()
                
                # Expected JSON structure from Webapp:
                # {
                #   "ble": {
                #     "target_name": "SensorStation",
                #     "char_uuid": "...",
                #     "write_uuid": "..."
                #   },
                #   "system": { "room_id": "..." }
                # }
                
                if 'ble' in remote_data:
                    config['ble'].update(remote_data['ble'])
                if 'system' in remote_data:
                    config['system'].update(remote_data['system'])
                if 'webapp' in remote_data:
                    config['webapp'].update(remote_data['webapp'])
                
                ConfigManager.save(config)
                logger.info("Successfully provisioned from Webapp.")
                return
            else:
                logger.warning(f"Provisioning endpoint returned {response.status_code}. Retrying in 5s...")
        except Exception as e:
            logger.warning(f"Connection error during bootstrap: {e}. Retrying in 5s...")
        
        time.sleep(5)

    logger.error("Failed to bootstrap after multiple attempts.")
    sys.exit(1)

if __name__ == "__main__":
    bootstrap()
