package org.lineageos.xiaomi_tws.headset.commands.notify

import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.Notify.DeviceName
import org.lineageos.xiaomi_tws.headset.commands.NotifyCommand

object DeviceName : NotifyCommand<DeviceName>() {

    override val payloadType: Byte = 0x01

    private const val MAX_NAME_LENGTH = 30

    override fun decode(payload: Payload): DeviceName {
        val deviceName = String(payload.value, Charsets.UTF_8)
        return DeviceName(deviceName)
    }

    override fun encode(value: DeviceName): Payload {
        if (value.name.length > MAX_NAME_LENGTH) {
            throw IllegalArgumentException("Device name must be at most $MAX_NAME_LENGTH characters, got ${value.name.length}")
        }

        val bytes = value.name.toByteArray(Charsets.UTF_8)
        return Payload(payloadType, bytes)
    }
}
