# PlatformIO Setup in VS Code

This guide explains how to set up and troubleshoot a PlatformIO project in Visual Studio Code for the Arduino module of the `g1t4` software engineering project.

The examples assume this repository layout:

```text
g1t4/
└── arduino/
    ├── include/
    ├── lib/
    ├── src/
    ├── test/
    └── platformio.ini
```

## 1. Important concept: project root

For PlatformIO, the **project root** is the folder that contains `platformio.ini`.

For this project, the PlatformIO root is:

```text
g1t4/arduino
```

Even if the Git repository root is:

```text
g1t4
```

PlatformIO should be opened from:

```text
g1t4/arduino
```

If VS Code is opened from the wrong folder, IntelliSense may not find files like:

```cpp
#include <Arduino.h>
#include <Wire.h>
#include <ArduinoBLE.h>
#include <Adafruit_BME680.h>
```

This usually produces errors such as:

```text
cannot open source file "Arduino.h"
cannot open source file "Wire.h"
#include errors detected. Please update your includePath.
```

These are often VS Code / IntelliSense indexing errors, not necessarily real compiler errors.

## 2. Correct `platformio.ini`

The file must start with a valid INI section header.

Correct:

```ini
[env:nano33ble]
platform = nordicnrf52
board = nano33ble
framework = arduino
monitor_speed = 9600

lib_deps =
    adafruit/Adafruit BME680 Library
    adafruit/Adafruit Unified Sensor
    arduino-libraries/ArduinoBLE
    seeed-studio/Grove - LCD RGB Backlight
```

Incorrect:

```ini
git[env:nano33ble]
```

That will cause an error like:

```text
File contains no section headers.
file: '.../platformio.ini', line: 1
'git[env:nano33ble]\n'
```

Fix it by changing the first line to:

```ini
[env:nano33ble]
```

## 3. Required VS Code extensions

Install these VS Code extensions:

1. **PlatformIO IDE**
2. **C/C++** by Microsoft

The Arduino extension is not required for a PlatformIO project. If it causes conflicts, disable the Arduino extension for this workspace.

PlatformIO IDE for VS Code includes PlatformIO Core internally, so a separate system-wide `pio` command is not required for normal VS Code usage.

## 4. Recommended workflow in VS Code

Open the correct folder:

```text
File → Open Folder → g1t4/arduino
```

Then run:

```text
PlatformIO: Build
```

If include errors remain, run:

```text
PlatformIO: Rebuild C/C++ Project Index
```

This rebuilds the IntelliSense index and fixes many false include errors.

Then reload VS Code:

```text
Developer: Reload Window
```

## 5. What “Rebuild C/C++ Project Index” means

`PlatformIO: Rebuild C/C++ Project Index` rebuilds the editor index used for:

- IntelliSense
- autocomplete
- include path resolution
- code navigation
- squiggle errors

It is not the same as compiling.

Use this mental model:

```text
PlatformIO: Build
→ checks whether the firmware actually compiles

PlatformIO: Rebuild C/C++ Project Index
→ fixes VS Code editor understanding / IntelliSense
```

If `Arduino.h` is missing in the editor but the project builds successfully, the issue is probably IntelliSense indexing.

## 6. Ubuntu / Linux setup

### 6.1 Install VS Code

Install Visual Studio Code from Microsoft or your package manager.

For Ubuntu, common options are:

```bash
sudo snap install code --classic
```

or install the `.deb` package from Microsoft.

### 6.2 Install required system packages

Install Python and common build dependencies:

```bash
sudo apt update
sudo apt install python3 python3-venv python3-pip git curl build-essential
```

### 6.3 Install VS Code extensions

Inside VS Code:

1. Open Extensions
2. Install **PlatformIO IDE**
3. Install **C/C++** by Microsoft

### 6.4 Open the project

Open:

```text
g1t4/arduino
```

not only:

```text
g1t4
```

and not:

```text
g1t4/arduino/src
```

### 6.5 Build and monitor

Use the PlatformIO sidebar or command palette:

```text
PlatformIO: Build
PlatformIO: Upload
PlatformIO: Monitor
```

If serial access fails on Linux, add your user to the serial/device groups:

```bash
sudo usermod -aG dialout $USER
sudo usermod -aG plugdev $USER
```

Then log out and log back in.

### 6.6 If `pio` is not available in the normal terminal

This is not necessarily a problem. PlatformIO IDE includes its own PlatformIO Core.

Use the VS Code UI:

```text
PlatformIO sidebar → Build / Upload / Monitor
```

or open the PlatformIO terminal from inside VS Code.

## 7. macOS setup

### 7.1 Install VS Code

Download and install Visual Studio Code for macOS.

