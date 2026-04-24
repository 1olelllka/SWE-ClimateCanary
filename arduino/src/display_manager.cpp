#include "display_manager.h"

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
  lcd.print("Init modules");
}

void DisplayManager::showWaiting() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Waiting for Pi");
  lcd.setCursor(0, 1);
  lcd.print("SensorStation");
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

  lcd.setCursor(0, 0);
  lcd.print("T:");
  lcd.print(reading.temperatureC, 1);
  lcd.print(" H:");
  lcd.print(reading.humidityPct, 0);

  lcd.setCursor(0, 1);
  lcd.print("CO2:");
  lcd.print(reading.airQualityIndex, 0);
}