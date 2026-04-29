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
  } else if (received.startsWith("ERROR:")) {
    //TODO: print error message on LCD
  } else if (received.startsWith("ADVICE:")) {
    //TODO: print advice message on LCD
  } else if (received.startsWith("SENSOR_READING_INTERVAL:")) {
    //TODO: update sensor reading interval
  } else if (received.startsWith("BLE_SEND_INTERVAL:")) {
    //TODO: update BLE send interval
  }
}