#pragma once
#include <Arduino.h>

enum class FaultType : uint8_t {
  WebappOffline = 0,
  PiDisconnected = 1,
  SensorReadFailed = 2,
  BleInitFailed = 3,
  SensorInitFailed = 4
};

class FaultManager {
public:
  void set(FaultType fault);
  void clear(FaultType fault);

  bool isActive(FaultType fault) const;
  bool hasAny() const;

  uint8_t count() const;
  String textAt(uint8_t index) const;

private:
  uint32_t activeFaults = 0;

  uint32_t mask(FaultType fault) const;
  String textFor(FaultType fault) const;
};