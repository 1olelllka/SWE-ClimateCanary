#include <Arduino.h>
#include "sensor_manager.h"
#include "ble_manager.h"
#include "display_manager.h"
#include "config.h"
#include "button_manager.h"
#include "led_manager.h"
#include "fault_manager.h"

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
LedManager ledManager;
FaultManager faultManager;

bool fatalStartupFault = false;
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
  ledManager.begin();
  displayManager.begin();

  if (!sensorManager.begin()) {
    fatalStartupFault = true;
    faultManager.set(FaultType::SensorInitFailed);  
    displayManager.showSetupFault(faultManager.activeText());                                         
    Serial.println("BME688 init failed");
    return;
  }

  if (!bleManager.begin(&displayManager)) {
    Serial.println("BLE init failed");  
    return;
  }

  Serial.println("System ready");
}

void loop() {
  if (fatalStartupFault) {
    return;
  }

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

  if (now - lastSensorRead >= bleManager.measureAndSendIntervalMs) {
    lastSensorRead = now;

    if (now < SENSOR_WARMUP_TIME_MS) {
      displayManager.showStabilizing(SENSOR_WARMUP_TIME_MS - now);
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
        displayManager.showFillingBuffer( 
          sensorManager.getSampleCount(), 
          sensorManager.getRequiredSamples()
        );
        Serial.println("Collecting sensor data in buffer, waiting for valid reading...");
      }
    }
  }

  if (bleManager.isConnected() && now - lastBleSend >= bleManager.measureAndSendIntervalMs) {
    lastBleSend = now;

    const SensorReading reading = sensorManager.getReading();

    if (reading.valid) {
      bleManager.sendReading(reading);
    } else {
      Serial.println("Skipping BLE send: invalid sensor reading");
    }
  }
}