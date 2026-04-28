#pragma once

#include <Arduino.h>

class ButtonManager {
public:
  void begin(uint8_t pin);
  void update();
  bool wasPressed();

private:
  uint8_t pin;
  bool lastReading = HIGH;
  bool stableState = HIGH;
  bool pressed = false;

  unsigned long lastDebounceTime = 0;
  static const unsigned long debounceDelay = 30;
};