#include "led_manager.h"

void LedManager::begin() {
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_BLUE_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);

  setOff();
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

void LedManager::update(LedMode ledMode) {
  if (ledMode == LedMode::Off) {
    setOff();
  } else if (ledMode == LedMode::Green) {
    setGreen();
  } else if (ledMode == LedMode::Blue) {
    setBlue();
  } else if (ledMode == LedMode::Red) {
    setRed();
  }
}