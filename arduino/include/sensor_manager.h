#pragma once

#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_BME680.h>

struct SensorReading {
  float temperatureC;
  float humidityPct;
  float gasResistanceKOhm;
  float airQualityIndex; 
  bool valid;
};

class SensorManager {
public:
  bool begin();
  bool update();
  SensorReading getReading() const;

private:
  static constexpr uint8_t NUM_SAMPLES = 5;       //average over 5 readings ~15s at 3s interval

  Adafruit_BME680 bme;
  SensorReading reading = {0, 0, 0, false};

  float gasBaseline = 0.0f;
  bool baselineInitialized = false;

  //ring buffer for averaging
  float temperatureBuffer[NUM_SAMPLES]    = {};
  float humidityBuffer[NUM_SAMPLES]       = {};
  float gasResistanceBuffer[NUM_SAMPLES]  = {};
  uint8_t sampleIndex = 0;
  uint8_t sampleCount = 0;
};