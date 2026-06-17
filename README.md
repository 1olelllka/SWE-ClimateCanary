# ClimateCanary – G1T4

## Usage & Configuration

### Webapp

In order to run Webapp use ```docker compose up``` command with environmental variables
```bash
export TEAM_NAME=<TEAM_NAME> # Optional team name, defaults to local
export APP_JWT_SECRET=<VALID_512_BITS_JWT_SECRET_KEY>
export LOG_DIR=<ABSOLUTE_DOCKER_PATH> # Optional logs path, otherwise defaults to /app/logs
docker compose up --build -d # Build the project and run it in background
docker compose logs -f # Show the following logs from containers
docker compose down # Stop and remove containers
```

Example:
```bash
export TEAM_NAME=G1T4
export APP_JWT_SECRET=HEm3FCUc3APqk3tySyuKfZfHDrqlBfHu55bEiF1EhHzARzEMvwfqIsgmrxoULlGKp67wfHanmssIDkPBIJ5U5o
export LOG_DIR=/tmp/logs
docker compose up --build -d
docker compose logs -f
```

### Raspberry Pi

#### For Raspberry Pi configuration refer to this [readme](raspberrypi/README.md).

### Arduino

*Note*: For Arduino you need PlatformIO

PlatformIO configuration file (platformio.ini):
```ini
[env:nano33ble]
platform = nordicnrf52
board = nano33ble
framework = arduino
monitor_speed = 9600

lib_deps =
    adafruit/Adafruit BME680 Library
    adafruit/Adafruit Unified Sensor
    arduino-libraries/ArduinoBLE
    seeed-studio/Grove - LCD RGB Backlight
```

Installation steps on linux (python preinstalled needed):

1. ```python3 -m pip install --user platformio```
2. ```echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc```
3. ```source ~/.bashrc```
4. ```sudo usermod -aG dialout $USER```
5. ```pio run```
6. ```pio run --target upload```

## Authors

Diana Postupaieva | Jacob Solomon | Josua Rebay | Matthias Tiefenthaler | Oleh Sichko 