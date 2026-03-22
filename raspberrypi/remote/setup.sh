#!/bin/bash

PI_IP="172.20.10.3"
PI_USER="pi"
PI_DIR="/home/pi/ble-raspberry"

# Arduino BLE Settings
BLE_NAME="SensorStation"
 # For Pi receiving data FROM Arduino
BLE_UUID="19B10001-E8F2-537E-4F6C-D104768A1214"      
 # For Pi sending commands TO Arduino
BLE_WRITE_UUID="19B10002-E8F2-537E-4F6C-D104768A1214"

# Webapp REST API Settings 
# Where the Pi pushes sensor data
API_URL="http://172.20.10.4:8080/test-info"   
# (Optional) If Webapp needs auth
API_TOKEN="my-secret-token"      
# The port the Pi listens on for commands
LOCAL_PORT="8080"                                    

echo "Connecting to $PI_USER@$PI_IP to launch the Two-Way BLE Gateway..."

ssh -t "${PI_USER}@${PI_IP}" "
    # 1. Inject Arduino variables
    export BLE_NAME='${BLE_NAME}'
    export BLE_UUID='${BLE_UUID}'
    export BLE_WRITE_UUID='${BLE_WRITE_UUID}'
    
    # 2. Inject Webapp variables
    export API_URL='${API_URL}'
    export API_TOKEN='${API_TOKEN}'
    export LOCAL_PORT='${LOCAL_PORT}'
    
    # 3. Startup Sequence
    source ${PI_DIR}/.venv/bin/activate
    
    echo 'Remote: Starting Python gateway...'
    python3 ${PI_DIR}/connect.py
"

echo "Gateway session ended cleanly."
