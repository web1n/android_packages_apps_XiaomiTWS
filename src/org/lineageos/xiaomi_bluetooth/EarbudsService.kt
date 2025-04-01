package org.lineageos.xiaomi_bluetooth

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.IntentCompat
import org.lineageos.xiaomi_bluetooth.utils.ATUtils
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils.getBluetoothAdapter
import org.lineageos.xiaomi_bluetooth.utils.NotificationUtils
import org.lineageos.xiaomi_bluetooth.utils.PermissionUtils.checkSelfPermissionGranted

class EarbudsService : Service() {

    private var bluetoothHeadset: BluetoothHeadset? = null

    private val profileListener: BluetoothProfile.ServiceListener =
        object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile != BluetoothProfile.HEADSET) {
                    return
                }

                if (DEBUG) Log.d(TAG, "Bluetooth headset connected: $proxy")
                bluetoothHeadset = proxy as BluetoothHeadset
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile != BluetoothProfile.HEADSET) {
                    return
                }

                if (DEBUG) Log.d(TAG, "Bluetooth headset disconnected")
                bluetoothHeadset = null
            }
        }

    private val bluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null) return
            if (DEBUG) Log.d(TAG, "onReceive: ${intent.action}")
            val device = IntentCompat.getParcelableExtra(
                intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
            )
            if (device == null) {
                if (DEBUG) Log.d(TAG, "onReceive: Received intent with null device")
                return
            }

            when (intent.action) {
                BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT ->
                    handleATCommand(device, intent)

                BluetoothDevice.ACTION_ALIAS_CHANGED -> handleDeviceAliasChanged(device, intent)

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleDeviceDisconnected(device)

                else -> Log.w(TAG, "unknown action: ${intent.action}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        initializeProfileProxy()
        startBluetoothStateListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DEBUG) Log.d(TAG, "onDestroy")

        closeProfileProxy()
        stopBluetoothStateListening()
    }

    override fun onBind(intent: Intent) = null

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        if (!checkSelfPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            return
        }

        NotificationUtils.cancelEarbudsNotification(this, device)
    }

    private fun handleDeviceAliasChanged(device: BluetoothDevice, intent: Intent) {
        val alias = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
        if (alias.isNullOrEmpty()) {
            Log.w(TAG, "Found null or empty BluetoothDevice alias for $device")
            return
        }

        if (bluetoothHeadset != null
            && checkSelfPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            ATUtils.sendSetDeviceAliasATCommand(bluetoothHeadset!!, device, alias)
        }
    }

    private fun handleATCommand(device: BluetoothDevice, intent: Intent) {
        val args = IntentCompat.getParcelableExtra(
            intent,
            BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS,
            Array<Any>::class.java
        )
        if ((args?.size ?: 0) > 1) {
            Log.w(TAG, "handleATCommand: Not valid args size: ${args?.size}, send update")

            if (bluetoothHeadset != null
                && checkSelfPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                ATUtils.sendUpdateATCommand(bluetoothHeadset!!, device)
            }
            return
        }

        runCatching {
            ATUtils.parseATCommandIntent(intent)
        }.onSuccess { earbuds ->
            if (earbuds == null) return

            if (checkSelfPermissionGranted(Manifest.permission.BLUETOOTH_PRIVILEGED)) {
                earbuds.updateDeviceTypeMetadata()
                earbuds.updateDeviceBatteryMetadata()
            }

            if (checkSelfPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                NotificationUtils.updateEarbudsNotification(this, earbuds)
            }
        }.onFailure {
            Log.e(TAG, "handleATCommand: Unable to parse at command intent", it)
        }
    }

    private fun initializeProfileProxy() {
        getBluetoothAdapter().getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)
    }

    private fun closeProfileProxy() {
        bluetoothHeadset?.run {
            getBluetoothAdapter().closeProfileProxy(BluetoothProfile.HEADSET, this)
            bluetoothHeadset = null
        }
    }

    private fun startBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "startBluetoothStateListening")

        val filter = IntentFilter().apply {
            // Xiaomi AT event
            addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
            addCategory(
                BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
                        + "." + ATUtils.MANUFACTURER_ID_XIAOMI
            )
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ALIAS_CHANGED)
        }

        if (DEBUG) Log.d(TAG, "registering bluetooth state receiver")
        registerReceiver(bluetoothStateReceiver, filter)
    }

    private fun stopBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "stopBluetoothStateListening")

        unregisterReceiver(bluetoothStateReceiver)
    }

    companion object {
        private val TAG = EarbudsService::class.java.simpleName
        private const val DEBUG = true
    }
}
