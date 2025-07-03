package org.lineageos.xiaomi_tws.mma

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.configs.InEarState

sealed class DeviceEvent {

    abstract val device: BluetoothDevice

    data class Connected(override val device: BluetoothDevice) : DeviceEvent()
    data class Disconnected(override val device: BluetoothDevice) : DeviceEvent()
    data class BatteryChanged(override val device: BluetoothDevice, val battery: Earbuds) :
        DeviceEvent()

    data class ConfigChanged(
        override val device: BluetoothDevice,
        val config: Int,
        val value: ByteArray
    ) : DeviceEvent()

    data class InEarStateChanged(
        override val device: BluetoothDevice,
        val left: InEarState.State,
        val right: InEarState.State
    ) : DeviceEvent()
}
