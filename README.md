# ClimateCanary – G1T4

## Usage & Configuration

### Webapp

In order to run Webapp use ```docker compose up``` command with environmental variables:
```bash
export TEAM_NAME=<TEAM_NAME> # Optional team name, defaults to local
export APP_JWT_SECRET=1a7783f6abab21b127e728d54043df9e6dbd9b1ae21ea1a743b6f04db60f3a02f16cae655b0de03bfc7b415572af35c865a083f061b4e37d333b9316516c4f557274c5988483056494ca9940de53b24b8547b1b9d06f9f4bc4515fcc9b9cdd241e027dd575744b4e9b028f4fb3b2a90956afc0685d6c5a12d47aceb9b39f451714e08dc6e0722dffc3481ac5a1a765ad12a37eecbcc218d03a6f742ac6de3c879bea46b8233fc5cee438371664cb1b323c2d52dae01b0673878507cf8b9d54f7f53519ca05e5a9e17cc831118348eca0d26de394a648d7f4ce78f2e3788b22add2bc37c401061cdacc18d5aa5a7d379b57085856d85a01127061ba9205504f5265ce6d806354daac802d0252d021e45676318ca8b89039bef8a75f68765958159d9a3c4ecaca061d7950081e9ad171fac2bcaed214f7ab19e323e4b7d47d41bd7bff38cd80949ab6b078cef870372252851e14d7f31fe137abd946ff7676fdc6ff9003054f65b4fdc8ea1e126294197461ace716da9fc1a8bd7a6edcb8d1d585280a59799798e13276d5797ce133ed3b8503d2cefca9b4022b95360b3cc2865fc02b8968741dcae7cd48ee6d16fe376d841215458fb7 # Or any other JWT token
36783182a06a3730c2f57f0f2e5e3b39bdfb86cbc2536e6e73522408ccf422686653f0efeee0662721de13069bcb37090d788c6393d7ce9dc8d9b463764232f7f1dc7f074f6ccb1a3232
export LOG_DIR=<ABSOLUTE_DOCKER_PATH> # Optional logs path, otherwise defaults to /app/logs
docker compose up --build
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