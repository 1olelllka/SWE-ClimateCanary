#include "ble_message_handler.h"
#include "ble_manager.h"
#include "led_manager.h"

extern LedManager ledManager;

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

      Serial.println("Parsed warning:");
      Serial.println(warnText);

      Serial.println("Parsed threshold:");
      Serial.println(threshold);

      Serial.println("Parsed tip:");
      Serial.println(tip);

      Serial.println("Parsed violation status:");
      Serial.println(violationStatus);

      manager->displayManager->setWarningData(
        warnText,
        threshold,
        tip
      );

      if (violationStatus == "BLUE") {
        Serial.println("Violation status:blue (blue led on)");
        ledManager.setBlue();
      } else if (violationStatus == "RED") {
        Serial.println("Violation status: red (red led on)");
        ledManager.setRed();
      } else {
        Serial.println("Violation status unknown");
      }
    } else {
      Serial.println("Invalid WARNTEXT message format");
    }
  }

  else if (received.startsWith("RESOLVED:")){
    //TODO: manager->displayManager->clearFault();
    ledManager.setGreen();
  }

  else if (received == "ERROR:WEBAPP_OFFLINE") {
    Serial.println("Webapp offline error received");

    manager->displayManager->setFault("Webapp offline");
  }

  else if (received == "ERROR:WEBAPP_CLEAR") {
    Serial.println("Webapp error cleared");
  }

  else {
    Serial.println("Unknown BLE message format");
  }
}