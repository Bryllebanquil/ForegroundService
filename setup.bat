@echo off
echo Phone Tracker System Setup
echo ==========================

echo Setting up project configuration...

REM Setup Android SDK path
if not exist "local.properties" (
    echo Setting up Android SDK configuration...
    echo Please edit local.properties with your Android SDK path
    copy local.properties.example local.properties
) else (
    echo local.properties already exists
)

REM Setup Firebase credentials for Python
if not exist "firebase-credentials.json" (
    echo Setting up Firebase credentials for Python script...
    if exist "firebase-credentials.json.example" (
        copy firebase-credentials.json.example firebase-credentials.json
        echo Please edit firebase-credentials.json with your Firebase service account key
    )
) else (
    echo firebase-credentials.json already exists
)

REM Setup web controller environment
if not exist "web-controller\.env" (
    echo Setting up web controller environment...
    if exist "web-controller\.env.example" (
        copy web-controller\.env.example web-controller\.env
        echo Please edit web-controller\.env with your Firebase credentials
    )
) else (
    echo web-controller\.env already exists
)

REM Install web controller dependencies
echo Installing web controller dependencies...
cd web-controller
call npm install
cd ..

echo.
echo Setup complete! Please:
echo 1. Edit local.properties with your Android SDK path
echo 2. Edit firebase-credentials.json with your Firebase service account key
echo 3. Edit web-controller\.env with your Firebase project details
echo 4. Place google-services.json in the app\ directory
echo.
echo Then run: gradlew.bat build
pause