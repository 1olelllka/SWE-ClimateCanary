#pragma once

#include <Arduino.h>

enum class FaultType {
  None,
  WebappOffline,
  PiDisconnected,
  SensorReadFailed,
  BleInitFailed,
  SensorInitFailed
};

class FaultManager {
public:
  void set(FaultType fault);
  void clear(FaultType fault);

  bool hasAny() const;

  FaultType activeFault() const;
  String activeText() const;

private:
  bool webappOffline = false;
  bool piDisconnected = false;
  bool sensorReadFailed = false;
  bool bleInitFailed = false;
  bool sensorInitFailed = false;
};