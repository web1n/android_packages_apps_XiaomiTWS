package org.lineageos.xiaomi_tws.headset.commands

import org.lineageos.xiaomi_tws.headset.commands.notify.*
import org.lineageos.xiaomi_tws.headset.CommandData.Notify

abstract class NotifyCommand<T : Notify> : Command<T>(), Command.Encoder<T> {

    override val commandType: Byte = COMMAND_TYPE

    companion object {
        const val COMMAND_TYPE: Byte = 0x03

        val COMMANDS_MAP = mapOf(
            Notify.AccountKey::class.java to AccountKey,
            Notify.AutoSwitchDevice::class.java to AutoSwitchDevice,
            Notify.DeviceName::class.java to DeviceName,
            Notify.SwitchDevice::class.java to SwitchDevice,
        )
    }
}
