#include "led_manager.h"

void LedManager::begin() {
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
}

void LedManager::setConnected(bool connected) {
  digitalWrite(LED_PIN, connected ? HIGH : LOW);
}

void LedManager::blinkActivity() {
  activityState = !activityState;
  digitalWrite(LED_PIN, activityState ? HIGH : LOW);
}