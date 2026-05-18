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
  void showStabilizing(unsigned long remainingMs);
  void showFillingBuffer(uint8_t currentSamples, uint8_t requiredSamples);
  void showReading(const SensorReading& reading);

  void nextMode();
  void nextPage();
  void previousPage();

  void showSetupFault(const String& faultText);
  void updateFault(const String& faultText);

  void setWarningData(
    const String& warnText,
    const String& threshold,
    const String& tip
  );

  void clearWarningData();
  

private:
  rgb_lcd lcd;

  String currentWarnText = "";
  String currentThreshold = "";
  String currentTip = "";
  String currentFaultText = "";
};