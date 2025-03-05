package org.lineageos.xiaomi_bluetooth.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.IntentCompat
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.UUID_XIAOMI_FAST_CONNECT
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_DATA_LENGTH
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils.parseFromBytes
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.hexToBytes

object ATUtils {

    private val TAG = ATUtils::class.java.simpleName
    private const val DEBUG = true

    const val MANUFACTURER_ID_XIAOMI: Int = 0x038F
    const val VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI: String = "+XIAOMI"

    private const val COMMAND_START = "FF010201"
    private const val COMMAND_END = "FF"

    @SuppressLint("CheckResult", "PrivateApi")
    fun checkIfSupportXiaomiATCommand() {
        try {
            BluetoothHeadset::class.java.getDeclaredField("VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI")
        } catch (_: NoSuchFieldException) {
            Log.e(TAG, "ROM dose not support +XIAOMI AT command")
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendATCommand(
        bluetoothHeadset: BluetoothHeadset,
        device: BluetoothDevice,
        type: Int, value: String
    ) {
        if (!device.isConnected) {
            return
        }

        val data = "%s%02x%s%s".format(COMMAND_START, type, value, COMMAND_END)

        val result = bluetoothHeadset.sendVendorSpecificResultCode(
            device, VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI, data
        )
        if (DEBUG) Log.d(TAG, "sendATCommand: type=$type, value=$value, result=$result")
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun sendUpdateATCommand(bluetoothHeadset: BluetoothHeadset, device: BluetoothDevice) {
        sendATCommand(bluetoothHeadset, device, 0x02, "0101")
    }

    private fun isValidATFastConnectCommand(arg0: String): Boolean {
        return arg0.length >= 16 && arg0.startsWith(COMMAND_START) && arg0.endsWith(COMMAND_END)
    }

    private fun parseATFastConnectCommand(device: BluetoothDevice, arg0: String): Earbuds? {
        check(isValidATFastConnectCommand(arg0)) {
            "Invalid AT Fast Connect command: $arg0"
        }
        val scanRecordBytes = arg0.substring(14, arg0.length - 2).hexToBytes()

        val record = checkNotNull(parseFromBytes(scanRecordBytes)) {
            "Failed to parse ScanRecord from AT command"
        }

        val fastConnectData = record.getServiceData(UUID_XIAOMI_FAST_CONNECT)
        check(fastConnectData != null && fastConnectData.size >= XIAOMI_MMA_DATA_LENGTH) {
            "Invalid fast connect data size ${fastConnectData?.size}"
        }

        return Earbuds.fromBytes(
            device.address,
            fastConnectData[13], fastConnectData[12], fastConnectData[14]
        ).let {
            if (it.valid) it else null
        }
    }

    fun parseATCommandIntent(intent: Intent): Earbuds? {
        val device = checkNotNull(
            IntentCompat.getParcelableExtra(
                intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
            )
        ) {
            "Unable to get bluetooth device"
        }

        val cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
        check(cmd == VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI) {
            "Invalid AT command received: $cmd"
        }

        val type = intent.getIntExtra(
            BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1
        )
        check(type == BluetoothHeadset.AT_CMD_TYPE_SET) {
            "Invalid AT cmd type received: $type"
        }

        val args = IntentCompat.getParcelableExtra(
            intent,
            BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS,
            Array<Any>::class.java
        )
        check(args != null && args.size == 1 && args[0] is String) {
            "Invalid AT command args type"
        }

        return parseATFastConnectCommand(device, args[0] as String).also {
            if (DEBUG) Log.d(TAG, "handleATCommand: Parsed AT command: ${args[0]} -> $it")
        }
    }

}
