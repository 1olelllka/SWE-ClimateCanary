#include <Arduino.h>
#include <Wire.h>
#include <ArduinoBLE.h>
#include "rgb_lcd.h"

// Grove LCD
rgb_lcd lcd;

// Service
BLEService testService("19B10000-E8F2-537E-4F6C-D104768A1214");

// Arduino -> Raspberry Pi
BLEStringCharacteristic txCharacteristic(
  "19B10001-E8F2-537E-4F6C-D104768A1214",
  BLERead | BLENotify,
  64
);

// Raspberry Pi -> Arduino
BLEStringCharacteristic rxCharacteristic(
  "19B10002-E8F2-537E-4F6C-D104768A1214",
  BLEWriteWithoutResponse,
  64
);

void onRxWritten(BLEDevice central, BLECharacteristic characteristic);

void setup() {
  Serial.begin(9600);
  while (!Serial);

  // LCD init
  lcd.begin(16, 2);
  lcd.setRGB(0, 128, 255);
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Starting...");
  lcd.setCursor(0, 1);
  lcd.print("BLE init");

  if (!BLE.begin()) {
    Serial.println("BLE failed");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("BLE failed");
    while (1);
  }

  BLE.setLocalName("SensorStation");
  BLE.setDeviceName("SensorStation");
  BLE.setAdvertisedService(testService);

  txCharacteristic.writeValue("Hello from Arduino");
  rxCharacteristic.setEventHandler(BLEWritten, onRxWritten);

  testService.addCharacteristic(txCharacteristic);
  testService.addCharacteristic(rxCharacteristic);
  BLE.addService(testService);

  BLE.advertise();

  Serial.println("BLE ready, waiting for Pi...");
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Waiting for Pi");
  lcd.setCursor(0, 1);
  lcd.print("SensorStation");
}

void loop() {
  BLEDevice central = BLE.central();

  if (central) {
    Serial.print("Connected to: ");
    Serial.println(central.address());

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Connected");
    lcd.setCursor(0, 1);
    lcd.print("Pi joined");

    unsigned long lastSend = 0;

    while (central.connected()) {
      BLE.poll();

      if (millis() - lastSend > 10000) {
        lastSend = millis();

        String msg = "Hello from Arduino, time: " + String(millis());
        txCharacteristic.writeValue(msg);

        Serial.print("Sent to Pi: ");
        Serial.println(msg);
      }
    }

    Serial.print("Disconnected from: ");
    Serial.println(central.address());

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Disconnected");
    lcd.setCursor(0, 1);
    lcd.print("Waiting...");
  }
}

void onRxWritten(BLEDevice central, BLECharacteristic characteristic) {
  String received = rxCharacteristic.value();

  Serial.print("Received from Pi (");
  Serial.print(central.address());
  Serial.print("): ");
  Serial.println(received);

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("From Pi:");

  // 16x2 LCD: first 16 chars on line 2
  lcd.setCursor(0, 1);
  if (received.length() <= 16) {
    lcd.print(received);
  } else {
    lcd.print(received.substring(0, 16));
  }
}