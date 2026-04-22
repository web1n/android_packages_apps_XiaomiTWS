package org.lineageos.xiaomi_tws.headset

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.features.DeviceBattery
import org.lineageos.xiaomi_tws.mma.ConfigData.InEarState
import org.lineageos.xiaomi_tws.mma.ConfigData.NoiseCancellationMode

import java.util.concurrent.ConcurrentHashMap

object DeviceStatus {

    private val statusMaps = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, Any>>()

    init {
        registerType<NoiseCancellationMode.Mode>()
        registerType<InEarState>()
        registerType<DeviceBattery>()
    }

    private inline fun <reified T : Any> registerType() {
        statusMaps.putIfAbsent(T::class.java, ConcurrentHashMap())
    }

    fun updateStatus(device: BluetoothDevice, value: Any): Boolean {
        val address = device.address

        val map = statusMaps[value.javaClass]
            ?: return true
        return map.put(address, value) != value
    }

    fun <T : Any> getStatus(device: BluetoothDevice, type: Class<T>): T? {
        val address = device.address
        val value = statusMaps[type]!![address]

        @Suppress("UNCHECKED_CAST")
        return value as? T
    }

    inline fun <reified T : Any> getStatus(device: BluetoothDevice): T? {
        return getStatus(device, T::class.java)
    }

    fun clearStatus(device: BluetoothDevice) {
        val address = device.address
        statusMaps.values.forEach { it.remove(address) }
    }

}
