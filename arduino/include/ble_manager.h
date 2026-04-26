#pragma once

#include <Arduino.h>
#include <ArduinoBLE.h>
#include "config.h"
#include "sensor_manager.h"

class DisplayManager;

class BLEManager {
public:
  bool begin(DisplayManager* display);
  void poll();
  bool isConnected() const;
  void sendReading(const SensorReading& reading);

private:
  static void onRxWritten(BLEDevice central, BLECharacteristic characteristic);
  String serializeReading(const SensorReading& reading) const;

  BLEService service{BLE_SERVICE_UUID};
  BLEStringCharacteristic txCharacteristic{
    BLE_TX_UUID,
    BLERead | BLENotify,
    64
  };
  
  BLEStringCharacteristic rxCharacteristic{
    BLE_RX_UUID,
    BLEWriteWithoutResponse,
    64
  };

  static BLEManager* instance;
  DisplayManager* displayManager = nullptr;
  BLEDevice currentCentral;
};