package org.lineageos.xiaomi_tws.headset.commands.notify

import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.Notify.SwitchDevice
import org.lineageos.xiaomi_tws.headset.commands.NotifyCommand

object SwitchDevice : NotifyCommand<SwitchDevice>() {

    override val payloadType: Byte = 0x11

    private const val MAX_NAME_LENGTH = 30

    override fun decode(payload: Payload): SwitchDevice {
        val deviceName = String(payload.value, Charsets.UTF_8)
        return SwitchDevice(deviceName)
    }

    override fun encode(value: SwitchDevice): Payload {
        if (value.deviceName.length > MAX_NAME_LENGTH) {
            throw IllegalArgumentException("Device name must be at most $MAX_NAME_LENGTH characters, got ${value.deviceName.length}")
        }

        val bytes = value.deviceName.toByteArray(Charsets.UTF_8)
        return Payload(payloadType, bytes)
    }
}