### 7.2 Install dependencies

Install Xcode Command Line Tools:

```bash
xcode-select --install
```

Python 3 is usually available, but installing it through Homebrew is also fine:

```bash
brew install python git
```

### 7.3 Install VS Code extensions

Inside VS Code:

1. Install **PlatformIO IDE**
2. Install **C/C++** by Microsoft

### 7.4 Open the project

Open this folder:

```text
g1t4/arduino
```

### 7.5 Build and monitor

Use:

```text
PlatformIO: Build
PlatformIO: Upload
PlatformIO: Monitor
```

If the serial monitor cannot open the device, check that the board is connected and visible under `/dev/cu.*` or `/dev/tty.*`.

## 8. Windows setup

### 8.1 Install VS Code

Install Visual Studio Code for Windows.

### 8.2 Install Git

Install Git for Windows.

During installation, allow Git to be available from the command line if possible.

### 8.3 Install Python

Install Python 3 from python.org or the Microsoft Store.

When using the python.org installer, enable:

```text
Add Python to PATH
```

### 8.4 Install VS Code extensions

Inside VS Code:

1. Install **PlatformIO IDE**
2. Install **C/C++** by Microsoft

### 8.5 Open the project

Open:

```text
g1t4\arduino
```

Do not open only:

```text
g1t4
```

unless you know how to configure a VS Code multi-root workspace.

### 8.6 Build and monitor

Use the PlatformIO sidebar:

```text
Build
Upload
Monitor
```

If upload or monitor fails, check Device Manager and verify that the board appears as a COM port.

Example:

```text
COM3
COM4
COM5
```

If necessary, set the port in `platformio.ini`:

```ini
upload_port = COM3
monitor_port = COM3
```

Use the COM port that matches your system.

## 9. Common problems and fixes

### Problem: `cannot open source file "Arduino.h"`

Likely causes:

- VS Code opened the wrong folder
- `platformio.ini` has syntax errors
- PlatformIO index was not generated
- libraries/framework were not installed yet

Fix:

1. Open `g1t4/arduino`
2. Check that `platformio.ini` starts with `[env:nano33ble]`
3. Run `PlatformIO: Build`
4. Run `PlatformIO: Rebuild C/C++ Project Index`
5. Run `Developer: Reload Window`

### Problem: `File contains no section headers`

Cause:

```ini
git[env:nano33ble]
```

Fix:

```ini
[env:nano33ble]
```

### Problem: `pio` command not found

This is okay when using PlatformIO from VS Code.

Use:

```text
PlatformIO sidebar → Build / Upload / Monitor
```

PlatformIO IDE includes PlatformIO Core internally.

### Problem: libraries are not found

Check `lib_deps` in `platformio.ini`:

```ini
lib_deps =
    adafruit/Adafruit BME680 Library
    adafruit/Adafruit Unified Sensor
    arduino-libraries/ArduinoBLE
    seeed-studio/Grove - LCD RGB Backlight
```

Then run:

```text
PlatformIO: Build
PlatformIO: Rebuild C/C++ Project Index
```

### Problem: build works, but VS Code still shows red squiggles

This is usually IntelliSense, not the compiler.

Run:

```text
PlatformIO: Rebuild C/C++ Project Index
Developer: Reload Window
```

If it still happens, disable the Arduino extension for this workspace and let PlatformIO manage the project.

## 10. Recommended project checklist

Before coding, verify:

- [ ] VS Code opened `g1t4/arduino`
- [ ] `platformio.ini` starts with `[env:nano33ble]`
- [ ] PlatformIO extension is installed
- [ ] C/C++ extension is installed
- [ ] `PlatformIO: Build` succeeds
- [ ] `PlatformIO: Rebuild C/C++ Project Index` was run after dependency changes
- [ ] Serial Monitor baud rate matches `monitor_speed = 9600`

## 11. Notes for this Arduino module

The project uses:

```ini
platform = nordicnrf52
board = nano33ble
framework = arduino
```

This means PlatformIO will provide Arduino framework headers such as:

```cpp
#include <Arduino.h>
#include <Wire.h>
```

The project also uses BLE and BME680 dependencies:

```cpp
#include <ArduinoBLE.h>
#include <Adafruit_BME680.h>
```

These are installed through `lib_deps`.

## 12. Quick recovery sequence

If the project behaves strangely, use this sequence:

1. Close VS Code
2. Reopen VS Code
3. Open folder: `g1t4/arduino`
4. Check `platformio.ini`
5. Run `PlatformIO: Build`
6. Run `PlatformIO: Rebuild C/C++ Project Index`
7. Run `Developer: Reload Window`

This fixes most setup and IntelliSense problems.
