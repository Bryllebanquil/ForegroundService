const express = require('express');
const admin = require('firebase-admin');
const dotenv = require('dotenv');
const path = require('path');
const http = require('http');
const socketIo = require('socket.io');

// Load environment variables
dotenv.config();

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// Initialize Firebase Admin
try {
    // Validate required environment variables
    const requiredEnvVars = [
        'FIREBASE_PROJECT_ID',
        'FIREBASE_CLIENT_EMAIL',
        'FIREBASE_PRIVATE_KEY',
        'FIREBASE_DATABASE_URL'
    ];
    
    const missingVars = requiredEnvVars.filter(varName => !process.env[varName]);
    if (missingVars.length > 0) {
        throw new Error(`Missing required environment variables: ${missingVars.join(', ')}. Please check your .env file.`);
    }
    
    // Initialize Firebase with proper error handling
    admin.initializeApp({
        credential: admin.credential.cert({
            projectId: process.env.FIREBASE_PROJECT_ID,
            clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
            privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n')
        }),
        databaseURL: process.env.FIREBASE_DATABASE_URL
    });
    console.log('Firebase initialized successfully');
} catch (error) {
    console.error('Error initializing Firebase:', error);
    console.error('Please check your .env file and Firebase credentials.');
    console.error('If running in development, make sure you have copied .env.example to .env and filled in the correct values.');
    process.exit(1);
}

const db = admin.database();

// Middleware
app.use(express.json());
app.use(express.static('public'));

// Routes
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// API Routes
app.get('/api/devices', async (req, res) => {
    try {
        const locationsRef = db.ref('locations');
        const locationSnapshot = await locationsRef.once('value');
        const deviceStatusRef = db.ref('device_status');
        const statusSnapshot = await deviceStatusRef.once('value');
        
        const devices = [];

        // Process location data
        locationSnapshot.forEach(childSnapshot => {
            const userId = childSnapshot.key;
            const lastLocation = childSnapshot.val();
            const lastLocationKey = Object.keys(lastLocation).pop();
            const locationData = lastLocation[lastLocationKey];
            
            // Add formatted date time to location data
            if (locationData && locationData.timestamp) {
                const date = new Date(locationData.timestamp);
                locationData.formattedDateTime = date.toLocaleString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });
            }

            // Get device status if available
            let deviceStatus = null;
            if (statusSnapshot.hasChild(userId)) {
                deviceStatus = statusSnapshot.child(userId).val();
                
                // Add formatted date time to status data
                if (deviceStatus && deviceStatus.timestamp) {
                    const date = new Date(deviceStatus.timestamp);
                    deviceStatus.formattedDateTime = date.toLocaleString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit'
                    });
                }
            }

            devices.push({
                userId,
                lastLocation: locationData,
                deviceStatus: deviceStatus
            });
        });

        res.json(devices);
    } catch (error) {
        console.error('Error fetching devices:', error);
        res.status(500).json({ error: 'Failed to fetch devices' });
    }
});

app.get('/api/recordings/:userId', async (req, res) => {
    try {
        const recordingsRef = db.ref(`recordings/${req.params.userId}`);
        const snapshot = await recordingsRef.once('value');
        const recordings = snapshot.val() || {};
        res.json(recordings);
    } catch (error) {
        console.error('Error fetching recordings:', error);
        res.status(500).json({ error: 'Failed to fetch recordings' });
    }
});

// Socket.IO for real-time updates
io.on('connection', (socket) => {
    console.log('Client connected');
    
    // Listen for device status updates
    const deviceStatusRef = db.ref('device_status');
    deviceStatusRef.on('child_changed', (snapshot) => {
        const userId = snapshot.key;
        const statusData = snapshot.val();
        
        // Add formatted date time
        if (statusData && statusData.timestamp) {
            const date = new Date(statusData.timestamp);
            statusData.formattedDateTime = date.toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        }
        
        socket.emit('deviceStatusUpdate', {
            userId,
            status: statusData
        });
    });

    // Listen for command requests
    socket.on('sendCommand', async (data) => {
        try {
            const { userId, command } = data;
            await db.ref(`commands/${userId}`).push({
                command,
                timestamp: admin.database.ServerValue.TIMESTAMP
            });
            socket.emit('commandSent', { success: true });
        } catch (error) {
            console.error('Error sending command:', error);
            socket.emit('commandSent', { success: false, error: error.message });
        }
    });

    // Subscribe to location updates for a specific device
    socket.on('subscribeToDevice', (userId) => {
        const locationRef = db.ref(`locations/${userId}`);
        locationRef.on('child_added', (snapshot) => {
            const locationData = snapshot.val();
            
            // Add formatted date time to location data
            if (locationData && locationData.timestamp) {
                const date = new Date(locationData.timestamp);
                locationData.formattedDateTime = date.toLocaleString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });
            }
            
            socket.emit('locationUpdate', {
                userId,
                location: locationData
            });
        });
    });
    
    // Subscribe to all devices
    socket.on('subscribeToDevices', async () => {
        try {
            const locationsRef = db.ref('locations');
            const snapshot = await locationsRef.once('value');
            
            snapshot.forEach(userSnapshot => {
                const userId = userSnapshot.key;
                const locationRef = db.ref(`locations/${userId}`);
                
                // Listen for new locations for this user
                locationRef.on('child_added', (snapshot) => {
                    const locationData = snapshot.val();
                    
                    // Add formatted date time to location data
                    if (locationData && locationData.timestamp) {
                        const date = new Date(locationData.timestamp);
                        locationData.formattedDateTime = date.toLocaleString('en-US', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit'
                        });
                    }
                    
                    socket.emit('locationUpdate', {
                        userId,
                        location: locationData
                    });
                });
            });
        } catch (error) {
            console.error('Error subscribing to all devices:', error);
        }
    });

    socket.on('disconnect', () => {
        console.log('Client disconnected');
    });
});

// Start server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});