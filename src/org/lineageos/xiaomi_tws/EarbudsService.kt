package org.lineageos.xiaomi_tws

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.InEarState
import org.lineageos.xiaomi_tws.nearby.NearbyDevice
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceListener
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.utils.HeadsetManager
import org.lineageos.xiaomi_tws.utils.MediaManager
import org.lineageos.xiaomi_tws.utils.MediaManager.Companion.isSelected
import org.lineageos.xiaomi_tws.utils.MediaManager.MediaPlayingListener
import org.lineageos.xiaomi_tws.utils.NotificationUtils
import org.lineageos.xiaomi_tws.utils.SettingsUtils

@SuppressLint("MissingPermission")
class EarbudsService : Service() {

    private val mmaManager: MMAManager by lazy { MMAManager.getInstance(this) }
    private val headsetManager: HeadsetManager by lazy { HeadsetManager.getInstance(this) }
    private val nearbyDeviceScanner: NearbyDeviceScanner by lazy {
        NearbyDeviceScanner.getInstance(this)
    }
    private val mediaManager: MediaManager by lazy { MediaManager(this) }
    private val settingsUtils: SettingsUtils by lazy { SettingsUtils.getInstance(this) }

    private val mmaListener = object : MMAListener {
        override fun onDeviceEvent(event: DeviceEvent) {
            when (event) {
                is DeviceEvent.Connected -> updateStatus(event.device)
                is DeviceEvent.Disconnected -> clearDevice(event.device)
                is DeviceEvent.BatteryChanged -> updateBattery(event.device, event.battery)
                is DeviceEvent.InEarStateChanged ->
                    switchMediaDevice(event.device, event.state)

                else -> {}
            }
        }
    }

