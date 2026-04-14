#include "sensor_manager.h"

bool SensorManager::begin() {
  if (!bme.begin()) {
    reading.valid = false;
    return false;
  }

  // Recommended oversampling / filter setup
  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_2X);
  bme.setPressureOversampling(BME680_OS_4X);
  bme.setIIRFilterSize(BME680_FILTER_SIZE_3);

  // Gas heater for gas resistance readings
  bme.setGasHeater(320, 150);

  reading.valid = false;
  return true;
}

bool SensorManager::update() {
  if (!bme.performReading()) {
    reading.valid = false;
    return false;
  }

  reading.temperatureC = bme.temperature;
  reading.humidityPct = bme.humidity;
  reading.pressurehPa = bme.pressure / 100.0f;
  reading.gasResistanceKOhm = bme.gas_resistance / 1000.0f;
  reading.valid = true;

  return true;
}

SensorReading SensorManager::getReading() const {
  return reading;
}