package org.lineageos.xiaomi_tws.headset

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAssignedNumbers
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
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.headset.commands.Command
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HeadsetManager private constructor(private val context: Context) {

    interface HeadsetDeviceListener {
        fun onDeviceConnected(device: BluetoothDevice)
        fun onDeviceDisconnected(device: BluetoothDevice)
        fun onDeviceChanged(device: BluetoothDevice, value: CommandData)
        fun onBatteryChanged(device: BluetoothDevice, battery: Earbuds)
    }

    private val adapter = context.getBluetoothAdapter()
    private var bluetoothHeadset: BluetoothHeadset? = null
    private val deviceListeners = ConcurrentHashMap.newKeySet<HeadsetDeviceListener>()
    private val connectedDevices = ConcurrentHashMap.newKeySet<BluetoothDevice>()
    private var isAtCommandReceiverRegistered = false
    private var isDeviceReceiverRegistered = false

    private val bluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
            ) ?: return
            if (intent.action != BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) return

            val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
            if (state == BluetoothHeadset.STATE_CONNECTED) {
                handleDeviceConnected(device)
            } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                handleDeviceDisconnected(device)
            }
        }
    }

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
            val args = intent.getSerializableExtra(
                EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, Array<Any>::class.java
            ) ?: return

            handleATCommand(device, args)
        }
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        Log.d(TAG, "Device connected: ${device.address}")
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        Log.d(TAG, "Device disconnected: ${device.address}")
        connectedDevices.remove(device)
        DeviceStatus.clearStatus(device)

        deviceListeners.forEach { listener ->
            listener.runCatching { onDeviceDisconnected(device) }
                .onFailure { Log.e(TAG, "Error notifying listener", it) }
        }
    }

    fun registerListener() {
        initProxy()
        registerAtCommandReceiver()
        registerDeviceReceiver()
    }

    fun unregisterListener() {
        closeProxy()
        unregisterAtCommandReceiver()
        unregisterDeviceReceiver()
    }

    private fun initProxy() {
        adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }

    private fun closeProxy() {
        adapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        bluetoothHeadset = null
    }

    private fun registerAtCommandReceiver() {
        if (isAtCommandReceiverRegistered) return
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            Log.w(TAG, "Rom doesn't support Xiaomi AT Command")
            return
        }
        if (DEBUG) Log.d(TAG, "registerAtCommandReceiver")

        val atCommandFilter = IntentFilter(ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
            addCategory("$VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY.$MANUFACTURER_ID_XIAOMI")
        }
        context.registerReceiver(atCommandReceiver, atCommandFilter, Context.RECEIVER_EXPORTED)
        isAtCommandReceiverRegistered = true
    }

    private fun unregisterAtCommandReceiver() {
        if (!isAtCommandReceiverRegistered) return
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            Log.w(TAG, "Rom doesn't support Xiaomi AT Command")
            return
        }
        if (DEBUG) Log.d(TAG, "unregisterAtCommandReceiver")

        context.unregisterReceiver(atCommandReceiver)
        isAtCommandReceiverRegistered = false
    }

    fun registerDeviceReceiver() {
        if (isDeviceReceiverRegistered) return
        if (DEBUG) Log.d(TAG, "registerDeviceReceiver")

        val filter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        context.registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_EXPORTED)
        isDeviceReceiverRegistered = true
    }

    fun unregisterDeviceReceiver() {
        if (!isDeviceReceiverRegistered) return
        if (DEBUG) Log.d(TAG, "unregisterDeviceReceiver")

        context.unregisterReceiver(bluetoothStateReceiver)
        isDeviceReceiverRegistered = false
        connectedDevices.clear()
    }

    fun registerDeviceListener(listener: HeadsetDeviceListener) {
        if (!SUPPORT_XIAOMI_AT_COMMAND) return
        deviceListeners.add(listener)
    }

    fun unregisterDeviceListener(listener: HeadsetDeviceListener) {
        if (!SUPPORT_XIAOMI_AT_COMMAND) return
        deviceListeners.remove(listener)
    }

    fun containsDevice(device: BluetoothDevice): Boolean {
        return connectedDevices.contains(device)
    }

    private fun checkDeviceConnected(device: BluetoothDevice) {
        if (connectedDevices.contains(device)) return

        connectedDevices.add(device)
        deviceListeners.forEach { listener ->
            listener.runCatching { onDeviceConnected(device) }
                .onFailure { Log.e(TAG, "Error notifying listener", it) }
        }
    }

    private fun handleATCommand(device: BluetoothDevice, args: Array<Any>) {
        if (DEBUG) Log.d(TAG, "handleATCommand: ${device.address}, ${args.joinToString()}")

        val parsed = parseATCommand(args) ?: return
        if (DEBUG) Log.d(TAG, "Parsed AT Command: $parsed")

        notifyDevice(device, parsed)
    }

    private fun parseATCommand(args: Array<Any>): Any? {
        return runCatching {
            when (args.size) {
                1 if args[0] is String -> Command.decode(ATCommand.decodeFromHex(args[0] as String))
                7 if args.all { it is Int } -> Earbuds.fromBytes(
                    (args[3] as Int).toByte(), (args[4] as Int).toByte(), (args[5] as Int).toByte()
                )

                else -> {
                    Log.w(TAG, "Not valid at command format: ${args.joinToString()}")
                    null
                }
            }
        }.onFailure {
            Log.w(TAG, "Failed to parse AT Command: ${args.joinToString()}", it)
        }.getOrNull()
    }

    private fun notifyDevice(device: BluetoothDevice, value: Any) {
        checkDeviceConnected(device)
        val newValue = updateStatus(device, value) ?: return

        deviceListeners.forEach { listener ->
            listener.runCatching {
                when (newValue) {
                    is Earbuds -> onBatteryChanged(device, newValue)
                    is CommandData -> onDeviceChanged(device, newValue)
                    else -> Log.w(TAG, "Unknown value type: ${newValue::class.java.simpleName}")
                }
            }.onFailure {
                Log.e(TAG, "Error notifying listener", it)
            }
        }
    }

    private fun updateStatus(device: BluetoothDevice, value: Any): Any? = when (value) {
        is CommandData.Status -> {
            val new = value.copy(
                anc = value.anc?.takeIf { DeviceStatus.updateStatus(device, it) },
                inEar = value.inEar?.takeIf { DeviceStatus.updateStatus(device, it) },
                raw = value.raw
            )
            new.takeIf { it.anc != null || it.inEar != null || it.raw.isNotEmpty() }
        }

        else -> value.takeIf { DeviceStatus.updateStatus(device, it) }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendATCommand(device: BluetoothDevice, arg: String): Boolean {
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            Log.w(TAG, "Rom doesn't support Xiaomi AT Command")
            return false
        }

        val result = bluetoothHeadset?.sendVendorSpecificResultCode(
            device,
            AT_COMMAND_XIAOMI,
            arg
        ) ?: let {
            Log.w(TAG, "bluetoothHeadset is null")
            return false
        }

        if (DEBUG) Log.d(TAG, "sendATCommand: $arg result: $result")
        return result
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendATCommand(device: BluetoothDevice, frame: ATCommand.Frame): Boolean {
        if (DEBUG) Log.d(TAG, "sendATCommand frame: $frame")
        return sendATCommand(device, ATCommand.encodeToHex(frame))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendATCommand(device: BluetoothDevice, notify: CommandData): Boolean {
        if (DEBUG) Log.d(TAG, "sendATCommand notify: $notify")
        return sendATCommand(device, Command.encode(notify))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendDeviceNewAccountKey(device: BluetoothDevice): Boolean {
        val randomBytes = byteArrayOf(0x04) + ByteArray(15).apply { Random.nextBytes(this) }
        if (DEBUG) Log.d(TAG, "sendDeviceNewAccountKey: ${randomBytes.toHexString()}")

        return sendATCommand(device, CommandData.Notify.AccountKey(randomBytes))
    }

    companion object {
        private val TAG = HeadsetManager::class.java.simpleName
        const val DEBUG = true

        private const val AT_COMMAND_XIAOMI = "+XIAOMI"
        private const val MANUFACTURER_ID_XIAOMI = 0x038F

        val SUPPORT_XIAOMI_AT_COMMAND = runCatching {
            BluetoothAssignedNumbers::class.java.getField("XIAOMI")
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
