package org.lineageos.xiaomi_tws.headset.commands

import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.FastConnect
import org.lineageos.xiaomi_tws.nearby.NearbyDevice
import org.lineageos.xiaomi_tws.utils.BluetoothUtils

object FastConnectCommand : Command<FastConnect>() {

    override val commandType: Byte = 0x02
    override val payloadType: Byte = 0x00

    override fun decode(payload: Payload): FastConnect {
        val scanRecord = BluetoothUtils.parseFromBytes(payload.value)
            ?: throw IllegalArgumentException("Invalid scan record: ${payload.value.contentToString()}")

        return FastConnect(NearbyDevice.fromScanRecord(scanRecord))
    }
}
