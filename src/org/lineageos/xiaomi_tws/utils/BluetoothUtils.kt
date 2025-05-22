package org.lineageos.xiaomi_tws.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanRecord
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission

object BluetoothUtils {

    private val TAG = BluetoothUtils::class.java.simpleName
//    private const val DEBUG = true

    val connectedHeadsetA2DPDevices: List<BluetoothDevice>
        @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        get() {
            return BluetoothAdapter.getDefaultAdapter()?.bondedDevices
                ?.filter { it.isConnected }
                ?.filter { isHeadsetA2DPDevice(it) }
                ?.toList().orEmpty()
        }

    fun getBluetoothDevice(mac: String): BluetoothDevice {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
    }

    fun parseFromBytes(bytes: ByteArray): ScanRecord? {
        return try {
            ScanRecord::class.java
                .getMethod("parseFromBytes", ByteArray::class.java)
                .invoke(null, bytes) as ScanRecord
        } catch (e: Exception) {
            Log.e(TAG, "parseAsScanRecord: ", e)
            null
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

}
