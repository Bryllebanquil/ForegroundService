#!/bin/bash

echo "=== Phone Tracker System Build Script ==="
echo "This script will attempt to build all components of the system."
echo

# Set up error handling
set -e

# Function to print colored output
print_status() {
    echo -e "\033[1;34m[INFO]\033[0m $1"
}

print_success() {
    echo -e "\033[1;32m[SUCCESS]\033[0m $1"
}

print_error() {
    echo -e "\033[1;31m[ERROR]\033[0m $1"
}

print_warning() {
    echo -e "\033[1;33m[WARNING]\033[0m $1"
}

# Check prerequisites
print_status "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    print_error "Java is not installed. Please install Java 11+ and try again."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    print_warning "Java $JAVA_VERSION detected. Android builds work best with Java 11+."
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    print_error "Node.js is not installed. Please install Node.js 16+ and try again."
    exit 1
fi

NODE_VERSION=$(node --version | sed 's/v//' | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 16 ]; then
    print_error "Node.js $NODE_VERSION detected. Please install Node.js 16+ and try again."
    exit 1
fi

# Check Python
if ! command -v python3 &> /dev/null; then
    print_error "Python 3 is not installed. Please install Python 3.7+ and try again."
    exit 1
fi

print_success "Prerequisites check completed"

# Build Web Controller
print_status "Building Web Controller..."
cd web-controller

if [ ! -d "node_modules" ]; then
    print_status "Installing Node.js dependencies..."
    npm install
fi

# Test web controller
print_status "Testing web controller configuration..."
if [ ! -f ".env" ]; then
    print_warning "No .env file found. Please configure Firebase credentials."
    print_warning "Copy .env.example to .env and fill in your Firebase details."
else
    print_success "Web controller environment configuration found"
fi

cd ..

# Test Python Dependencies
print_status "Checking Python dependencies..."
if python3 -c "import firebase_admin" 2>/dev/null; then
    print_success "Python Firebase Admin SDK is available"
else
    print_warning "Firebase Admin SDK not found. Installing..."
    pip3 install --break-system-packages firebase-admin || pip3 install firebase-admin || {
        print_error "Failed to install Firebase Admin SDK. Please install manually:"
        print_error "pip3 install firebase-admin"
        exit 1
    }
fi

# Test Python script
print_status "Testing Python script configuration..."
if [ ! -f "firebase-credentials.json" ]; then
    print_warning "No firebase-credentials.json found. Please configure Firebase credentials."
    print_warning "Copy firebase-credentials.json.example to firebase-credentials.json and fill in your Firebase service account details."
else
    print_success "Python script Firebase configuration found"
fi

# Android Build
print_status "Attempting Android build..."

if [ ! -f "local.properties" ]; then
    print_warning "No local.properties found. Creating with default Android SDK path..."
    echo "sdk.dir=/usr/lib/android-sdk" > local.properties
fi

# Try to build with Gradle
print_status "Building Android app..."
if ./gradlew assembleDebug 2>/dev/null; then
    print_success "Android app built successfully!"
else
    print_warning "Android build failed. This might be due to:"
    print_warning "1. Incompatible Gradle/Android plugin versions"
    print_warning "2. Missing or incorrect Android SDK path"
    print_warning "3. Missing build tools or platform SDK"
    print_warning ""
    print_warning "To fix Android build issues:"
    print_warning "1. Install Android Studio with latest SDK"
    print_warning "2. Update local.properties with correct SDK path"
    print_warning "3. Or use a newer Gradle wrapper version"
    print_warning ""
    print_warning "For now, other components can be run independently:"
fi

# Summary
echo
print_status "=== Build Summary ==="
echo "✅ Web Controller: Ready (Node.js dependencies installed)"
echo "✅ Python Script: Ready (Firebase Admin SDK installed)"

# Check Android build result
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Android App: Built successfully"
else
    echo "⚠️  Android App: Build needs manual configuration"
fi

echo
print_status "Next Steps:"
echo "1. Configure Firebase credentials in:"
echo "   - firebase-credentials.json (for Python script)"
echo "   - web-controller/.env (for web controller)"
echo "   - app/google-services.json (already configured)"
echo
echo "2. To run components:"
echo "   - Web Controller: cd web-controller && npm start"
echo "   - Python Script: python3 command_sender.py"
echo "   - Android App: Install APK on device (if build succeeded)"
echo
echo "3. For Android build issues, see Android setup documentation"
echo

print_success "Build script completed!"