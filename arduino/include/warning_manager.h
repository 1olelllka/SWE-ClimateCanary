#pragma once

#include <Arduino.h>

enum class SeverityLevel {
  Red,
  Yellow,
  Green,
  None
};

enum class MeasurementType {
  Temperature,
  Humidity,
  AirQuality,
  Unknown
};

struct WarningData {
  MeasurementType measurement = MeasurementType::Unknown;
  SeverityLevel severity = SeverityLevel::None;

  String warnText = "";
  String threshold = "";
  String tip = "";

  bool active = false;
};

class WarningManager {
public:
  void set(
    MeasurementType measurement,
    SeverityLevel severity,
    const String& warnText,
    const String& threshold,
    const String& tip
  );

  void clear(MeasurementType measurement);
  void clearAll();

  bool hasAny() const;

  SeverityLevel activeWarning() const;
  WarningData activeData() const;

private:
  WarningData temperatureWarning;
  WarningData humidityWarning;
  WarningData airQualityWarning;

  WarningData* getWarningSlot(MeasurementType measurement);
  const WarningData* getWarningSlot(MeasurementType measurement) const;

  int severityPriority(SeverityLevel severity) const;
};