#pragma once

#include <Arduino.h>
#include <ArduinoBLE.h>

class BLEManager;

class BLEMessageHandler {
public:
  static void handleRxMessage(
    BLEManager* manager,
    BLEDevice central,
    const String& received
  );
};