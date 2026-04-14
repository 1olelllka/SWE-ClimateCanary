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

    if (sensorManager.update()) {
      const SensorReading reading = sensorManager.getReading();

      Serial.print("Temperature: ");
      Serial.print(reading.temperatureC);
      Serial.println(" C");

      Serial.print("Humidity: ");
      Serial.print(reading.humidityPct);
      Serial.println(" %");

      Serial.print("Pressure: ");
      Serial.print(reading.pressurehPa);
      Serial.println(" hPa");

      Serial.print("Gas: ");
      Serial.print(reading.gasResistanceKOhm);
      Serial.println(" kOhm");

      displayManager.showReading(reading);
    } else {
      Serial.println("Sensor read failed");
    }
  }

  if (bleManager.isConnected() && now - lastBleSend >= BLE_SEND_INTERVAL_MS) {
    lastBleSend = now;

    const SensorReading reading = sensorManager.getReading();

    if (reading.valid) {
      String payload = String("{\"temperatureC\":") + String(reading.temperatureC, 2) +
                       ",\"humidityPct\":" + String(reading.humidityPct, 2) +
                       ",\"pressurehPa\":" + String(reading.pressurehPa, 2) +
                       ",\"gasResistanceKOhm\":" + String(reading.gasResistanceKOhm, 2) +
                       "}";

      bleManager.sendMessage(payload);
    }
  }
}