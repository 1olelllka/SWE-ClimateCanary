#include "ble_message_handler.h"
#include "ble_manager.h"
#include "led_manager.h"
#include "fault_manager.h"

extern LedManager ledManager;
extern FaultManager faultManager;

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

    if (
      thresholdIndex != -1 &&
      tipIndex != -1 &&
      statusIndex != -1 &&
      thresholdIndex > 9 &&
      tipIndex > thresholdIndex &&
      statusIndex > tipIndex
    ) {
      String warnText = received.substring(9, thresholdIndex);
      String threshold = received.substring(thresholdIndex + 10, tipIndex);
      String tip = received.substring(tipIndex + 4, statusIndex);
      String violationStatus = received.substring(statusIndex + 7);

      warnText.trim();
      threshold.trim();
      tip.trim();
      violationStatus.trim();
      violationStatus.toUpperCase();

      manager->displayManager->setWarningData(
        warnText,
        threshold,
        tip
      );

      if (violationStatus == "GREEN") {
        ledManager.update(LedManager::LedMode::Green);
      } else if (violationStatus == "YELLOW") {
        ledManager.update(LedManager::LedMode::Blue);
      } else if (violationStatus == "RED") {
        ledManager.update(LedManager::LedMode::Red);
      }
    } else {
      Serial.println("Invalid WARNTEXT message format");
    }
  }

  else if (received.startsWith("RESOLVED:")){
    ledManager.update(LedManager::LedMode::White);
    manager->displayManager->clearWarningData();
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