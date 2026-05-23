#pragma once

#include <Arduino.h>
#include <ArduinoBLE.h>
#include "config.h"
#include "sensor_manager.h"
#include "display_manager.h"
#include "reading_buffer.h"

class DisplayManager;
class BLEMessageHandler;

class BLEManager {
public:
  bool begin(DisplayManager* display);
  void poll();
  bool isConnected() const;
  void sendReading(const SensorReading& reading);
  String serializeBufferedReading(const BufferedReading& buffered) const;
  void flushBufferedReadings();

  BLEDevice currentCentral;
  unsigned int piConnectionCount = 0; 
  String piAddress = "";

  unsigned long measureAndSendIntervalMs = MEASURE_AND_SEND_INTERVAL_MS;
  

private:
  friend class BLEMessageHandler;

  static void onRxWritten(BLEDevice central, BLECharacteristic characteristic);

  String serializeReading(const SensorReading& reading) const;

  BLEService service{BLE_SERVICE_UUID};
  BLEStringCharacteristic txCharacteristic{
    BLE_TX_UUID,
    BLERead | BLENotify,
    192
  };

  BLEStringCharacteristic rxCharacteristic{
    BLE_RX_UUID,
    BLEWriteWithoutResponse,
    192
  };

  static BLEManager* instance;
  DisplayManager* displayManager = nullptr;

  String receivedTimestamp = "";
  unsigned long timeSyncMillis = 0;

  bool piConnectionEstablished = false;
};