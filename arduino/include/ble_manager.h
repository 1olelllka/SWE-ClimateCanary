#pragma once

#include <Arduino.h>
#include <ArduinoBLE.h>
#include "config.h"

class DisplayManager;

class BLEManager {
public:
  bool begin(DisplayManager* display);
  void poll();
  bool isConnected() const;
  void sendMessage(const String& msg);

private:
  static void onRxWritten(BLEDevice central, BLECharacteristic characteristic);

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