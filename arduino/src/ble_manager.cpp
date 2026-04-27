#include "ble_manager.h"
#include "display_manager.h"

BLEManager* BLEManager::instance = nullptr;

bool BLEManager::begin(DisplayManager* display) {
  instance = this;
  displayManager = display;

  if (!BLE.begin()) {
    return false;
  }

  BLE.setAdvertisingInterval(32);
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
    receivedUnixTime = 0;

    txCharacteristic.writeValue("TIME_REQUEST");
    Serial.println("Requested time from Pi");

    if (displayManager != nullptr) {
      displayManager->showConnected();
    }
  }

  if (currentCentral && !currentCentral.connected()) {
    Serial.print("Disconnected from: ");
    Serial.println(currentCentral.address());

    currentCentral = BLEDevice();
    timeReceived = false;
    receivedUnixTime = 0;

    BLE.advertise();
    Serial.println("BLE advertising restarted");

    if (displayManager != nullptr) {
      displayManager->showDisconnected();
    }
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
  json.reserve(64);

  json += "{";
  json += "\"temperature\":";
  json += String(r.temperatureC, 2);
  json += ",\"moisture\":";
  json += String(r.humidityPct, 2);
  json += ",\"co2\":";
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

  Serial.print("Received from Pi (");
  Serial.print(central.address());
  Serial.print("): ");
  Serial.println(received);

  if (received.startsWith("TIME:")) {
    String timeString = received.substring(5);

    instance->receivedUnixTime = timeString.toInt();
    instance->timeReceived = true;

    Serial.print("Time received from Pi: ");
    Serial.println(instance->receivedUnixTime);
  }
}

bool BLEManager::hasReceivedTime() const {
  return timeReceived;
}

unsigned long BLEManager::getReceivedTime() const {
  return receivedUnixTime;
}
