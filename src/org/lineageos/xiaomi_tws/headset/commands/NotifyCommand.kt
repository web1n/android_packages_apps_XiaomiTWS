package org.lineageos.xiaomi_tws.headset.commands

import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.Notify
import org.lineageos.xiaomi_tws.headset.commands.notify.*

abstract class NotifyCommand<T : Notify> : Command<T>(), Command.Encoder<T> {

    override val commandType: Byte = COMMAND_TYPE

    companion object {
        const val COMMAND_TYPE: Byte = 0x03

        fun decode(payload: Payload): Notify = when (payload.type) {
            AccountKey.payloadType -> AccountKey.decode(payload)
            AutoSwitchDevice.payloadType -> AutoSwitchDevice.decode(payload)
            DeviceName.payloadType -> DeviceName.decode(payload)
            SwitchDevice.payloadType -> SwitchDevice.decode(payload)
            else -> throw IllegalArgumentException("Unknown payload type: ${payload.type}")
        }

        fun encode(value: Notify): Payload = when (value) {
            is Notify.AccountKey -> AccountKey.encode(value)
            is Notify.AutoSwitchDevice -> AutoSwitchDevice.encode(value)
            is Notify.DeviceName -> DeviceName.encode(value)
            is Notify.SwitchDevice -> SwitchDevice.encode(value)
        }
    }
}
