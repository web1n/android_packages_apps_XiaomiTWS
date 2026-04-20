package org.lineageos.xiaomi_tws.headset.commands.notify

import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.Notify.AutoSwitchDevice
import org.lineageos.xiaomi_tws.headset.commands.NotifyCommand

object AutoSwitchDevice : NotifyCommand<AutoSwitchDevice>() {

    override val payloadType: Byte = 0x0E

    private val AUTO_SWITCH_ENABLED = byteArrayOf(0x11)
    private val AUTO_SWITCH_DISABLED = byteArrayOf(0x00)

    override fun decode(payload: Payload): AutoSwitchDevice {
        if (payload.value.size != 1) {
            throw IllegalArgumentException("Invalid payload size: ${payload.value.size}")
        }

        val enabled = payload.value.contentEquals(AUTO_SWITCH_DISABLED)
        return AutoSwitchDevice(enabled)
    }

    override fun encode(value: AutoSwitchDevice): Payload {
        val bytes = if (value.enabled) AUTO_SWITCH_ENABLED else AUTO_SWITCH_DISABLED
        return Payload(payloadType, bytes)
    }
}
