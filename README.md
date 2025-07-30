# Phone Tracker System

A comprehensive phone tracking and monitoring system with Android app, web controller, and Python command sender.

## Project Structure

- `app/` - Android application (Kotlin/Java)
- `web-controller/` - Node.js web interface for monitoring
- `command_sender.py` - Python script for sending commands to devices

## Prerequisites

### For Android Development
- Android Studio
- Android SDK (API level 26+)
- Java 8 or higher

### For Web Controller
- Node.js 16+ and npm
- Firebase project with Realtime Database

### For Python Script
- Python 3.7+
- Firebase Admin SDK

## Setup Instructions

### 1. Android App Setup

1. **Configure Android SDK**
   ```bash
   # Copy and edit local.properties with your SDK path
   cp local.properties.example local.properties
   # Edit local.properties and set sdk.dir to your Android SDK path
   ```

2. **Firebase Configuration**
   - Create a Firebase project at https://console.firebase.google.com
   - Download `google-services.json` and place it in the `app/` directory
   - Enable Realtime Database in your Firebase project

3. **Build the App**
   ```bash
   ./gradlew build
   ```

### 2. Web Controller Setup

1. **Install Dependencies**
   ```bash
   cd web-controller
   npm install
   ```

2. **Configure Environment**
   ```bash
   # Copy the example environment file
   cp .env.example .env
   # Edit .env with your Firebase credentials
   ```

3. **Start the Server**
   ```bash
   npm start
   # or for development
   npm run dev
   ```

### 3. Python Command Sender Setup

1. **Install Dependencies**
   ```bash
   pip install firebase-admin
   ```

2. **Configure Firebase Credentials**
   ```bash
   # Copy the example credentials file
   cp firebase-credentials.json.example firebase-credentials.json
   # Edit firebase-credentials.json with your service account key
   ```

3. **Run the Command Sender**
   ```bash
   python command_sender.py
   ```

## Firebase Configuration

### Required Firebase Services
- **Realtime Database**: For real-time communication
- **Authentication**: For user management
- **Storage**: For file uploads

### Database Structure
```
/commands/{userId} - Commands sent to devices
/command_responses/{userId} - Responses from devices
/locations/{userId} - Device location data
/device_status/{userId} - Device status information
/audio_stream/{userId} - Audio stream data
/camera_stream/{userId} - Camera stream data
/screen_stream/{userId} - Screen capture data
/recordings/{userId} - Recorded files
```

## Features

### Android App
- Background location tracking
- Audio recording
- Camera streaming
- Screen capture
- File system access
- Command execution
- Firebase integration

### Web Controller
- Real-time device monitoring
- Location tracking visualization
- Command sending interface
- File management
- Stream monitoring

### Python Script
- Command sending interface
- Stream monitoring
- File operations
- Automated responses

## Security Notes

- Keep Firebase credentials secure
- Use environment variables for sensitive data
- Regularly update dependencies
- Monitor Firebase usage and costs

## Troubleshooting

### Common Issues

1. **Android SDK not found**
   - Ensure `local.properties` contains correct SDK path
   - Verify ANDROID_HOME environment variable

2. **Firebase connection errors**
   - Check credentials in `.env` and `firebase-credentials.json`
   - Verify Firebase project configuration

3. **Permission errors**
   - Ensure all required permissions are granted in Android app
   - Check Firebase security rules

## License

This project is for educational purposes only. Ensure compliance with local laws and regulations.