#include "button_manager.h"

void ButtonManager::begin(uint8_t p) {
  pin = p;
  pinMode(pin, INPUT_PULLUP);

  lastReading = digitalRead(pin);
  pressed = false;
  lastPressTime = 0;
}

void ButtonManager::update() {
  const bool currentReading = digitalRead(pin);
  const unsigned long now = millis();

  // INPUT_PULLUP:
  // HIGH = button not pressed
  // LOW  = button pressed
  // Falling edge: HIGH -> LOW
  if (lastReading == HIGH && currentReading == LOW) {
    if (now - lastPressTime > debounceDelayMs) {
      pressed = true;
      lastPressTime = now;
    }
  }

  lastReading = currentReading;
}

bool ButtonManager::wasPressed() {
  if (pressed) {
    pressed = false;
    return true;
  }

  return false;
}