#include "ble_message_handler.h"
#include "ble_manager.h"
#include "led_manager.h"
#include "fault_manager.h"
#include "warning_manager.h"

extern LedManager ledManager;
extern FaultManager faultManager;
extern WarningManager warningManager;

static MeasurementType parseMeasurementType(String type) {
  type.trim();
  type.toUpperCase();

  if (type == "TEMPERATURE") {
    return MeasurementType::Temperature;
  }

  if (type == "HUMIDITY") {
    return MeasurementType::Humidity;
  }

  if (type == "CO2") {
    return MeasurementType::AirQuality;
  }

  return MeasurementType::Unknown;
}

static SeverityLevel parseSeverityLevel(String status) {
  status.trim();
  status.toUpperCase();

  if (status == "RED") {
    return SeverityLevel::Red;
  }

  if (status == "YELLOW") {
    return SeverityLevel::Yellow;
  }

  if (status == "GREEN") {
    return SeverityLevel::Green;
  }

  return SeverityLevel::None;
}

static void updateLedForActiveWarning() {
  switch (warningManager.activeWarning()) {
    case SeverityLevel::Red:
      ledManager.update(LedManager::LedMode::Red);
      break;

    case SeverityLevel::Yellow:
      ledManager.update(LedManager::LedMode::Blue);
      break;

    case SeverityLevel::Green:
      ledManager.update(LedManager::LedMode::Green);
      break;

    case SeverityLevel::None:
      ledManager.update(LedManager::LedMode::White);
      break;
  }
}

static void updateDisplayForActiveWarning(DisplayManager* displayManager) {
  WarningData data = warningManager.activeData();

  if (data.active) {
    displayManager->setWarningData(
      data.warnText,
      data.threshold,
      data.tip
    );
  } else {
    displayManager->clearWarningData();
  }
}

void BLEMessageHandler::handleRxMessage(
  BLEManager* manager,
  BLEDevice central,
  const String& received
) {
  if (manager == nullptr) {
    return;
  }

  Serial.print("Received from Pi (");
  Serial.print(central.address());
  Serial.print("): ");
  Serial.println(received);

  if (received.startsWith("TIME:")) {
    manager->receivedTimestamp = received.substring(5);
    manager->timeSyncMillis = millis();
    manager->timeReceived = true;

    Serial.print("Time Format: ");
    Serial.println(manager->receivedTimestamp);

    manager->flushBufferedReadings();
  }

  else if (received.startsWith("FREQUENCY:")) {
    String frequency = received.substring(10);
    frequency.trim();

    //s to ms conversion: webapp sends frequency in seconds but we need it in ms for the interval
    unsigned long newIntervalMs = frequency.toInt() * 1000UL;

    if (newIntervalMs > 0) {
      manager->measureAndSendIntervalMs = newIntervalMs;

      Serial.print("Updated send interval ms: ");
      Serial.println(manager->measureAndSendIntervalMs);
    }
  }

  else if (received.startsWith("WARNTEXT:")) {
  int thresholdIndex = received.indexOf("THRESHOLD:");
  int tipIndex = received.indexOf("TIP:");
  int statusIndex = received.indexOf("STATUS:");
  int typeIndex = received.indexOf("TYPE:");

    if (
      thresholdIndex != -1 &&
      tipIndex != -1 &&
      statusIndex != -1 &&
      typeIndex != -1 &&
      thresholdIndex > 9 &&
      tipIndex > thresholdIndex &&
      statusIndex > tipIndex &&
      typeIndex > statusIndex
    ) {
      String warnText = received.substring(9, thresholdIndex);
      String threshold = received.substring(thresholdIndex + 10, tipIndex);
      String tip = received.substring(tipIndex + 4, statusIndex);
      String violationStatus = received.substring(statusIndex + 7, typeIndex);
      String measurementTypeText = received.substring(typeIndex + 5);

      warnText.trim();
      threshold.trim();
      tip.trim();
      violationStatus.trim();
      measurementTypeText.trim();

      SeverityLevel severityLevel = parseSeverityLevel(violationStatus);
      MeasurementType measurementType = parseMeasurementType(measurementTypeText);

      if (severityLevel == SeverityLevel::None) {
        Serial.println("Unknown warning status");
        return;
      }

      if (measurementType == MeasurementType::Unknown) {
        Serial.println("Unknown measurement type");
        return;
      }

      warningManager.set(
        measurementType,
        severityLevel,
        warnText,
        threshold,
        tip
      );

      updateDisplayForActiveWarning(manager->displayManager);
      updateLedForActiveWarning();
    } else {
      Serial.println("Invalid WARNTEXT message format");
    }
  }

  else if (received.startsWith("RESOLVED:")) {
    String resolvedTypeText = received.substring(9);
    resolvedTypeText.trim();

    MeasurementType measurementType = parseMeasurementType(resolvedTypeText);

    if (measurementType == MeasurementType::Unknown) {
      Serial.println("Unknown resolved measurement type");
      return;
    }

    warningManager.clear(measurementType);

    updateDisplayForActiveWarning(manager->displayManager);
    updateLedForActiveWarning();

    Serial.println("Warning resolved");
  }

  else if (received == "ERROR:WEBAPP_OFFLINE") {
    Serial.println("Webapp offline");
    ledManager.update(LedManager::LedMode::Off);
    faultManager.set(FaultType::WebappOffline);
    manager->displayManager->updateFault(faultManager.activeText());
  }

  else if (received == "ERROR:WEBAPP_CLEAR") {
    ledManager.update(LedManager::LedMode::White);
    faultManager.clear(FaultType::WebappOffline);
    manager->displayManager->updateFault(faultManager.activeText());
  }

  else {
    Serial.println("Unknown BLE message format");
  }
}