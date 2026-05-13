#include "led_manager.h"

void LedManager::begin() {
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_BLUE_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);
}

void LedManager::setOff() {
  analogWrite(LED_RED_PIN, 0);
  analogWrite(LED_BLUE_PIN, 0);
  analogWrite(LED_GREEN_PIN, 0);
}

void LedManager::setGreen() {
  analogWrite(LED_RED_PIN, 0);
  analogWrite(LED_BLUE_PIN, 0);
  analogWrite(LED_GREEN_PIN, 255);
}

void LedManager::setBlue() {
  analogWrite(LED_RED_PIN, 0);
  analogWrite(LED_BLUE_PIN, 255);
  analogWrite(LED_GREEN_PIN, 0);
}

void LedManager::setRed() {
  analogWrite(LED_RED_PIN, 255);
  analogWrite(LED_BLUE_PIN, 0);
  analogWrite(LED_GREEN_PIN, 0);
}

void LedManager::update() {
  switch (ledMode) {
    case LedMode::Off:
      setOff();
      break;
    case LedMode::Green:
      setGreen();
      break;
    case LedMode::Blue:
      setBlue();
      break;
    case LedMode::Red:
      setRed();
      break;
  }
}