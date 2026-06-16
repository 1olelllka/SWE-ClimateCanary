#pragma once

#include <Arduino.h>

enum class FaultType :uint16_t {
  None              = 0,
  WebappOffline     = 101,
  PiDisconnected    = 102,
  BleInitFailed     = 103,
  SensorInitFailed  = 301,
  SensorReadFailed  = 302
};

class FaultManager {
public:
  void set(FaultType fault);
  void clear(FaultType fault);

  bool hasAny() const;

  FaultType activeFault() const;
  String activeText() const;

  uint16_t code(FaultType fault) const;
  String activeCodeText() const;

private:
  bool webappOffline = false;
  bool piDisconnected = false;
  bool sensorReadFailed = false;
  bool bleInitFailed = false;
  bool sensorInitFailed = false;
};