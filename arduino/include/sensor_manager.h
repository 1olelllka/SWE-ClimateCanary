#pragma once

#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_BME680.h>

struct SensorReading {
  float temperatureC;
  float humidityPct;
  float pressurehPa;
  float gasResistanceKOhm;
  bool valid;
};

class SensorManager {
public:
  bool begin();
  bool update();
  SensorReading getReading() const;

private:
  Adafruit_BME680 bme;
  SensorReading reading = {0, 0, 0, 0, false};
};