    private val deviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateNearbyScanStatus()
    }

    private val mediaPlayingListener = object : MediaPlayingListener {
        override fun onAnyMediaPlaying() = tryConnectNearbyDevice()
    }

    private val nearbyDeviceListener = object : NearbyDeviceListener {
        override fun onDevicesChanged(devices: Set<NearbyDevice>) {
            if (DEBUG) Log.d(TAG, "Nearby devices changed: ${devices.joinToString()}")
        }
    }

    private val headsetDeviceListener = object : HeadsetManager.HeadsetDeviceListener {
        override fun onDeviceChanged(device: BluetoothDevice, nearbyDevice: NearbyDevice) =
            updateStatus(device, nearbyDevice)
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        registerStateListener()
        registerFastConnectListener()
        registerMediaManager()
        registerHeadsetListener()
        registerMMAManager()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DEBUG) Log.d(TAG, "onDestroy")

        unregisterStateListener()
        unregisterFastConnectListener()
        unregisterMediaManager()
        unregisterHeadsetListener()
        unregisterMMAManager()
    }

    private fun registerStateListener() {
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(deviceStateReceiver, intentFilter, RECEIVER_EXPORTED)
    }

    private fun unregisterStateListener() = unregisterReceiver(deviceStateReceiver)

    private fun registerFastConnectListener() {
        nearbyDeviceScanner.registerNearbyListener(nearbyDeviceListener)
        updateNearbyScanStatus()
    }

    private fun unregisterFastConnectListener() {
        nearbyDeviceScanner.unregisterNearbyListener(nearbyDeviceListener)
        nearbyDeviceScanner.stopScan()
    }

    private fun registerMediaManager() {
        mediaManager.addPlayingListener(mediaPlayingListener)
        mediaManager.startScan()
    }

    private fun unregisterMediaManager() {
        mediaManager.removeListener(mediaPlayingListener)
        mediaManager.stopScan()
    }

    private fun registerHeadsetListener() {
        headsetManager.registerListener()
        headsetManager.registerDeviceListener(headsetDeviceListener)
    }

    private fun unregisterHeadsetListener() {
        headsetManager.unregisterDeviceListener(headsetDeviceListener)
        headsetManager.unregisterListener()
    }

    private fun registerMMAManager() {
        mmaManager.registerConnectionListener(mmaListener)
        mmaManager.startBluetoothStateListening()
    }

    private fun unregisterMMAManager() {
        mmaManager.unregisterConnectionListener(mmaListener)
        mmaManager.stopBluetoothStateListening()
    }

    override fun onBind(intent: Intent) = null

    private fun updateNearbyScanStatus() {
        val isLeEnabled =
            getSystemService(BluetoothManager::class.java)?.adapter?.isLeEnabled == true
        val isInteractive = getSystemService(PowerManager::class.java).isInteractive
        val bleScanEnabled = settingsUtils.enableBleScan

        if (isLeEnabled && isInteractive && bleScanEnabled) {
            if (!nearbyDeviceScanner.isScanning()) nearbyDeviceScanner.startScan()
        } else {
            if (nearbyDeviceScanner.isScanning()) nearbyDeviceScanner.stopScan()
        }
    }

    private fun tryConnectNearbyDevice() {
        val builtinDevice = mediaManager.getBuiltinMediaDevice() ?: return
        if (!builtinDevice.isSelected()) return

        val device = nearbyDeviceScanner.devices
            .mapNotNull { it.getDevice(this) }
            .filter { it.bondState == BluetoothDevice.BOND_BONDED && !it.isConnected }
            .find { settingsUtils.isAutoConnectDeviceEnabled(it) }
            ?: return

        if (DEBUG) Log.d(TAG, "Find bonded nearby device, try connect: $device")
        device.runCatching { connect() }
    }

    private fun updateStatus(device: BluetoothDevice, nearbyDevice: NearbyDevice) {
        if (DEBUG) Log.d(TAG, "Headset device changed: $device, $nearbyDevice")
        if (!nearbyDevice.isValidAccountKey()) {
            if (DEBUG) Log.d(TAG, "Invalid account key for device: $device, generate new")

            headsetManager.sendDeviceNewAccountKey(device)
            return
        }

        val accountKey = nearbyDevice.accountKey
        if (settingsUtils.getAccountKeyForDevice(device) != accountKey) {
            if (DEBUG) Log.d(TAG, "Writing device account key: $device $accountKey")
            settingsUtils.putAccountKeyForDevice(device, nearbyDevice.accountKey)
        }
    }

    private fun updateStatus(device: BluetoothDevice) {
        BluetoothUtils.updateDeviceTypeMetadata(this, device)
        headsetManager.sendSwitchDeviceAllowed(device, settingsUtils.isSwitchDeviceAllowed(device))
    }

    private fun clearDevice(device: BluetoothDevice) {
        NotificationUtils.cancelEarbudsNotification(this, device)
        BluetoothUtils.updateDeviceBatteryMetadata(device, null)
    }

    private fun updateBattery(device: BluetoothDevice, earbuds: Earbuds) {
        BluetoothUtils.updateDeviceBatteryMetadata(device, earbuds)

        if (settingsUtils.enableNotification) {
            NotificationUtils.updateEarbudsNotification(this, device, earbuds)
        }
    }

    private fun switchMediaDevice(device: BluetoothDevice, state: InEarState.BothState) {
        if (!settingsUtils.isAutoSwitchDeviceEnabled(device)) {
            return
        }

        val builtinDevice = mediaManager.getBuiltinMediaDevice() ?: return
        val bluetoothDevice = mediaManager.getBluetoothMediaDevice(device) ?: return

        val leftInEar = state.left == InEarState.State.InEar
        val rightInEar = state.right == InEarState.State.InEar
        if (leftInEar or rightInEar) {
            if (builtinDevice.isSelected()) {
                if (DEBUG) Log.d(TAG, "Switching media device to ${bluetoothDevice.name}")
                mediaManager.connectDevice(bluetoothDevice)
            }

            if (leftInEar and rightInEar) {
                if (bluetoothDevice.isSelected()) {
                    mediaManager.playMedia()
                } else {
                    if (DEBUG) Log.d(TAG, "Media device ${bluetoothDevice.name} not selected")
                }
            }
        } else {
            if (bluetoothDevice.isSelected()) {
                mediaManager.pauseMedia()
                mediaManager.connectDevice(builtinDevice)
            } else {
                if (DEBUG) Log.d(TAG, "Media device ${bluetoothDevice.name} not selected")
            }
        }
    }

    companion object {
        private val TAG = EarbudsService::class.java.simpleName
        private const val DEBUG = true
    }
}
