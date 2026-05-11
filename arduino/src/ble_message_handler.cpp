#include "ble_message_handler.h"
#include "ble_manager.h"

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
  } else if (received.startsWith("FREQUENCY:")) {
    //TODO: update frequency based on message
  } else if (received.startsWith("WARNTEXT:")) {
    int thresholdIndex = received.indexOf("TRESHOLD:");
    int tipIndex = received.indexOf("TIP:");

    if (thresholdIndex != -1 && tipIndex != -1) {
      String warnText = received.substring(9, thresholdIndex);
      String threshold = received.substring(thresholdIndex + 10, tipIndex);
      String tip = received.substring(tipIndex + 4);

      warnText.trim();
      threshold.trim();
      tip.trim();

      Serial.println("Parsed warning:");
      Serial.println(warnText);

      Serial.println("Parsed threshold:");
      Serial.println(threshold);

      Serial.println("Parsed tip:");
      Serial.println(tip);

      manager->displayManager->setWarningData(
        warnText,
        threshold,
        tip
      );
    } 
  } else if (received.startsWith("ERROR:")) {
      String faultText = received.substring(6);
      faultText.trim();

      if (faultText.length() > 0) {
        Serial.println("Parsed fault: " + faultText);

        manager->displayManager->setFault(faultText);
      }
  }
}