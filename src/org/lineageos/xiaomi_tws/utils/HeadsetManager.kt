package org.lineageos.xiaomi_tws.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter

class HeadsetManager private constructor(private val context: Context) {

    private val adapter = context.getBluetoothAdapter()
    private var bluetoothHeadset: BluetoothHeadset? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HEADSET || proxy !is BluetoothHeadset) {
                return
            }
            bluetoothHeadset = proxy
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HEADSET) {
                return
            }
            bluetoothHeadset = null
        }
    }

    fun initProxy() {
        adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }

    fun closeProxy() {
        adapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        bluetoothHeadset = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendXiaomiATCommand(device: BluetoothDevice, arg: String): Boolean {
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            Log.w(TAG, "Rom doesn't support Xiaomi AT Command")
        }

        val result = bluetoothHeadset?.sendVendorSpecificResultCode(
            device,
            AT_COMMAND_XIAOMI,
            arg
        ) ?: let {
            Log.w(TAG, "bluetoothHeadset is null")
            return false
        }

        if (DEBUG) Log.d(TAG, "sendXiaomiATCommand: $arg result: $result")
        return result
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendSwitchDeviceAllowed(device: BluetoothDevice, enabled: Boolean): Boolean {
        return sendXiaomiATCommand(
            device,
            if (enabled) COMMAND_ENABLE_SWITCH_DEVICE else COMMAND_DISABLE_SWITCH_DEVICE
        )
    }

    companion object {
        private val TAG = HeadsetManager::class.java.simpleName
        private const val DEBUG = true

        private const val AT_COMMAND_XIAOMI = "+XIAOMI"

        private const val COMMAND_ENABLE_SWITCH_DEVICE = "FF01020103020E11FF"
        private const val COMMAND_DISABLE_SWITCH_DEVICE = "FF01020103020E00FF"

        val SUPPORT_XIAOMI_AT_COMMAND = runCatching {
            BluetoothHeadset::class.java.getField("VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI")
        }.isSuccess

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: HeadsetManager? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: HeadsetManager(context.applicationContext).also {
                instance = it
            }
        }
    }
}
