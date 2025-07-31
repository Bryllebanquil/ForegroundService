package com.example.foregroundservice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONObject;
import android.os.Handler;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private DatabaseReference commandsRef;
    private ChildEventListener commandListener;
    private String userId;
    private Handler handler = new Handler();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
        
        // Initialize Firebase connection
        initializeFirebaseListener();
    }

    private void initializeFirebaseListener() {
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                commandsRef = FirebaseDatabase.getInstance().getReference("accessibility_commands").child(userId);
                
                commandListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        handleCommand(dataSnapshot);
                    }
                    
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}
                    
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}
                    
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}
                    
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Firebase listener cancelled", databaseError.toException());
                    }
                };
                
                commandsRef.addChildEventListener(commandListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase listener", e);
        }
    }

    private void handleCommand(DataSnapshot dataSnapshot) {
        try {
            String commandJson = dataSnapshot.getValue(String.class);
            if (commandJson != null) {
                JSONObject command = new JSONObject(commandJson);
                String action = command.getString("action");
                
                switch (action) {
                    case "click":
                        performClick(command);
                        break;
                    case "swipe":
                        performSwipe(command);
                        break;
                    case "back":
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        break;
                    case "home":
                        performGlobalAction(GLOBAL_ACTION_HOME);
                        break;
                    case "recents":
                        performGlobalAction(GLOBAL_ACTION_RECENTS);
                        break;
                    case "notifications":
                        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                        break;
                    default:
                        Log.w(TAG, "Unknown accessibility command: " + action);
                }
                
                // Remove processed command
                dataSnapshot.getRef().removeValue();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling accessibility command", e);
        }
    }

    private void performClick(JSONObject command) {
        try {
            JSONObject params = command.getJSONObject("params");
            float x = (float) params.getDouble("x");
            float y = (float) params.getDouble("y");
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Path clickPath = new Path();
                clickPath.moveTo(x, y);
                
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 100));
                
                dispatchGesture(gestureBuilder.build(), null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing click", e);
        }
    }

    private void performSwipe(JSONObject command) {
        try {
            JSONObject params = command.getJSONObject("params");
            float startX = (float) params.getDouble("startX");
            float startY = (float) params.getDouble("startY");
            float endX = (float) params.getDouble("endX");
            float endY = (float) params.getDouble("endY");
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Path swipePath = new Path();
                swipePath.moveTo(startX, startY);
                swipePath.lineTo(endX, endY);
                
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 500));
                
                dispatchGesture(gestureBuilder.build(), null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing swipe", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log accessibility events for debugging
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Window changed: " + event.getPackageName());
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commandsRef != null && commandListener != null) {
            commandsRef.removeEventListener(commandListener);
        }
        Log.d(TAG, "Accessibility service destroyed");
    }
}