#include "ble_manager.h"
#include "display_manager.h"
#include "ble_message_handler.h"
#include "fault_manager.h"
#include "led_manager.h"
#include "reading_buffer.h"

#define JSON_BUFFER_SIZE 192
#define ADVERTISING_INTERVAL 32
#define TIMESTAMP_MAX_LENGTH 19

BLEManager* BLEManager::instance = nullptr;
extern FaultManager faultManager;
extern LedManager ledManager;
extern ReadingBuffer readingBuffer;

bool BLEManager::begin(DisplayManager* display) {
  instance = this;
  displayManager = display;

  if (!BLE.begin()) {
    return false;
  }

  BLE.setAdvertisingInterval(ADVERTISING_INTERVAL);
  BLE.setLocalName(STATION_COMPLETE_NAME);
  BLE.setDeviceName(STATION_COMPLETE_NAME);
  BLE.setAdvertisedService(service);

  service.addCharacteristic(txCharacteristic);
  service.addCharacteristic(rxCharacteristic);
  BLE.addService(service);

  rxCharacteristic.setEventHandler(BLEWritten, onRxWritten);

  return true;
}

void BLEManager::poll() {
  BLE.poll();

  BLEDevice central = BLE.central();

  if (central && !currentCentral) {
    currentCentral = central;

    piConnectionEstablished = true;
    piConnectionCount++;
    piAddress = currentCentral.address();
    BLE.stopAdvertise();

    Serial.println("Connected to: " + currentCentral.address() + " (Historical connections: " + String(piConnectionCount) + ")");
    
    faultManager.clear(FaultType::PiDisconnected);
    displayManager->updateFault(faultManager.activeText());
    ledManager.update(LedManager::LedMode::White);

    timeReceived = false;

    txCharacteristic.writeValue("TIME_REQUEST");
    Serial.println("Requested time from Pi");
  } 

  if (currentCentral && !currentCentral.connected()) {
    Serial.println("Disconnected from: " + currentCentral.address());

    faultManager.set(FaultType::PiDisconnected);
    displayManager->updateFault(faultManager.activeText());
    ledManager.update(LedManager::LedMode::Off);

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

  json += ",\"humidity\":";
  json += String(r.humidityPct, 2);

  json += ",\"iaq\":";
  json += String(r.airQualityIndex, 2);

  json += "}";

  return json;
}

String BLEManager::serializeBufferedReading(const BufferedReading& buffered) const {
  String json;
  json.reserve(JSON_BUFFER_SIZE);

  json += "{";

  json += "\"timestamp\":\"";
  json += buffered.timestamp;
  json += "\"";

  json += ",\"millis_offset\":";
  json += String(buffered.millisOffset);

  json += ",\"temperature\":";
  json += String(buffered.reading.temperatureC, 2);

  json += ",\"humidity\":";
  json += String(buffered.reading.humidityPct, 2);

  json += ",\"iaq\":";
  json += String(buffered.reading.airQualityIndex, 2);

  json += "}";

  return json;
}

void BLEManager::sendReading(const SensorReading& reading) {
  BLE.poll();

  if (!reading.valid) {
    return;
  }

  if (!timeReceived) {
    Serial.println("Cannot send reading: time sync not established");
    return;
  }

  if (!isConnected()) {
    const unsigned long millisOffset = millis() - timeSyncMillis;

    const bool stored = readingBuffer.push(
      reading,
      receivedTimestamp,
      millisOffset
    );

    if (stored) {
      Serial.print("Buffered reading. Buffer size: ");
      Serial.println(readingBuffer.size());
    } else {
      Serial.println("Failed to buffer reading");
    }

    return;
  }

  String payload = serializeReading(reading);
  bool write_success = txCharacteristic.writeValue(payload);

  if (write_success) {
    Serial.print("Sent to Pi: ");
    Serial.println(payload);
  } else {
    Serial.print("BLE write failed, buffering reading: ");
    Serial.println(payload);

    const unsigned long millisOffset = millis() - timeSyncMillis;

    readingBuffer.push(
      reading,
      receivedTimestamp,
      millisOffset
    );
  }
}

void BLEManager::flushBufferedReadings() {
  if (!isConnected()) {
    return;
  }

  if (readingBuffer.isEmpty()) {
    return;
  }

  Serial.print("Flushing buffered readings. Count: ");
  Serial.println(readingBuffer.size());

  BufferedReading buffered;

  while (!readingBuffer.isEmpty() && isConnected()) {
    if (!readingBuffer.pop(buffered)) {
      break;
    }

    String payload = serializeBufferedReading(buffered);
    txCharacteristic.writeValue(payload);

    Serial.print("Sent buffered reading: ");
    Serial.println(payload);

    delay(50);
  }

  Serial.println("Finished flushing buffered readings");
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
