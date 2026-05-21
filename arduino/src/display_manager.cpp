#include "display_manager.h"

static DisplayManager::DisplayMode currentMode = DisplayManager::DisplayMode::Regular;
static DisplayManager::RegularModeDisplay currentRegularPage = DisplayManager::RegularModeDisplay::Temperature;
static DisplayManager::WarningModeDisplay currentWarningPage = DisplayManager::WarningModeDisplay::WarnMessage;

void DisplayManager::begin() {
  lcd.begin(16, 2);
  lcd.setRGB(0, 128, 255);
  lcd.clear();
}

static void printLine(rgb_lcd& lcd, uint8_t row, const String& text) {
  lcd.setCursor(0, row);

  String padded = text;
  while (padded.length() < 16) {
    padded += " ";
  }

  lcd.print(padded.substring(0, 16));
}

static constexpr uint8_t LCD_WIDTH = 16;
static constexpr unsigned long SCROLL_INTERVAL_MS = 400;

String DisplayManager::scrollText(const String& text) {
  if (text.length() <= LCD_WIDTH) {
    scrollIndex = 0;
    lastScrollText = text;
    return text;
  }

  String loopText = text + " ";

  if (loopText != lastScrollText) {
    lastScrollText = loopText;
    scrollIndex = 0;
    lastScrollUpdate = millis();
  }

  if (millis() - lastScrollUpdate >= SCROLL_INTERVAL_MS) {
    lastScrollUpdate = millis();
    scrollIndex = (scrollIndex + 1) % loopText.length();
  }

  String visible = "";

  for (uint8_t i = 0; i < LCD_WIDTH; i++) {
    const uint8_t charIndex = (scrollIndex + i) % loopText.length();
    visible += loopText[charIndex];
  }

  return visible;
}

void DisplayManager::showSetupFault(const String& faultText) {
  lcd.clear();
  printLine(lcd, 0, "Setup Fault");
  printLine(lcd, 1, faultText);
}

void DisplayManager::showStabilizing(unsigned long remainingMs) {
  const unsigned long remainingSec = (remainingMs + 999) / 1000;

  printLine(lcd, 0, "Stabilize Sensor");
  printLine(lcd, 1, "Wait: " + String(remainingSec) + "s");
}

void DisplayManager::showFillingBuffer(uint8_t currentSamples, uint8_t requiredSamples) {
  printLine(lcd, 0, "Filling buffer");
  printLine(lcd, 1, "Samples: " + String(currentSamples) + "/" + String(requiredSamples));
}

void DisplayManager::setWarningData(
  const String& warnText,
  const String& threshold,
  const String& tip
) {
  const bool changed =
    currentWarnText != warnText ||
    currentThreshold != threshold ||
    currentTip != tip;

  currentWarnText = warnText;
  currentThreshold = threshold;
  currentTip = tip;

  if (changed) {
    scrollIndex = 0;
    lastScrollText = "";
    lastScrollUpdate = millis();
  }
}

void DisplayManager::clearWarningData() {
  currentWarnText = "";
  currentThreshold = "";
  currentTip = "";
}

void DisplayManager::updateFault(const String& faultText) {
  currentFaultText = faultText;

  if (currentMode == DisplayMode::Fault) {
    printLine(lcd, 0, "Fault Mode");
    printLine(lcd, 1, currentFaultText);
  }
}

void DisplayManager::showReading(const SensorReading& reading) {
  if (!reading.valid) {
    printLine(lcd, 0, "Sensor invalid");
    printLine(lcd, 1, "Check BME688");
    return;
  }

  if (currentMode == DisplayMode::Regular) {
    switch (currentRegularPage) {
      case RegularModeDisplay::Temperature:
        printLine(lcd, 0, "Regular Mode");
        printLine(lcd, 1, "Temp.: " + String(reading.temperatureC, 1) + "C");
        break;

      case RegularModeDisplay::Humidity:
        printLine(lcd, 0, "Regular Mode");
        printLine(lcd, 1, "Humidity: " + String(reading.humidityPct, 0) + "%");
        break;

      case RegularModeDisplay::AirQuality:
        printLine(lcd, 0, "Regular Mode");
        printLine(lcd, 1, "IAQ: " + String(reading.airQualityIndex, 0) + "%");
        break;
    }
  }

  else if (currentMode == DisplayMode::Warning) {
    switch (currentWarningPage) {
      case WarningModeDisplay::WarnMessage:
        printLine(lcd, 0, "Warn Mode(text)");
        printLine(lcd, 1, scrollText(currentWarnText));
        break;

      case WarningModeDisplay::ExceededThreshold:
        printLine(lcd, 0, "Warn Mode");
        printLine(lcd, 1, "Threshold: " + currentThreshold);
        break;

      case WarningModeDisplay::AdviceMessage:
        printLine(lcd, 0, "Warn Mode(tip)");
        printLine(lcd, 1, scrollText(currentTip));
        break;
    }
  }

  else if (currentMode == DisplayMode::Fault) {
    printLine(lcd, 0, "Fault Mode");
    printLine(lcd, 1, currentFaultText);
  }
}

void DisplayManager::nextMode() {
  if (currentMode == DisplayMode::Regular) {
    currentMode = DisplayMode::Warning;
  } else if (currentMode == DisplayMode::Warning) {
    currentMode = DisplayMode::Fault;
  } else {
    currentMode = DisplayMode::Regular;
  }
}

void DisplayManager::nextPage() {
  if (currentMode == DisplayMode::Regular) {
    if (currentRegularPage == RegularModeDisplay::Temperature) {
      currentRegularPage = RegularModeDisplay::Humidity;
    } else if (currentRegularPage == RegularModeDisplay::Humidity) {
      currentRegularPage = RegularModeDisplay::AirQuality;
    } else {
      currentRegularPage = RegularModeDisplay::Temperature;
    }
  }

  else if (currentMode == DisplayMode::Warning) {
    if (currentWarningPage == WarningModeDisplay::WarnMessage) {
      currentWarningPage = WarningModeDisplay::ExceededThreshold;
    } else if (currentWarningPage == WarningModeDisplay::ExceededThreshold) {
      currentWarningPage = WarningModeDisplay::AdviceMessage;
    } else {
      currentWarningPage = WarningModeDisplay::WarnMessage;
    }
  }
}

void DisplayManager::previousPage() {
  if (currentMode == DisplayMode::Regular) {
    if (currentRegularPage == RegularModeDisplay::Temperature) {
      currentRegularPage = RegularModeDisplay::AirQuality;
    } else if (currentRegularPage == RegularModeDisplay::AirQuality) {
      currentRegularPage = RegularModeDisplay::Humidity;
    } else {
      currentRegularPage = RegularModeDisplay::Temperature;
    }
  }

  else if (currentMode == DisplayMode::Warning) {
    if (currentWarningPage == WarningModeDisplay::WarnMessage) {
      currentWarningPage = WarningModeDisplay::AdviceMessage;
    } else if (currentWarningPage == WarningModeDisplay::AdviceMessage) {
      currentWarningPage = WarningModeDisplay::ExceededThreshold;
    } else {
      currentWarningPage = WarningModeDisplay::WarnMessage;
    }
  }
}