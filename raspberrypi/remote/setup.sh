#!/bin/bash

PI_IP="172.20.10.3"
PI_USER="pi"
PI_DIR="/home/pi/ble-raspberry"

BLE_NAME="SensorStation"
BLE_UUID="19B10001-E8F2-537E-4F6C-D104768A1214"      
BLE_WRITE_UUID="19B10002-E8F2-537E-4F6C-D104768A1214"

API_URL="http://172.20.10.4:8080/test-info"   
LOCAL_PORT="8080"                                    

echo "Connecting to $PI_USER@$PI_IP to launch the Two-Way BLE Gateway..."

ssh -t "${PI_USER}@${PI_IP}" "
    export BLE_NAME='${BLE_NAME}'
    export BLE_UUID='${BLE_UUID}'
    export BLE_WRITE_UUID='${BLE_WRITE_UUID}'
    
    export API_URL='${API_URL}'
    export LOCAL_PORT='${LOCAL_PORT}'
    
    source ${PI_DIR}/.venv/bin/activate
    
    echo 'Remote: Starting Python gateway...'
    python3 ${PI_DIR}/connect.py
"

echo "Gateway session ended cleanly."
