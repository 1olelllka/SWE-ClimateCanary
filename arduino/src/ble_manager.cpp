#include "ble_manager.h"
#include "display_manager.h"
#include "ble_message_handler.h"

#define JSON_BUFFER_SIZE 128
#define ADVERTISING_INTERVAL 32
#define TIMESTAMP_MAX_LENGTH 19

BLEManager* BLEManager::instance = nullptr;

bool BLEManager::begin(DisplayManager* display) {
  instance = this;
  displayManager = display;

  if (!BLE.begin()) {
    return false;
  }

  BLE.setAdvertisingInterval(ADVERTISING_INTERVAL);
  BLE.setLocalName(DEVICE_NAME);
  BLE.setDeviceName(DEVICE_NAME);
  BLE.setAdvertisedService(service);

  service.addCharacteristic(txCharacteristic);
  service.addCharacteristic(rxCharacteristic);
  BLE.addService(service);

  txCharacteristic.writeValue("Hello from Arduino");
  rxCharacteristic.setEventHandler(BLEWritten, onRxWritten);

  BLE.advertise();
  Serial.println("BLE advertising started");

  return true;
}

void BLEManager::poll() {
  BLE.poll();

  BLEDevice central = BLE.central();

  if (central && !currentCentral) {
    currentCentral = central;

    Serial.print("Connected to: ");
    Serial.println(currentCentral.address());

    timeReceived = false;

    txCharacteristic.writeValue("TIME_REQUEST");
    Serial.println("Requested time from Pi");
  } 

  if (currentCentral && !currentCentral.connected()) {
    Serial.print("Disconnected from: ");
    Serial.println(currentCentral.address());

    currentCentral = BLEDevice();
    timeReceived = false;

    BLE.advertise();
    Serial.println("BLE advertising restarted");
  }
}

bool BLEManager::isConnected() const {
  return currentCentral && currentCentral.connected();
}

String BLEManager::serializeReading(const SensorReading& r) const {
  if (!r.valid) {
    return "{\"valid\":false}";
  }

  String json;
  json.reserve(JSON_BUFFER_SIZE);

  json += "{";

  json += "\"timestamp\":\"";
  json += receivedTimestamp;
  json += "\"";

  json += ",\"millis_offset\":";
  json += String(millis() - timeSyncMillis);

  json += ",\"temperature\":";
  json += String(r.temperatureC, 2);

  json += ",\"moisture\":";
  json += String(r.humidityPct, 2);

  json += ",\"iaq\":";
  json += String(r.airQualityIndex, 2);

  json += "}";

  return json;
}

void BLEManager::sendReading(const SensorReading& reading) {
  if (!isConnected()) {
    return;
  }

  String payload = serializeReading(reading);
  txCharacteristic.writeValue(payload);

  Serial.print("Sent to Pi: ");
  Serial.println(payload);
}

void BLEManager::onRxWritten(BLEDevice central, BLECharacteristic characteristic) {
  (void) characteristic;

  if (instance == nullptr) {
    return;
  }

  String received = instance->rxCharacteristic.value();

  BLEMessageHandler::handleRxMessage(
    instance,
    central,
    received
  );
}
