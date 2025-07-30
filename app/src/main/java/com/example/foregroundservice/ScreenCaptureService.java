package com.example.foregroundservice;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "ScreenCapture";
    private static final int NOTIFICATION_ID = 1;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FRAME_RATE = 30;
    private static final int BITRATE = 6000000;
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    
    private DatabaseReference commandsRef;
    private DatabaseReference screenDataRef;
    private StorageReference storageRef;
    private String userId;
    private MediaCodec.BufferInfo bufferInfo;
    private AtomicBoolean isCapturing;
    private Handler mainHandler;
    private long lastFrameTime;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionManager projectionManager;
    private Surface inputSurface;
    private MediaCodec encoder;

    // Add missing fields for configurationSPS and configurationPPS
    private byte[] configurationSPS;
    private byte[] configurationPPS;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        bufferInfo = new MediaCodec.BufferInfo();
        isCapturing = new AtomicBoolean(false);
        mainHandler = new Handler(Looper.getMainLooper());
        
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference().child("screen_captures");
        
        setupFirebaseListeners();
    }
    
    private void setupFirebaseListeners() {
        userId = getSharedPreferences("ServicePrefs", MODE_PRIVATE).getString("user_id", null);
        if (userId == null) {
            Log.e(TAG, "User ID not found");
            stopSelf();
            return;
        }

        commandsRef = FirebaseDatabase.getInstance().getReference("commands").child(userId);
        screenDataRef = FirebaseDatabase.getInstance().getReference("screen_data").child(userId);

        commandsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                
                String command = snapshot.child("command").getValue(String.class);
                if (command == null) return;

                handleRemoteCommand(command);
                // Clear the command after processing
                commandsRef.removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Command listener cancelled", error.toException());
            }
        });
    }

    private void handleRemoteCommand(String command) {
        Log.d(TAG, "Received command: " + command);
        switch (command) {
            case "START_SCREEN":
                // Screen capture already running
                break;
            case "STOP_SCREEN":
                stopSelf();
                break;
            case "CHANGE_QUALITY":
                // Implement quality change if needed
                break;
            default:
                Log.w(TAG, "Unknown command: " + command);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Screen Capture Service",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Active")
            .setContentText("Your screen is being shared")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent data = intent.getParcelableExtra("data");
        startCapture(resultCode, data);
        return START_STICKY;
    }

    private void startCapture(int resultCode, Intent data) {
        try {
            Log.d(TAG, "Starting screen capture");
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection failed to initialize");
                return;
            }
            setupMediaCodec();
            createVirtualDisplay();
            Log.d(TAG, "Screen capture started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen capture", e);
        }
    }

    private void setupMediaCodec() {
        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41);
            
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = encoder.createInputSurface();
            encoder.start();
            
            startEncodingThread();
        } catch (IOException e) {
            Log.e(TAG, "Failed to setup media codec", e);
            stopSelf();
        }
    }
    
    private long calculateFrameDelay() {
        long currentTime = System.nanoTime();
        long delay = 0;
        if (lastFrameTime > 0) {
            long targetFrameTime = 1000000000L / FRAME_RATE; // Convert frame rate to nanoseconds
            long actualFrameTime = currentTime - lastFrameTime;
            delay = Math.max(0, targetFrameTime - actualFrameTime);
        }
        lastFrameTime = currentTime;
        return delay / 1000000L; // Convert to milliseconds
    }

    private void startEncodingThread() {
        Thread encodingThread = new Thread(() -> {
            isCapturing.set(true);
            ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
            
            while (isCapturing.get() && !Thread.interrupted()) {
                try {
                    int outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                    if (outputBufferId >= 0) {
                        ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferId);
                        if (outputBuffer != null) {
                            byte[] data = new byte[bufferInfo.size];
                            outputBuffer.get(data);
                            
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // Store codec configuration for later use
                                frameBuffer.write(data);
                            } else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                                // Key frame - send previous buffer and start new one
                                if (frameBuffer.size() > 0) {
                                    uploadFrame(frameBuffer.toByteArray());
                                    frameBuffer.reset();
                                }
                                frameBuffer.write(data);
                            } else {
                                // Regular frame - add to buffer
                                frameBuffer.write(data);
                            }
                            
                            encoder.releaseOutputBuffer(outputBufferId, false);
                            
                            // Control frame rate
                            Thread.sleep(calculateFrameDelay());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in encoding thread", e);
                    break;
                }
            }
            
            // Clean up
            try {
                frameBuffer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing frame buffer", e);
            }
        });
        
        encodingThread.start();
    }
    
    private void uploadFrame(byte[] frameData) {
        if (userId == null || frameData.length == 0) return;
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        StorageReference frameRef = storageRef.child(userId).child(timestamp + ".h264");
        
        frameRef.putBytes(frameData)
            .addOnSuccessListener(taskSnapshot -> {
                frameRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Update Firebase database with frame URL
                    screenDataRef.child("latest_frame").setValue(uri.toString());
                    screenDataRef.child("timestamp").setValue(timestamp);
                });
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error uploading frame", e));
    }

    private void handleConfigurationFrame(byte[] data) {
        // Parse SPS and PPS from configuration frame
        int spsStart = 4; // Skip NAL header
        int spsLength = 0;
        int ppsStart = 0;
        int ppsLength = 0;

        // Find SPS length
        for (int i = spsStart + 1; i < data.length - 4; i++) {
            if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                spsLength = i - spsStart;
                ppsStart = i + 4;
                ppsLength = data.length - ppsStart;
                break;
            }
        }

        if (spsLength > 0 && ppsLength > 0) {
            configurationSPS = new byte[spsLength];
            configurationPPS = new byte[ppsLength];
            System.arraycopy(data, spsStart, configurationSPS, 0, spsLength);
            System.arraycopy(data, ppsStart, configurationPPS, 0, ppsLength);
        }
    }

    private void sendEncodedFrame(byte[] frameData) {
        if (screenDataRef == null) return;

        try {
            screenDataRef.child("frame").setValue(frameData);
            screenDataRef.child("timestamp").setValue(System.currentTimeMillis());
            if (configurationSPS != null && configurationPPS != null) {
                screenDataRef.child("sps").setValue(configurationSPS);
                screenDataRef.child("pps").setValue(configurationPPS);
                configurationSPS = null; // Only send once
                configurationPPS = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending frame", e);
        }
    }

    private void createVirtualDisplay() {
        try {
            Log.d(TAG, "Creating virtual display");
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                WIDTH, HEIGHT, 
                getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null);
            
            if (virtualDisplay == null) {
                Log.e(TAG, "Virtual display is null");
                throw new RuntimeException("Failed to create Virtual Display");
            }
            Log.d(TAG, "Virtual display created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating virtual display", e);
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying ScreenCaptureService");
        
        // Stop encoding thread
        isCapturing.set(false);
        
        // Clean up resources on main thread
        mainHandler.post(() -> {
            try {
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
                if (mediaProjection != null) {
                    mediaProjection.stop();
                    mediaProjection = null;
                }
                if (encoder != null) {
                    encoder.stop();
                    encoder.release();
                    encoder = null;
                }
                if (inputSurface != null) {
                    inputSurface.release();
                    inputSurface = null;
                }
                
                // Remove Firebase listeners
                if (commandsRef != null) {
                    commandsRef.removeEventListener(commandsListener);
                }
                
                // Update service status
                if (screenDataRef != null) {
                    screenDataRef.child("status").setValue("stopped");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during service cleanup", e);
            }
        });
        
        super.onDestroy();
    }
    
    private final ValueEventListener commandsListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (!snapshot.exists()) return;
            
            String command = snapshot.child("command").getValue(String.class);
            if (command == null) return;
            
            handleRemoteCommand(command);
            commandsRef.removeValue();
        }
        
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e(TAG, "Command listener cancelled", error.toException());
        }
    };
}
