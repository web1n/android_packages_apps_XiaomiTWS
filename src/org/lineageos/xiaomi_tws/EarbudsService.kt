package org.lineageos.xiaomi_tws

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.utils.NotificationUtils
import org.lineageos.xiaomi_tws.utils.PermissionUtils.checkSelfPermissionGranted

@SuppressLint("MissingPermission")
class EarbudsService : Service() {

    private val manager: MMAManager by lazy { MMAManager.getInstance(this) }
    private val mmaListener = object : MMAListener() {
        override fun onDeviceDisconnected(device: BluetoothDevice) {
            cancelNotification(device)
        }

        override fun onDeviceBatteryChanged(device: BluetoothDevice, earbuds: Earbuds) {
            updateBattery(earbuds)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        manager.startBluetoothStateListening()
        manager.registerConnectionListener(mmaListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DEBUG) Log.d(TAG, "onDestroy")

        manager.stopBluetoothStateListening()
        manager.unregisterConnectionListener(mmaListener)
    }

    override fun onBind(intent: Intent) = null

    private fun cancelNotification(device: BluetoothDevice) {
        if (checkSelfPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            NotificationUtils.cancelEarbudsNotification(this, device)
        }
    }

    private fun updateBattery(earbuds: Earbuds) {
        if (checkSelfPermissionGranted(Manifest.permission.BLUETOOTH_PRIVILEGED)) {
            BluetoothUtils.updateDeviceTypeMetadata(earbuds.device)
            BluetoothUtils.updateDeviceBatteryMetadata(earbuds)
        }

        if (checkSelfPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            NotificationUtils.updateEarbudsNotification(this, earbuds)
        }
    }

    companion object {
        private val TAG = EarbudsService::class.java.simpleName
        private const val DEBUG = true
    }
}
