package org.lineageos.xiaomi_tws.mma

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.features.DeviceBattery

sealed class DeviceEvent {

    abstract val device: BluetoothDevice

    data class Connected(override val device: BluetoothDevice) : DeviceEvent()
    data class Disconnected(override val device: BluetoothDevice) : DeviceEvent()
    data class BatteryChanged(override val device: BluetoothDevice, val battery: DeviceBattery) :
        DeviceEvent()

    data class RawConfigChanged(
        override val device: BluetoothDevice,
        val configId: Int,
        val value: ByteArray
    ) : DeviceEvent()

    data class ConfigChanged(
        override val device: BluetoothDevice,
        val configId: Int,
        val value: ConfigData
    ) : DeviceEvent()
}
