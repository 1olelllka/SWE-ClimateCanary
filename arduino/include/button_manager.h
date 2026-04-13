#pragma once

#include <Arduino.h>

class ButtonManager {
public:
  void begin(uint8_t pin);
  void update();

  bool wasPressed();   

private:
  uint8_t pin;
  bool lastState = HIGH;
  bool pressed = false;
};