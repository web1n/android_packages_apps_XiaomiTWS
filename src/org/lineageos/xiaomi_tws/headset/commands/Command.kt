package org.lineageos.xiaomi_tws.headset.commands

import android.util.Log
import org.lineageos.xiaomi_tws.headset.ATCommand.Frame
import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData
import org.lineageos.xiaomi_tws.headset.CommandData.Notify
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

        fun decode(frame: Frame): CommandData {
            val data = when (frame.commandType) {
                FastConnectCommand.commandType -> FastConnectCommand.decode(frame.payload)
                StatusCommand.commandType -> StatusCommand.decode(frame.payload)
                NotifyCommand.COMMAND_TYPE -> NotifyCommand.decode(frame.payload)
                else -> throw IllegalArgumentException("Unknown command type: ${frame.commandType}")
            }

            if (DEBUG) Log.d(TAG, "decode: frame=$frame, result=$data")
            return data
        }

        fun encode(data: CommandData) = when (data) {
            is Notify -> Frame(NotifyCommand.COMMAND_TYPE, NotifyCommand.encode(data))
            else -> {
                throw IllegalArgumentException("Command does not support encode: ${data::class.simpleName}")
            }
        }
    }
}
