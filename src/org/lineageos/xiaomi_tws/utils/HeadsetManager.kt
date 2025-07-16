package org.lineageos.xiaomi_tws.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT
import android.bluetooth.BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS
import android.bluetooth.BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.nearby.NearbyDevice
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter
import org.lineageos.xiaomi_tws.utils.ByteUtils.hexToBytes
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HeadsetManager private constructor(private val context: Context) {

    interface HeadsetDeviceListener {
        fun onDeviceChanged(device: BluetoothDevice, nearbyDevice: NearbyDevice)
    }

    private val adapter = context.getBluetoothAdapter()
    private var bluetoothHeadset: BluetoothHeadset? = null
    private val deviceListeners = ConcurrentHashMap.newKeySet<HeadsetDeviceListener>()

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

    private val atCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
            ) ?: return
            val arg = intent.getSerializableExtra(
                EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, Array<Any>::class.java
            )?.joinToString() ?: return

            if (!arg.startsWith("FF01020102") || arg.length < 16) {
                return
            }

            val nearbyDevice = runCatching {
                val scanRecordBytes = arg.substring(14, arg.length - 2).hexToBytes()
                val scanRecord = BluetoothUtils.parseFromBytes(scanRecordBytes) ?: return
                NearbyDevice.fromScanRecord(scanRecord)
            }.getOrNull() ?: return

            notifyDevice(device, nearbyDevice)
        }
    }

    fun registerListener() {
        initProxy()
        registerAtCommandReceiver()
    }

    fun unregisterListener() {
        closeProxy()
        unregisterAtCommandReceiver()
    }

    private fun initProxy() {
        adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }

    private fun closeProxy() {
        adapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        bluetoothHeadset = null
    }

    private fun registerAtCommandReceiver() {
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            Log.w(TAG, "Rom doesn't support Xiaomi AT Command")
            return
        }
        if (DEBUG) Log.d(TAG, "registerAtCommandReceiver")

        val atCommandFilter = IntentFilter(ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
            addCategory("$VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY.$MANUFACTURER_ID_XIAOMI")
        }
        context.registerReceiver(atCommandReceiver, atCommandFilter, Context.RECEIVER_EXPORTED)
    }

    private fun unregisterAtCommandReceiver() {
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            Log.w(TAG, "Rom doesn't support Xiaomi AT Command")
            return
        }
        if (DEBUG) Log.d(TAG, "unregisterAtCommandReceiver")

        context.unregisterReceiver(atCommandReceiver)
    }

    fun registerDeviceListener(listener: HeadsetDeviceListener) {
        if (!SUPPORT_XIAOMI_AT_COMMAND) return
        deviceListeners.add(listener)
    }

    fun unregisterDeviceListener(listener: HeadsetDeviceListener) {
        if (!SUPPORT_XIAOMI_AT_COMMAND) return
        deviceListeners.remove(listener)
    }

    private fun notifyDevice(device: BluetoothDevice, nearbyDevice: NearbyDevice) {
        deviceListeners.forEach { listener ->
            listener.runCatching { onDeviceChanged(device, nearbyDevice) }
                .onFailure { Log.e(TAG, "Error notifying listener", it) }
        }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendDeviceNewAccountKey(device: BluetoothDevice): Boolean {
        val randomBytes = byteArrayOf(0x04) + ByteArray(15).apply { Random.nextBytes(this) }

        return sendXiaomiATCommand(device, "FF010201031103${randomBytes.toHexString()}FF")
    }

    companion object {
        private val TAG = HeadsetManager::class.java.simpleName
        private const val DEBUG = true

        private const val AT_COMMAND_XIAOMI = "+XIAOMI"
        private const val MANUFACTURER_ID_XIAOMI = 0x038F

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
