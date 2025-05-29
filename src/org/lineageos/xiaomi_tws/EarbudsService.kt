package org.lineageos.xiaomi_tws

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.utils.MediaManager
import org.lineageos.xiaomi_tws.utils.NotificationUtils
import org.lineageos.xiaomi_tws.utils.SettingsUtils

@SuppressLint("MissingPermission")
class EarbudsService : Service() {

    private val mmaManager: MMAManager by lazy { MMAManager.getInstance(this) }
    private val mediaManager: MediaManager by lazy { MediaManager(this) }
    private val settingsUtils: SettingsUtils by lazy { SettingsUtils.getInstance(this) }

    private val mmaListener = object : MMAListener {
        override fun onDeviceEvent(event: DeviceEvent) {
            when (event) {
                is DeviceEvent.Disconnected -> cancelNotification(event.device)
                is DeviceEvent.BatteryChanged -> updateBattery(event.battery)
                is DeviceEvent.InEarStateChanged ->
                    switchMediaDevice(event.device, event.left, event.right)

                else -> {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        mediaManager.startScan()
        mmaManager.startBluetoothStateListening()
        mmaManager.registerConnectionListener(mmaListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DEBUG) Log.d(TAG, "onDestroy")

        mediaManager.stopScan()
        mmaManager.stopBluetoothStateListening()
        mmaManager.unregisterConnectionListener(mmaListener)
    }

    override fun onBind(intent: Intent) = null

    private fun cancelNotification(device: BluetoothDevice) {
        NotificationUtils.cancelEarbudsNotification(this, device)
    }

    private fun updateBattery(earbuds: Earbuds) {
        BluetoothUtils.updateDeviceTypeMetadata(this, earbuds.device)
        BluetoothUtils.updateDeviceBatteryMetadata(earbuds)

        if (settingsUtils.enableNotification) {
            NotificationUtils.updateEarbudsNotification(this, earbuds)
        }
    }

    private fun switchMediaDevice(
        device: BluetoothDevice,
        leftInEar: Boolean,
        rightInEar: Boolean
    ) {
        if (!settingsUtils.isAutoSwitchDeviceEnabled(device)) {
            return
        }

        if (leftInEar or rightInEar) {
            mediaManager.getBluetoothMediaDevice(device)?.let { mediaManager.connectDevice(it) }

            if (leftInEar and rightInEar) mediaManager.playMedia()
        } else {
            mediaManager.pauseMedia()
            mediaManager.getBuiltinMediaDevice()?.let { mediaManager.connectDevice(it) }
        }
    }

    companion object {
        private val TAG = EarbudsService::class.java.simpleName
        private const val DEBUG = true
    }
}
