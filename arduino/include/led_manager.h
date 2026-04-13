#pragma once

#include <Arduino.h>

class LedManager {
public:
  void begin();
  void setConnected(bool connected);
  void blinkActivity();

private:
  static const int LED_PIN = LED_BUILTIN;
  bool activityState = false;
};