# Quick Start Guide

## Immediate Build

Run this single command to build everything:

```bash
./build-all.sh
```

## What This Does

1. ✅ **Checks Prerequisites** - Java, Node.js, Python
2. ✅ **Installs Dependencies** - npm packages, Python Firebase SDK
3. ✅ **Creates Config Files** - All templates ready for your Firebase details
4. ⚠️ **Attempts Android Build** - May need manual Gradle update

## Result

After running the build script, you'll have:

- **Web Controller**: Ready to run with `cd web-controller && npm start`
- **Python Script**: Ready to run with `python3 command_sender.py` 
- **Android App**: Either built successfully or needs Gradle version update

## Next Step: Configure Firebase

1. **Get Firebase credentials** from [Firebase Console](https://console.firebase.google.com/)
2. **Fill in templates**:
   - `firebase-credentials.json` (Python script)
   - `web-controller/.env` (Web controller)
3. **Run components**:
   ```bash
   # Web Controller
   cd web-controller && npm start
   
   # Python Script (in another terminal)
   python3 command_sender.py
   ```

## If Android Build Failed

The Android component may need a Gradle version update. See `BUILD_GUIDE.md` for solutions.

**The system is ready to use!** 🚀