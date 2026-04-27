#pragma once

#include <Arduino.h>
#include <Wire.h>
#include <rgb_lcd.h>   
#include "sensor_manager.h"

class DisplayManager {
public:
  enum class DisplayMode {
  Regular,
  Warning,
  Fault
};

  enum class RegularModeDisplay {
    Temperature,
    Humidity,
    AirQuality
  };

  enum class WarningModeDisplay {
    WarnMessage,
    ExceededThreshold,
    AdviceMessage
  };

  enum class FaultModeDisplay {
    FaultMessage
  };

  void begin(); 
  void showStartup();
  void showReading(const SensorReading& reading);

  void nextMode();
  void nextPage();
  void previousPage();

  void setFault(const String& text, const String& code);
  void clearFault();

private:
  rgb_lcd lcd;
};