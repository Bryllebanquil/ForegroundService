package com.example.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
            intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            
            Log.d(TAG, "Received boot completed intent");
            
            // Check if service was previously started
            SharedPreferences prefs = context.getSharedPreferences("ServicePrefs", Context.MODE_PRIVATE);
            boolean wasServiceStarted = prefs.getBoolean("service_started", false);
            
            if (wasServiceStarted && checkPermissions(context)) {
                Log.d(TAG, "Starting service after boot");
                
                Intent serviceIntent = new Intent(context, MyForegroundService.class);
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    serviceIntent.putExtra("user_id", auth.getCurrentUser().getUid());
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }

    private boolean checkPermissions(Context context) {
        boolean basePermissions = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
               
        // Check Bluetooth permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return basePermissions && 
                   ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return basePermissions;
        }
    }
}