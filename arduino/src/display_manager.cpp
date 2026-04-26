#include "display_manager.h"

static DisplayManager::DisplayMode currentMode = DisplayManager::DisplayMode::Regular;
static DisplayManager::RegularModeDisplay currentRegularPage = DisplayManager::RegularModeDisplay::Temperature;
static DisplayManager::WarningModeDisplay currentWarningPage = DisplayManager::WarningModeDisplay::WarnMessage;

void DisplayManager::begin() {
  lcd.begin(16, 2);
  lcd.setRGB(0, 128, 255);
  lcd.clear();
}

void DisplayManager::showStartup() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Starting...");
  lcd.setCursor(0, 1);
  lcd.print("Collecting data...");
}

void DisplayManager::showConnected() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Connected");
  lcd.setCursor(0, 1);
  lcd.print("Pi joined");
}

void DisplayManager::showDisconnected() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Disconnected");
  lcd.setCursor(0, 1);
  lcd.print("Waiting...");
}

void DisplayManager::showMessageFromPi(const String& msg) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("From Pi:");
  lcd.setCursor(0, 1);

  if (msg.length() <= 16) {
    lcd.print(msg);
  } else {
    lcd.print(msg.substring(0, 16));
  }
}

void DisplayManager::showReading(const SensorReading& reading) {
  lcd.clear();

  if (!reading.valid) {
    lcd.setCursor(0, 0);
    lcd.print("Sensor invalid");
    lcd.setCursor(0, 1);
    lcd.print("Check BME680");
    return;
  }

  if (currentMode == DisplayMode::Regular) {
    switch (currentRegularPage) {
      case RegularModeDisplay::Temperature:
        lcd.setCursor(0, 0);
        lcd.print("Mode: Regular");
        lcd.setCursor(0, 1);
        lcd.print("Temp.: ");
        lcd.print(reading.temperatureC, 1);
        lcd.print(" C");
        break;

      case RegularModeDisplay::Humidity:
        lcd.setCursor(0, 0);
        lcd.print("Mode: Regular");
        lcd.setCursor(0, 1);
        lcd.print("Humidity: ");  
        lcd.print(reading.humidityPct, 0);
        lcd.print("%");
        break;

      case RegularModeDisplay::AirQuality:
        lcd.setCursor(0, 0);
        lcd.print("Mode: Regular");
        lcd.setCursor(0, 1);
        lcd.print("CO2: ");
        lcd.print(reading.airQualityIndex, 0);
        lcd.print("%");
        break;
    }
  }

  else if (currentMode == DisplayMode::Warning) {
    switch (currentWarningPage) {
      case WarningModeDisplay::WarnMessage:
        lcd.setCursor(0, 0);
        lcd.print("Mode: Warning");
        lcd.setCursor(0, 1);
        lcd.print("Limit exceeded");
        break;

      case WarningModeDisplay::ExceededThreshold:
        lcd.setCursor(0, 0);
        lcd.print("Mode: Warning");
        lcd.setCursor(0, 1);
        lcd.print("T/H/Air quality");
        break;

      case WarningModeDisplay::AdviceMessage:
        lcd.setCursor(0, 0);
        lcd.print("Mode: Warning");
        lcd.setCursor(0, 1);
        lcd.print("No advice yet");
        break;
    }
  }

  else if (currentMode == DisplayMode::Fault) {
    lcd.setCursor(0, 0);
    lcd.print("Mode: Fault");
    lcd.setCursor(0, 1);
    lcd.print("Code: NONE");
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