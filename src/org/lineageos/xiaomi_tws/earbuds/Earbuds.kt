package org.lineageos.xiaomi_tws.earbuds

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.BleSliceProvider.Companion.generateSliceUri
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothDevice
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.setMetadata

data class Earbuds(
    val address: String,
    val left: Earbud,
    val right: Earbud,
    val case: Earbud
) {

    val caseValid = this.case.valid
    val leftOrRightValid = this.left.valid || this.right.valid
    val valid = this.leftOrRightValid || this.caseValid

    val device: BluetoothDevice
        get() = getBluetoothDevice(address)

    val readableString: String
        get() = ArrayList<String>().apply {
            if (left.valid) add("\uD83C\uDFA7 Left: ${left.battery}% ${if (left.charging) "charging" else ""}")
            if (right.valid) add("\uD83C\uDFA7 Right: ${right.battery}% ${if (right.charging) "charging" else ""}")
            if (case.valid) add("\uD83D\uDD0B Case: ${case.battery}% ${if (case.charging) "charging" else ""}")
        }.joinToString()

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    fun updateDeviceTypeMetadata() = device.apply {
        if (!isConnected) {
            if (DEBUG) Log.d(TAG, "Device $device not connected")
            return@apply
        }

        mapOf(
            BluetoothDevice.METADATA_DEVICE_TYPE to BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET,
            BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET to true,
            BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI to generateSliceUri(address)
        ).forEach {
            setMetadata(it.key, it.value)
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    fun updateDeviceBatteryMetadata() = device.apply {
        if (!isConnected) {
            if (DEBUG) Log.d(TAG, "Device $device not connected")
            return@apply
        }

        mapOf<Int, Any>(
            BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING to left.charging,
            BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING to right.charging,
            BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING to case.charging,
            BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY to left.battery,
            BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY to right.battery,
            BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY to case.battery,
        ).forEach {
            setMetadata(it.key, it.value)
        }
    }

    companion object {
        private val TAG = Earbuds::class.java.simpleName
        private const val DEBUG = true

        fun fromBytes(address: String, left: Byte, right: Byte, case: Byte): Earbuds {
            return Earbuds(address, Earbud(left), Earbud(right), Earbud(case))
        }
    }
}
