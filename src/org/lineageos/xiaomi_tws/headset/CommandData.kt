package org.lineageos.xiaomi_tws.headset

import org.lineageos.xiaomi_tws.mma.configs.InEarState
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationMode
import org.lineageos.xiaomi_tws.nearby.NearbyDevice

sealed interface CommandData {

    data class FastConnect(val nearbyDevice: NearbyDevice) : CommandData

    data class Status(
        val anc: NoiseCancellationMode.Mode?,
        val inEar: InEarState.BothState?,
        val raw: Map<Byte, ByteArray>
    ) : CommandData

    sealed interface Notify : CommandData {
        data class AccountKey(val key: ByteArray) : Notify
        data class AutoSwitchDevice(val enabled: Boolean, val newEnable: Boolean = false) : Notify
        data class DeviceName(val name: String) : Notify
        data class SwitchDevice(val deviceName: String) : Notify
    }
}
