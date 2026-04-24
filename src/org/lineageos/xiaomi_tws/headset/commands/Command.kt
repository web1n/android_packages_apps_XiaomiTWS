package org.lineageos.xiaomi_tws.headset.commands

import android.util.Log
import org.lineageos.xiaomi_tws.headset.ATCommand.Frame
import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData
import org.lineageos.xiaomi_tws.headset.HeadsetManager.Companion.DEBUG

abstract class Command<T : CommandData> {

    interface Encoder<T : CommandData> {
        fun encode(value: T): Payload
    }

    abstract val commandType: Byte
    abstract val payloadType: Byte

    abstract fun decode(payload: Payload): T

    companion object {
        private val TAG = Command::class.java.simpleName

        private val COMMANDS_MAP = mapOf(
            CommandData.FastConnect::class.java to FastConnectCommand,
//            CommandData.Notify::class.java to NotifyCommand,
            CommandData.Status::class.java to StatusCommand,
        ) + NotifyCommand.COMMANDS_MAP
        private val COMMANDS = COMMANDS_MAP.values

        fun decode(frame: Frame): CommandData {
            val config = COMMANDS.find {
                if (it is NotifyCommand) {
                    it.commandType == frame.commandType && it.payloadType == frame.payload.type
                } else {
                    it.commandType == frame.commandType
                }
            } ?: throw IllegalArgumentException("Unknown command type: ${frame.commandType}")
            val result = config.decode(frame.payload)

            if (DEBUG) Log.d(TAG, "decode: frame=$frame, result=$result")
            return result
        }

        fun encode(data: CommandData): Frame {
            val command = COMMANDS_MAP[data::class.java]
                ?: throw IllegalArgumentException("Unknown config data: $data")
            if (command !is Encoder<*>) {
                throw IllegalArgumentException("Command type $command $data is not encodable")
            }
            @Suppress("UNCHECKED_CAST")
            val encoder = command as Encoder<CommandData>

            val payload = encoder.encode(data)
            return Frame(command.commandType, payload)
        }
    }
}
