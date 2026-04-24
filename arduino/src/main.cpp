#include <Arduino.h>
#include "sensor_manager.h"
#include "ble_manager.h"
#include "display_manager.h"
#include "config.h"

SensorManager sensorManager;
BLEManager bleManager;
DisplayManager displayManager;

unsigned long lastSensorRead = 0;
unsigned long lastBleSend = 0;

bool isAdvertising = false;

void setup() {
  Serial.begin(9600);
  while (!Serial) {
    ;
  }

  displayManager.begin();
  displayManager.showStartup();

  if (!sensorManager.begin()) {
    Serial.println("BME680 init failed");
    displayManager.showDisconnected();
    return;
  }

  if (!bleManager.begin(&displayManager)) {
    Serial.println("BLE init failed");
    displayManager.showDisconnected();
    return;
  }

  Serial.println("System ready");
  displayManager.showWaiting();
}

void loop() {
  bleManager.poll();

  const unsigned long now = millis();

  if (now - lastSensorRead >= SENSOR_INTERVAL_MS) {
    lastSensorRead = now;

    if (now < 30000) {
      Serial.println("Still in gas sensor warm-up period, skipping sensor read");
    }
    else if (sensorManager.update()) {
      const SensorReading reading = sensorManager.getReading();
      if (reading.valid) {
        displayManager.showReading(reading);

        if (!isAdvertising) {
          BLE.advertise();
          Serial.println("Advertising...");          
          isAdvertising = true;
        }
      }
      else {
        Serial.println("Collecting sensor data in buffer, waiting for valid reading...");
      }
    } 
    else {
      Serial.println("Sensor read failed");
    }
  }

  if (bleManager.isConnected() && now - lastBleSend >= BLE_SEND_INTERVAL_MS) {
    lastBleSend = now;

    const SensorReading reading = sensorManager.getReading();

    if (reading.valid) {
      bleManager.sendReading(reading);
    } 
    else {
      Serial.println("Skipping BLE send: invalid sensor reading");
    }
  }
}