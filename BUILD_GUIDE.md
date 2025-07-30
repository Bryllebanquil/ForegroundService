# Phone Tracker System - Build Guide

This guide helps you build and configure all components of the Phone Tracker System.

## Quick Start

Run the automated build script:
```bash
./build-all.sh
```

## Manual Build Process

### Prerequisites

1. **Java 11+** for Android builds
2. **Node.js 16+** for web controller
3. **Python 3.7+** for command sender
4. **Android SDK** (if building Android app)

### 1. Web Controller Setup

```bash
cd web-controller
npm install
cp .env.example .env
# Edit .env with your Firebase credentials
npm start
```

### 2. Python Script Setup

```bash
pip3 install firebase-admin
cp firebase-credentials.json.example firebase-credentials.json
# Edit firebase-credentials.json with your Firebase service account
python3 command_sender.py
```

### 3. Android App Setup

```bash
# Create local.properties with your Android SDK path
echo "sdk.dir=/path/to/your/android/sdk" > local.properties

# Build the app
./gradlew assembleDebug
```

## Configuration Required

### Firebase Setup

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create new project or select existing

2. **Configure Android App**
   - Add Android app to Firebase project
   - Package name: `com.example.foregroundservice`
   - Download `google-services.json` to `app/` directory

3. **Create Service Account**
   - Go to Project Settings > Service Accounts
   - Generate new private key
   - Download JSON file as `firebase-credentials.json`

4. **Set Up Database**
   - Enable Realtime Database
   - Create database structure:
   ```json
   {
     "commands": {},
     "command_responses": {},
     "locations": {},
     "accessibility_commands": {},
     "screen_capture_permission": {}
   }
   ```

### Environment Configuration

#### Web Controller (.env)
```env
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CLIENT_EMAIL=your-service-account@project.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nkey-content\n-----END PRIVATE KEY-----
FIREBASE_DATABASE_URL=https://your-project-default-rtdb.firebaseio.com
PORT=3000
```

#### Python Script (firebase-credentials.json)
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\nkey-content\n-----END PRIVATE KEY-----\n",
  "client_email": "service-account@project.iam.gserviceaccount.com",
  "client_id": "client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/service-account%40project.iam.gserviceaccount.com"
}
```

## Troubleshooting

### Android Build Issues

#### Problem: Gradle Plugin Version Incompatibility
```
Plugin [id: 'com.android.application', version: 'X.X.X'] was not found
```

**Solutions:**
1. **Update Gradle Wrapper** (Recommended):
   ```bash
   ./gradlew wrapper --gradle-version=8.0
   ```

2. **Use Android Studio**:
   - Import project in Android Studio
   - Let it update Gradle automatically
   - Build from IDE

3. **Manual Version Downgrade**:
   Edit `build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.android.application") version "7.4.2" apply false
       id("org.jetbrains.kotlin.android") version "1.8.0" apply false
   }
   ```

#### Problem: Android SDK Not Found
```
SDK location not found
```

**Solutions:**
1. Install Android Studio with SDK
2. Update `local.properties`:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
3. Common SDK locations:
   - Linux: `/usr/lib/android-sdk`
   - macOS: `/Users/username/Library/Android/sdk`
   - Windows: `C:\Users\username\AppData\Local\Android\Sdk`

#### Problem: Missing Build Tools
```
Could not find build tools revision X.X.X
```

**Solutions:**
1. Install build tools via Android Studio SDK Manager
2. Or via command line:
   ```bash
   sdkmanager "build-tools;34.0.0"
   ```

### Web Controller Issues

#### Problem: Firebase Connection Error
```
Error: Could not authenticate
```

**Solutions:**
1. Verify `.env` file configuration
2. Check Firebase service account permissions
3. Ensure database URL is correct

#### Problem: Port Already in Use
```
Error: listen EADDRINUSE :::3000
```

**Solutions:**
1. Change PORT in `.env` file
2. Kill existing process:
   ```bash
   kill $(lsof -t -i:3000)
   ```

### Python Script Issues

#### Problem: Firebase Module Not Found
```
ModuleNotFoundError: No module named 'firebase_admin'
```

**Solutions:**
1. Install with pip:
   ```bash
   pip3 install firebase-admin
   ```
2. If externally managed environment:
   ```bash
   pip3 install --break-system-packages firebase-admin
   ```

#### Problem: Firebase Authentication Error
```
Error: Could not load service account key
```

**Solutions:**
1. Verify `firebase-credentials.json` exists
2. Check JSON file format and permissions
3. Ensure service account has correct roles

## Running the System

### 1. Start Web Controller
```bash
cd web-controller
npm start
# Access at http://localhost:3000
```

### 2. Run Python Script
```bash
python3 command_sender.py
```

### 3. Install Android App
```bash
# If build succeeded, install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Development Environment

### Recommended Setup
1. **IDE**: Android Studio for Android, VS Code for web/Python
2. **Testing**: Use Android emulator or physical device
3. **Debugging**: Enable Firebase debugging logs
4. **Version Control**: Use git with proper .gitignore

### Security Notes
- Never commit Firebase credentials to git
- Use environment variables for sensitive data
- Implement proper Firebase security rules
- Regular dependency updates

## Getting Help

1. Check this troubleshooting guide
2. Review Firebase console for errors
3. Check application logs
4. Verify all prerequisites are installed

## Current System Status

✅ **Web Controller**: Fully configured and ready
✅ **Python Script**: Fully configured and ready  
✅ **Configuration Files**: All templates created
⚠️  **Android App**: May need Gradle version update for seamless builds

The system is ready to use with proper Firebase configuration!