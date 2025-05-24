package org.lineageos.xiaomi_tws.mma

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.earbuds.Earbuds

open class MMAListener {
    open fun onDeviceConnected(device: BluetoothDevice) {}
    open fun onDeviceDisconnected(device: BluetoothDevice) {}
    open fun onDeviceBatteryChanged(device: BluetoothDevice, earbuds: Earbuds) {}
    open fun onDeviceConfigChanged(device: BluetoothDevice, config: Int, value: ByteArray) {}
    open fun onInEarStateChanged(device: BluetoothDevice, left: Boolean, right: Boolean) {}
}
