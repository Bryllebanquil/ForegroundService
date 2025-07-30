package com.example.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemEventReceiver";
    private FirebaseDatabase database;
    private String userId;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            Log.d(TAG, "Received system event: " + action);
            
            switch (action) {
                case Intent.ACTION_POWER_CONNECTED:
                    handlePowerConnected(context);
                    break;
                    
                case Intent.ACTION_POWER_DISCONNECTED:
                    handlePowerDisconnected(context);
                    break;
                    
                case Intent.ACTION_BATTERY_LOW:
                    handleBatteryLow(context);
                    break;
                    
                case Intent.ACTION_BATTERY_OKAY:
                    handleBatteryOkay(context);
                    break;
            }
        }
    }

    private void handlePowerConnected(Context context) {
        // Increase location update frequency when charging
        Log.d(TAG, "Power connected");
        updateBatteryStatus(context, true);
    }

    private void handlePowerDisconnected(Context context) {
        // Decrease location update frequency to save battery
        Log.d(TAG, "Power disconnected");
        updateBatteryStatus(context, false);
    }

    private void handleBatteryLow(Context context) {
        // Reduce location update frequency
        Log.d(TAG, "Battery low");
        updateBatteryStatus(context, false);
    }

    private void handleBatteryOkay(Context context) {
        // Restore normal location update frequency
        Log.d(TAG, "Battery okay");
        updateBatteryStatus(context, false);
    }
    
    private void updateBatteryStatus(Context context, boolean isCharging) {
        // Get battery information
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;
            
            // If isCharging wasn't provided in the event, check the battery status
            if (!isCharging) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL;
            }
            
            // Get power source if charging
            String powerSource = "";
            if (isCharging) {
                int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) {
                    powerSource = "USB";
                } else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) {
                    powerSource = "AC";
                } else if (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    powerSource = "Wireless";
                }
            }
            
            // Send to Firebase
            sendBatteryInfoToFirebase(batteryPct, isCharging, powerSource);
        }
    }
    
    private void sendBatteryInfoToFirebase(float batteryLevel, boolean isCharging, String powerSource) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference deviceStatusRef = FirebaseDatabase.getInstance()
                    .getReference("device_status")
                    .child(userId);
            
            Map<String, Object> batteryInfo = new HashMap<>();
            batteryInfo.put("batteryLevel", batteryLevel);
            batteryInfo.put("isCharging", isCharging);
            batteryInfo.put("powerSource", powerSource);
            batteryInfo.put("timestamp", System.currentTimeMillis());
            
            deviceStatusRef.setValue(batteryInfo)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Battery info updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating battery info", e));
        }
    }
}