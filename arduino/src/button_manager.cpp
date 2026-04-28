#include "button_manager.h"

void ButtonManager::begin(uint8_t p) {
  pin = p;
  pinMode(pin, INPUT_PULLUP);
}

void ButtonManager::update() {
  bool currentReading = digitalRead(pin);

  if (currentReading != lastReading) {
    lastDebounceTime = millis();
  }

  if ((millis() - lastDebounceTime) > debounceDelay) {
    if (currentReading != stableState) {
      stableState = currentReading;

      if (stableState == LOW) {
        pressed = true;
      }
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