# Project Setup Instructions

## Fixed Issues

The following issues have been resolved in your codebase:

1. ✅ **Android SDK Configuration** - Created `local.properties` with default Android SDK path
2. ✅ **Missing XML Configuration Files** - Created:
   - `app/src/main/res/xml/accessibility_service_config.xml`
   - `app/src/main/res/xml/device_admin.xml`
3. ✅ **Missing Java Classes** - Created:
   - `AccessibilityService.java`
   - `DeviceAdminReceiver.java`
4. ✅ **Missing String Resources** - Added accessibility service description to `strings.xml`
5. ✅ **Web Controller Environment** - Created `.env` file template
6. ✅ **Firebase Configuration** - Created `firebase-credentials.json` template
7. ✅ **Node.js Dependencies** - All packages installed successfully
8. ✅ **Python Script Syntax** - No syntax errors found

## Configuration Required

To complete the setup, you need to configure the following:

### 1. Android SDK Setup

If the default SDK path doesn't work, edit `local.properties`:

```properties
# Update this path to match your Android SDK installation
sdk.dir=/path/to/your/android/sdk
```

Common SDK locations:
- Ubuntu/Debian: `/usr/lib/android-sdk` or `/opt/android-sdk`
- Manual installation: `/home/username/Android/Sdk`

### 2. Firebase Configuration

#### For the Android App:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Add an Android app with package name: `com.example.foregroundservice`
4. Download `google-services.json` and replace the existing one in `app/`

#### For the Web Controller:
1. In Firebase Console, go to Project Settings > Service Accounts
2. Generate a new private key for "Firebase Admin SDK"
3. Download the JSON file and replace `firebase-credentials.json`
4. Update `web-controller/.env` with your Firebase details:

```env
FIREBASE_PROJECT_ID=your-actual-project-id
FIREBASE_CLIENT_EMAIL=your-service-account@your-project.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nYour-actual-private-key\n-----END PRIVATE KEY-----
FIREBASE_DATABASE_URL=https://your-project-default-rtdb.firebaseio.com
```

5. Update `command_sender.py` Firebase URL (line 13):
```python
'databaseURL': 'https://your-project-default-rtdb.firebaseio.com'
```

### 3. Database Setup

In Firebase Console:
1. Go to Realtime Database
2. Create a database in test mode
3. Set up the following structure:
```json
{
  "commands": {},
  "command_responses": {},
  "locations": {},
  "accessibility_commands": {},
  "screen_capture_permission": {}
}
```

## Building and Running

### Android App:
```bash
./gradlew :app:assembleDebug
```

### Web Controller:
```bash
cd web-controller
npm start
```

### Python Command Sender:
```bash
python3 command_sender.py
```

## Security Notes

This application requests extensive permissions for remote device monitoring. Please ensure:

1. **Legal Compliance**: Only use on devices you own or have explicit permission to monitor
2. **Security**: Use strong Firebase security rules in production
3. **Privacy**: Implement proper data encryption for sensitive operations
4. **Network**: Use secure connections (HTTPS/WSS) in production

## Common Issues

1. **Build fails**: Check Android SDK path in `local.properties`
2. **Firebase errors**: Verify credentials and database URLs are correct
3. **Permission denied**: Some features require root access or system-level permissions
4. **Service not starting**: Check device admin and accessibility permissions are enabled

## Development vs Production

- The current configuration is for development/testing
- For production, implement proper authentication, encryption, and security measures
- Update Firebase security rules to restrict access appropriately