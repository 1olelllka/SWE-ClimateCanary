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
    //TODO: update warning text based on message
  } else if (received.startsWith("ERROR:")) {
    //TODO: implement error code handling based on message
  }

}