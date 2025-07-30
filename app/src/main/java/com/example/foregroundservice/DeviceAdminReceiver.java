package com.example.foregroundservice;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

public class DeviceAdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "DeviceAdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device admin enabled");
        sendAdminStatus(context, "enabled");
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "Device admin disabled");
        sendAdminStatus(context, "disabled");
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
        Log.d(TAG, "Password changed");
        sendAdminStatus(context, "password_changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        Log.d(TAG, "Password failed");
        sendAdminStatus(context, "password_failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        Log.d(TAG, "Password succeeded");
        sendAdminStatus(context, "password_succeeded");
    }

    private void sendAdminStatus(Context context, String status) {
        try {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            String userId = "default_user";
            JSONObject statusData = new JSONObject();
            statusData.put("type", "device_admin_status");
            statusData.put("status", status);
            statusData.put("timestamp", System.currentTimeMillis());
            database.child("admin_status").child(userId).setValue(statusData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending admin status", e);
        }
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, DeviceAdminReceiver.class);
    }
}