# Raspberry Pi IoT Gateway

A robust, portable, and self-configuring IoT Gateway that bridges **Arduino (BLE)** sensors to a **Web Application (REST)**.

## Features

*   **Zero-Touch Provisioning:** Automatically fetches BLE UUIDs and Room IDs from the WebApp on first launch.
*   **Dynamic Configuration:** Remotely update the Gateway's settings via a REST API. The Gateway automatically saves and hot-restarts.
*   **Edge Processing:** Local SQLite database for offline data buffering and limit-violation logging.
*   **Dockerized with UV:** Lightweight, fast, and handles all system dependencies (Bluetooth, DBus) automatically.
*   **Resilient Design:** A managed startup loop handles crashes, network outages, and configuration-triggered restarts.

## Project Structure

```text
.
├── data
│   ├── config.yaml
│   └── production.sqlite
├── logs
│   └── raspberrypi.log
└── run
    ├── ble_manager.py
    ├── bootstrap.py
    ├── config_manager.py
    ├── data_processor.py
    ├── db_manager.py
    ├── docker-compose.yml
    ├── Dockerfile
    ├── entrypoint.sh
    ├── main.py
    ├── mock_data.py
    ├── requirements.txt
    ├── simulate_arduino.py
    └── web_manager.py
```

## Quick Start (Raspberry Pi)

### 1. Prerequisites
Ensure you have **Docker** and **Docker Compose** installed on your Raspberry Pi:
```bash
sudo apt-get update
sudo apt-get install docker.io docker-compose -y
```

### 2. Configure Local Identity
Edit the `.env` file in the root directory. This is the **only** file you need to change per device:
```bash
ROOM_ID=your-unique-room-uuid
API_URL=http://your-webapp-ip:8080/api/measurements
```

### 3. Launch the Gateway
Run the following command to build and start the container in the background:
```bash
docker compose up --build -d
```

## How it Works

1.  **Phase 1 (Bootstrap):** On startup, `bootstrap.py` checks if the BLE configuration exists. If not, it calls the `API_URL` to fetch the specific Arduino UUIDs for the assigned `ROOM_ID`.
2.  **Phase 2 (Main Loop):** `main.py` starts, connects to the Arduino, and begins streaming sensor data to the WebApp.
3.  **Phase 3 (Remote Update):** If the WebApp sends a new configuration to the Pi's local listener (`/api/config/update`), the Gateway saves the changes and exits with **Code 42**.
4.  **Phase 4 (Hot-Restart):** The `entrypoint.sh` script detects Code 42 and immediately restarts the application without restarting the entire Docker container.

## Monitoring
To view real-time logs:
```bash
docker logs -f iot-gateway
```
To access the local database (from the host):
```bash
sqlite3 data/production.sqlite
```
