#pragma once

#include <Arduino.h>
#include <Wire.h>
#include <rgb_lcd.h>   
#include "sensor_manager.h"

class DisplayManager {
public:
  void begin(); 
  void showStartup();
  void showWaiting();
  void showConnected();
  void showDisconnected();
  void showMessageFromPi(const String& msg);
  void showReading(const SensorReading& reading);

private:
  rgb_lcd lcd;   
};