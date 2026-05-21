#include "fault_manager.h"

void FaultManager::set(FaultType fault) {
  switch (fault) {
    case FaultType::WebappOffline:
      webappOffline = true;
      break;

    case FaultType::PiDisconnected:
      piDisconnected = true;
      break;

    case FaultType::SensorReadFailed:
      sensorReadFailed = true;
      break;

    case FaultType::BleInitFailed:
      bleInitFailed = true;
      break;

    case FaultType::SensorInitFailed:
      sensorInitFailed = true;
      break;

    case FaultType::None:
      break;
  }
}

void FaultManager::clear(FaultType fault) {
  switch (fault) {
    case FaultType::WebappOffline:
      webappOffline = false;
      break;

    case FaultType::PiDisconnected:
      piDisconnected = false;
      break;

    case FaultType::SensorReadFailed:
      sensorReadFailed = false;
      break;

    case FaultType::BleInitFailed:
      bleInitFailed = false;
      break;

    case FaultType::SensorInitFailed:
      sensorInitFailed = false;
      break;

    case FaultType::None:
      break;
  }
}

bool FaultManager::hasAny() const {
  return webappOffline ||
         piDisconnected ||
         sensorReadFailed ||
         bleInitFailed ||
         sensorInitFailed;
}

FaultType FaultManager::activeFault() const {
  if (sensorInitFailed) {
    return FaultType::SensorInitFailed;
  }

  if (bleInitFailed) {
    return FaultType::BleInitFailed;
  }

  if (sensorReadFailed) {
    return FaultType::SensorReadFailed;
  }

  if (piDisconnected) {
    return FaultType::PiDisconnected;
  }

  if (webappOffline) {
    return FaultType::WebappOffline;
  }

  return FaultType::None;
}

String FaultManager::activeText() const {
  switch (activeFault()) {
    case FaultType::SensorInitFailed:
      return "BME init failed";

    case FaultType::BleInitFailed:
      return "BLE init failed";

    case FaultType::SensorReadFailed:
      return "Sensor failed";

    case FaultType::PiDisconnected:
      return "Pi disconnected";

    case FaultType::WebappOffline:
      return "Webapp offline";

    case FaultType::None:
      return "";
  }

  return "";
}