#pragma once

#include <Arduino.h>
#include "config.h"

#define LED_RED_PIN A0
#define LED_BLUE_PIN A1
#define LED_GREEN_PIN A2

class LedManager {
public:
  enum class LedMode {
    Off,
    Green,
    Blue,
    Red
  };

  LedMode ledMode = LedMode::Off;
  void begin();
  void update(LedMode ledMode);

private:
  void setOff();
  void setGreen();
  void setBlue();
  void setRed();
};