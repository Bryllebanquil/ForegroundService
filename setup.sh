#!/bin/bash

echo "Phone Tracker System Setup"
echo "=========================="

# Check if running on Linux/macOS
if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected Linux/macOS system"
    
    # Setup Android SDK path
    if [ ! -f "local.properties" ]; then
        echo "Setting up Android SDK configuration..."
        echo "Please enter your Android SDK path:"
        echo "Common locations:"
        echo "  Linux: /home/YourUsername/Android/Sdk"
        echo "  macOS: /Users/YourUsername/Library/Android/sdk"
        read -p "SDK path: " sdk_path
        
        if [ ! -z "$sdk_path" ]; then
            echo "sdk.dir=$sdk_path" > local.properties
            echo "Android SDK configured!"
        else
            echo "Using example configuration. Please edit local.properties manually."
            cp local.properties.example local.properties
        fi
    else
        echo "local.properties already exists"
    fi
    
    # Setup Firebase credentials for Python
    if [ ! -f "firebase-credentials.json" ]; then
        echo "Setting up Firebase credentials for Python script..."
        if [ -f "firebase-credentials.json.example" ]; then
            cp firebase-credentials.json.example firebase-credentials.json
            echo "Please edit firebase-credentials.json with your Firebase service account key"
        fi
    else
        echo "firebase-credentials.json already exists"
    fi
    
    # Setup web controller environment
    if [ ! -f "web-controller/.env" ]; then
        echo "Setting up web controller environment..."
        if [ -f "web-controller/.env.example" ]; then
            cp web-controller/.env.example web-controller/.env
            echo "Please edit web-controller/.env with your Firebase credentials"
        fi
    else
        echo "web-controller/.env already exists"
    fi
    
    # Install web controller dependencies
    echo "Installing web controller dependencies..."
    cd web-controller
    npm install
    cd ..
    
    echo ""
    echo "Setup complete! Please:"
    echo "1. Edit local.properties with your Android SDK path"
    echo "2. Edit firebase-credentials.json with your Firebase service account key"
    echo "3. Edit web-controller/.env with your Firebase project details"
    echo "4. Place google-services.json in the app/ directory"
    echo ""
    echo "Then run: ./gradlew build"
    
else
    echo "This setup script is for Linux/macOS systems."
    echo "For Windows, please manually:"
    echo "1. Copy local.properties.example to local.properties and edit"
    echo "2. Copy firebase-credentials.json.example to firebase-credentials.json and edit"
    echo "3. Copy web-controller/.env.example to web-controller/.env and edit"
    echo "4. Run 'npm install' in web-controller directory"
fi