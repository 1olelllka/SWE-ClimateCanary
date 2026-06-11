#include "sensor_manager.h"
#define HEATING_TIME_MS 150
#define HEATING_TEMP_C 320

bool SensorManager::begin() {
  if (!bme.begin()) {
    reading.valid = false;
    return false;
  }

  //oversampling for reducing noise
  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_4X);
  bme.setIIRFilterSize(BME680_FILTER_SIZE_7);

  /*
  gas heater for gas resistance readings
  heating temp for measurement 320° C,
  heating time: 150ms 
  */
  bme.setGasHeater(HEATING_TEMP_C, HEATING_TIME_MS);

  reading.valid = false;
  return true;
}

bool SensorManager::update() {
  // Ignore gas readings during startup stabilization period.
  // This does not keep the heater on continuously.
  if (millis() < SENSOR_STABILIZATION_TIME_MS) {
    return false;
  }

  if (!bme.performReading()) {
    return false;
  }

  //accept gas reading if heater reached target temperature
  const float gasKOhm = bme.gas_resistance / 1000.0f;
  if (gasKOhm < 1.0f) {
    return false;
  }
  
  temperatureBuffer[sampleIndex]    = bme.temperature;
  humidityBuffer[sampleIndex]       = bme.humidity;
  gasResistanceBuffer[sampleIndex]  = gasKOhm;

  sampleIndex = (sampleIndex + 1) % NUM_SAMPLES;
  if (sampleCount < NUM_SAMPLES) {
    sampleCount++;
  }

  float sumTemperature = 0, sumHumidity = 0, sumGasResistance = 0;
  for (uint8_t i = 0; i < sampleCount; i++) {
    sumTemperature    += temperatureBuffer[i];
    sumHumidity       += humidityBuffer[i];
    sumGasResistance  += gasResistanceBuffer[i]; 
  }

  reading.temperatureC      = sumTemperature / sampleCount;
  reading.humidityPct       = sumHumidity / sampleCount;
  reading.gasResistanceKOhm = sumGasResistance / sampleCount;

  //initialize baseline after first average is ready
  if (!baselineInitialized && sampleCount == NUM_SAMPLES) {
    gasBaseline = reading.gasResistanceKOhm;
    baselineInitialized = true;
  }

  float airQuality = 0.0f;

  if (baselineInitialized) {
    //slowly update gas baseline to track long-term air quality while preserving short-term changes
    gasBaseline = 0.99f * gasBaseline + 0.01f * reading.gasResistanceKOhm;

      /*
      > 1: cleaner than baseline
      = 1: same as baseline
      < 1: more polluted than baseline
      */
    float ratio = reading.gasResistanceKOhm / gasBaseline;

    //ratio 1 ~ 100, ratio 0.5 ~ 50,...
    airQuality = ratio * 100.0f;

    //range limit
    if (airQuality > 100.0f) {
      airQuality = 100.0f;
    }
    else if (airQuality < 0.0f) {
      airQuality = 0.0f;
    }
  }

  reading.airQualityIndex = airQuality;

  reading.valid = (sampleCount == NUM_SAMPLES) && baselineInitialized;

  return true;
}

uint8_t SensorManager::getSampleCount() const {
  return sampleCount;
}

uint8_t SensorManager::getRequiredSamples() const {
  return NUM_SAMPLES;
}

SensorReading SensorManager::getReading() const {
  return reading;
}