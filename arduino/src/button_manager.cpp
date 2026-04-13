#include "button_manager.h"

void ButtonManager::begin(uint8_t p) {
  pin = p;
  pinMode(pin, INPUT_PULLUP);
}

void ButtonManager::update() {
  bool currentState = digitalRead(pin);

  if (lastState == HIGH && currentState == LOW) {
    pressed = true;
  }

  lastState = currentState;
}

bool ButtonManager::wasPressed() {
  if (pressed) {
    pressed = false;
    return true;
  }
  return false;
}