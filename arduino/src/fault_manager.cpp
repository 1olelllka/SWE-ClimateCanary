#include "fault_manager.h"

uint32_t FaultManager::mask(FaultType fault) const {
  return 1UL << static_cast<uint8_t>(fault);
}

void FaultManager::set(FaultType fault) {
  activeFaults |= mask(fault);
}

void FaultManager::clear(FaultType fault) {
  activeFaults &= ~mask(fault);
}

bool FaultManager::isActive(FaultType fault) const {
  return (activeFaults & mask(fault)) != 0;
}

bool FaultManager::hasAny() const {
  return activeFaults != 0;
}

uint8_t FaultManager::count() const {
  uint8_t result = 0;

  for (uint8_t i = 0; i < 5; i++) {
    if ((activeFaults & (1UL << i)) != 0) {
      result++;
    }
  }

  return result;
}

String FaultManager::textFor(FaultType fault) const {
  switch (fault) {
    case FaultType::WebappOffline:
      return "Webapp offline";
    case FaultType::PiDisconnected:
      return "Pi disconnected";
    case FaultType::SensorReadFailed:
      return "Sensor failed";
    case FaultType::BleInitFailed:
      return "BLE init failed";
    case FaultType::SensorInitFailed:
      return "BME init failed";
  }

  return "Unknown fault";
}

String FaultManager::textAt(uint8_t index) const {
  uint8_t seen = 0;

  for (uint8_t i = 0; i < 5; i++) {
    if ((activeFaults & (1UL << i)) == 0) {
      continue;
    }

    if (seen == index) {
      return textFor(static_cast<FaultType>(i));
    }

    seen++;
  }

  return "No faults";
}