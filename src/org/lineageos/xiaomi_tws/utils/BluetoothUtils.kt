package org.lineageos.xiaomi_tws.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.BleSliceProvider.Companion.generateSliceUri
import org.lineageos.xiaomi_tws.earbuds.Earbuds

object BluetoothUtils {

    private val TAG = BluetoothUtils::class.java.simpleName
//    private const val DEBUG = true

    val connectedHeadsetA2DPDevices: List<BluetoothDevice>
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        get() {
            return BluetoothAdapter.getDefaultAdapter()?.bondedDevices
                ?.filter { it.isConnected }
                ?.filter { isHeadsetA2DPDevice(it) }
                ?.toList().orEmpty()
        }

    fun getBluetoothDevice(mac: String): BluetoothDevice {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
    }

    fun isHeadsetA2DPDevice(device: BluetoothDevice): Boolean {
        return device.bluetoothClass?.run {
            doesClassMatch(BluetoothClass.PROFILE_A2DP)
                    && doesClassMatch(BluetoothClass.PROFILE_HEADSET)
        } == true
    }

    fun Context.getBluetoothAdapter(): BluetoothAdapter {
        return getSystemService(BluetoothManager::class.java).adapter
    }

    fun BluetoothDevice.setMetadata(key: Int, value: Any): Boolean {
        val valueStr = value.toString()
        if (valueStr.length > BluetoothDevice.METADATA_MAX_LENGTH) {
            return false
        }

        return setMetadata(key, valueStr.toByteArray())
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    fun updateDeviceTypeMetadata(device: BluetoothDevice) {
        mapOf(
            BluetoothDevice.METADATA_DEVICE_TYPE to BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET,
            BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET to true,
            BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI to generateSliceUri(device.address)
        ).forEach {
            device.setMetadata(it.key, it.value)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    fun updateDeviceBatteryMetadata(earbuds: Earbuds) {
        mapOf<Int, Any>(
            BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING to earbuds.left.charging,
            BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING to earbuds.right.charging,
            BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING to earbuds.case.charging,
            BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY to earbuds.left.battery,
            BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY to earbuds.right.battery,
            BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY to earbuds.case.battery,
        ).forEach {
            earbuds.device.setMetadata(it.key, it.value)
        }
    }

}
