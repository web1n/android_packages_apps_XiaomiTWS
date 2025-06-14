package org.lineageos.xiaomi_tws.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothUuid
import android.content.Context
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.BleSliceProvider.Companion.generateSliceUri
import org.lineageos.xiaomi_tws.earbuds.Earbuds

object BluetoothUtils {

    private val TAG = BluetoothUtils::class.java.simpleName
//    private const val DEBUG = true

    val bondedDevices: Set<BluetoothDevice>
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        get() = BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet()

    fun getBluetoothDevice(mac: String): BluetoothDevice {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isA2dpDevice(device: BluetoothDevice): Boolean {
        return device.uuids?.contains(BluetoothUuid.A2DP_SINK) == true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isHeadsetDevice(device: BluetoothDevice): Boolean {
        return device.uuids?.contains(BluetoothUuid.HFP) == true
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
    fun updateDeviceTypeMetadata(context: Context, device: BluetoothDevice) {
        mapOf(
            BluetoothDevice.METADATA_COMPANION_APP to context.packageName,
            BluetoothDevice.METADATA_DEVICE_TYPE to BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET,
            BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET to true,
            BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI to generateSliceUri(device.address)
        ).forEach {
            device.setMetadata(it.key, it.value)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    fun isDeviceMetadataSet(context: Context, device: BluetoothDevice): Boolean {
        val companionApp = device.getMetadata(BluetoothDevice.METADATA_COMPANION_APP)
            ?.runCatching { String(this) }?.getOrNull()
        return companionApp == context.packageName
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    fun updateDeviceBatteryMetadata(device: BluetoothDevice, earbuds: Earbuds?) {
        mapOf<Int, Any?>(
            BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING to earbuds?.left?.charging,
            BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING to earbuds?.right?.charging,
            BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING to earbuds?.case?.charging,
            BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY to earbuds?.left?.battery,
            BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY to earbuds?.right?.battery,
            BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY to earbuds?.case?.battery,
        ).forEach { key, value ->
            device.setMetadata(key, value ?: "")
        }
    }

}
