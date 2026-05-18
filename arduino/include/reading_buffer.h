#pragma once

#include <Arduino.h>
#include "sensor_manager.h"

struct BufferedReading {
  SensorReading reading;
  String timestamp;
  unsigned long millisOffset;
};

class ReadingBuffer {
public:
  static constexpr uint8_t BUFFER_SIZE = 100;

  bool push(
    const SensorReading& reading,
    const String& timestamp,
    unsigned long millisOffset
  );

  bool pop(BufferedReading& outReading);

  bool isEmpty() const;
  bool isFull() const;

  uint8_t size() const;
  uint8_t capacity() const;

  void clear();

private:
  BufferedReading buffer[BUFFER_SIZE];

  uint8_t head = 0;
  uint8_t tail = 0;
  uint8_t count = 0;
};