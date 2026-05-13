#pragma once

#include <Arduino.h>

class ButtonManager {
public:
  void begin(uint8_t pin);
  void update();
  bool wasPressed();

private:
  uint8_t pin = 0;

  bool lastReading = HIGH;
  bool pressed = false;

  unsigned long lastPressTime = 0;

  static const unsigned long debounceDelayMs = 150;
};