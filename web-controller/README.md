# Phone Tracker Control Panel

A web-based control panel for managing multiple phone tracker devices.

## Features

- Real-time device location tracking on a map
- Remote microphone control
- Recording management
- Multiple device support
- Real-time updates using Socket.IO

## Setup

1. Install dependencies:
```bash
npm install
```

2. Set up Firebase:
   - Go to Firebase Console
   - Create a new project or use existing one
   - Go to Project Settings > Service Accounts
   - Generate new private key
   - Save the JSON file

3. Configure environment variables:
   - Copy `.env.example` to `.env`
   - Fill in Firebase configuration from the JSON file:
     ```
     FIREBASE_PROJECT_ID=your-project-id
     FIREBASE_CLIENT_EMAIL=your-client-email
     FIREBASE_PRIVATE_KEY=your-private-key
     FIREBASE_DATABASE_URL=https://your-project-id.firebaseio.com
     ```

4. Start the server:
```bash
npm start
```

## Deployment to Railway

1. Create a new Railway project:
   - Go to [Railway](https://railway.app/)
   - Create a new project
   - Connect your GitHub repository

2. Add environment variables:
   - In Railway dashboard, go to Variables
   - Add all variables from `.env.example`
   - Make sure to properly escape the Firebase private key

3. Deploy:
   - Railway will automatically deploy when you push to your repository
   - You can also trigger manual deployments from the dashboard

## Firebase Database Structure

```
/locations/{userId}/{locationId}
  - latitude: number
  - longitude: number
  - accuracy: number
  - timestamp: number

/recordings/{userId}/{recordingId}
  - url: string
  - timestamp: number
  - duration: string

/commands/{userId}/{commandId}
  - command: string
  - timestamp: number
```

## Android App Integration

The Android app needs to:

1. Listen for commands:
```java
DatabaseReference commandsRef = FirebaseDatabase.getInstance()
    .getReference("commands")
    .child(userId);

commandsRef.addChildEventListener(new ChildEventListener() {
    @Override
    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
        String command = snapshot.child("command").getValue(String.class);
        handleCommand(command);
    }
    // ... other methods ...
});
```

2. Update location:
```java
DatabaseReference locationsRef = FirebaseDatabase.getInstance()
    .getReference("locations")
    .child(userId);

locationsRef.push().setValue(locationData);
```

3. Upload recordings:
```java
StorageReference recordingRef = FirebaseStorage.getInstance()
    .getReference()
    .child("recordings")
    .child(userId)
    .child("recording_" + timestamp + ".mp3");

recordingRef.putFile(recordingFile);
```

## Security Rules

Add these rules to your Firebase Realtime Database:

```json
{
  "rules": {
    "locations": {
      "$userId": {
        ".read": true,
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    "recordings": {
      "$userId": {
        ".read": true,
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    "commands": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": true
      }
    }
  }
}
```

## License

MIT 