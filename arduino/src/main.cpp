#include <Arduino.h>
#include "sensor_manager.h"
#include "ble_manager.h"
#include "display_manager.h"
#include "config.h"
#include "button_manager.h"

#define MODE_BUTTON_PIN D2
#define NEXT_PAGE_BUTTON_PIN D3
#define PREVIOUS_PAGE_BUTTON_PIN D4
#define SENSOR_WARMUP_TIME_MS 30000

SensorManager sensorManager;
BLEManager bleManager;
DisplayManager displayManager;
ButtonManager modeButton;
ButtonManager nextPageButton;
ButtonManager previousPageButton;


unsigned long lastSensorRead = 0;
unsigned long lastBleSend = 0;

bool isAdvertising = false;

void setup() {
  Serial.begin(9600);
  while (!Serial) {
    ;
  }

  modeButton.begin(MODE_BUTTON_PIN);
  nextPageButton.begin(NEXT_PAGE_BUTTON_PIN);
  previousPageButton.begin(PREVIOUS_PAGE_BUTTON_PIN);
  displayManager.begin();
  displayManager.showStartup();

  if (!sensorManager.begin()) {
    Serial.println("BME680 init failed");
    return;
  }

  if (!bleManager.begin(&displayManager)) {
    Serial.println("BLE init failed");
    return;
  }

  Serial.println("System ready");
}

void loop() {
  bleManager.poll();

  modeButton.update();
  nextPageButton.update();
  previousPageButton.update();

  if (modeButton.wasPressed()) { 
    displayManager.nextMode();
    displayManager.showReading(sensorManager.getReading());
  }

  if (nextPageButton.wasPressed()) {
    displayManager.nextPage();
    displayManager.showReading(sensorManager.getReading());
  }

  if (previousPageButton.wasPressed()) {
    displayManager.previousPage();
    displayManager.showReading(sensorManager.getReading());
  }

  const unsigned long now = millis();

  if (now - lastSensorRead >= SENSOR_INTERVAL_MS) {
    lastSensorRead = now;

    if (now < SENSOR_WARMUP_TIME_MS) {
      Serial.println("Still in gas sensor warm-up period, skipping sensor read");
    } else if (sensorManager.update()) {
      const SensorReading reading = sensorManager.getReading();
      if (reading.valid) {
        displayManager.showReading(reading);

        if (!isAdvertising) {
          BLE.advertise();
          Serial.println("Advertising...");          
          isAdvertising = true;
        }
      } else {
        Serial.println("Collecting sensor data in buffer, waiting for valid reading...");
      }
    } else {
      Serial.println("Sensor read failed");
    }
  }

  if (bleManager.isConnected() && now - lastBleSend >= BLE_SEND_INTERVAL_MS) {
    lastBleSend = now;

    const SensorReading reading = sensorManager.getReading();

    if (reading.valid) {
      bleManager.sendReading(reading);
    } else {
      Serial.println("Skipping BLE send: invalid sensor reading");
    }
  }
}