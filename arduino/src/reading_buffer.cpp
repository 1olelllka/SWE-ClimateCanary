#include "reading_buffer.h"

bool ReadingBuffer::push(const SensorReading& reading, unsigned long millisOffset) {
  if (!reading.valid) {
    return false;
  }

  buffer[head].reading = reading;
  buffer[head].millisOffset = millisOffset;

  head = (head + 1) % BUFFER_SIZE;

  if (count < BUFFER_SIZE) {
    count++;
  } else {
    tail = (tail + 1) % BUFFER_SIZE;    //overwrite oldest entry, move tail forward
  }

  return true;
}

bool ReadingBuffer::pop(BufferedReading& outReading) {
  if (isEmpty()) {
    return false;
  }

  outReading = buffer[tail];

  tail = (tail + 1) % BUFFER_SIZE;
  count--;

  return true;
}

bool ReadingBuffer::isEmpty() const {
  return count == 0;
}

bool ReadingBuffer::isFull() const {
  return count == BUFFER_SIZE;
}

uint8_t ReadingBuffer::size() const {
  return count;
}

uint8_t ReadingBuffer::capacity() const {
  return BUFFER_SIZE;
}

void ReadingBuffer::clear() {
  head = 0;
  tail = 0;
  count = 0;
}