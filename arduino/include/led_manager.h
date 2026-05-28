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
    White,
    Green,
    Blue,
    Red
  };

  LedMode ledMode = LedMode::Off;
  void begin();
  void update(LedMode ledMode);

private:
  void setOff();
  void setWhite();
  void setGreen();
  void setBlue();
  void setRed();
};