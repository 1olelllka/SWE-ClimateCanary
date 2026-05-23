#include "warning_manager.h"

WarningData* WarningManager::getWarningSlot(MeasurementType measurement) {
  switch (measurement) {
    case MeasurementType::Temperature:
      return &temperatureWarning;

    case MeasurementType::Humidity:
      return &humidityWarning;

    case MeasurementType::AirQuality:
      return &airQualityWarning;

    case MeasurementType::Unknown:
      return nullptr;
  }

  return nullptr;
}

const WarningData* WarningManager::getWarningSlot(MeasurementType measurement) const {
  switch (measurement) {
    case MeasurementType::Temperature:
      return &temperatureWarning;

    case MeasurementType::Humidity:
      return &humidityWarning;

    case MeasurementType::AirQuality:
      return &airQualityWarning;

    case MeasurementType::Unknown:
      return nullptr;
  }

  return nullptr;
}

int WarningManager::severityPriority(SeverityLevel severity) const {
  switch (severity) {
    case SeverityLevel::Red:
      return 3;

    case SeverityLevel::Yellow:
      return 2;

    case SeverityLevel::Green:
      return 1;

    case SeverityLevel::None:
      return 0;
  }

  return 0;
}

void WarningManager::set(
  MeasurementType measurement,
  SeverityLevel severity,
  const String& warnText,
  const String& threshold,
  const String& tip
) {
  WarningData* target = getWarningSlot(measurement);

  if (target == nullptr || severity == SeverityLevel::None) {
    return;
  }

  target->measurement = measurement;
  target->severity = severity;
  target->warnText = warnText;
  target->threshold = threshold;
  target->tip = tip;
  target->active = true;
}

void WarningManager::clear(MeasurementType measurement) {
  WarningData* target = getWarningSlot(measurement);

  if (target != nullptr) {
    *target = WarningData();
  }
}

void WarningManager::clearAll() {
  temperatureWarning = WarningData();
  humidityWarning = WarningData();
  airQualityWarning = WarningData();
}

bool WarningManager::hasAny() const {
  return temperatureWarning.active ||
         humidityWarning.active ||
         airQualityWarning.active;
}

SeverityLevel WarningManager::activeWarning() const {
  return activeData().severity;
}

WarningData WarningManager::activeData() const {
  const WarningData* warnings[] = {
    &temperatureWarning,
    &humidityWarning,
    &airQualityWarning
  };

  const WarningData* best = nullptr;
  int bestPriority = 0;

  for (const WarningData* warning : warnings) {
    if (!warning->active) {
      continue;
    }

    const int priority = severityPriority(warning->severity);

    if (priority > bestPriority) {
      best = warning;
      bestPriority = priority;
    }
  }

  if (best != nullptr) {
    return *best;
  }

  return WarningData();
}