package org.lineageos.xiaomi_tws.headset

import android.util.Log
import org.lineageos.xiaomi_tws.headset.HeadsetManager.Companion.DEBUG
import org.lineageos.xiaomi_tws.utils.ByteUtils.hexToBytes
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

import java.nio.ByteBuffer

object ATCommand {
    data class Payload(val type: Byte, val value: ByteArray)
    data class Frame(val commandType: Byte, val payload: Payload)

    private val TAG = ATCommand::class.java.simpleName

    private val COMMAND_START = byteArrayOf(0xFF.toByte(), 0x01, 0x02, 0x01)
    private val COMMAND_END = byteArrayOf(0xFF.toByte())
    private const val PAYLOAD_MAX_SIZE = 0xFE

    fun encodeToHex(frame: Frame): String {
        val commandType = frame.commandType
        val payloadType = frame.payload.type
        val payload = frame.payload.value

        require(payload.size <= PAYLOAD_MAX_SIZE) {
            "Payload too large: ${payload.size}, max is 254"
        }

        val buffer =
            ByteBuffer.allocate(COMMAND_START.size + payload.size + 3 + COMMAND_END.size)
        buffer.put(COMMAND_START)
        buffer.put(commandType)
        buffer.put((payload.size + 1).toByte())
        buffer.put(payloadType)
        buffer.put(payload)
        buffer.put(COMMAND_END)

        return buffer.array().toHexString()
    }

    fun decodeFromHex(atValue: String): Frame {
        val buffer = ByteBuffer.wrap(atValue.hexToBytes())
        require(buffer.remaining() >= COMMAND_START.size + COMMAND_END.size + 2) {
            "AT Command data too short: ${buffer.remaining()}"
        }

        val startBytes = ByteArray(COMMAND_START.size).apply {
            buffer.get(this, 0, COMMAND_START.size)
        }
        if (!startBytes.contentEquals(COMMAND_START)) {
            throw IllegalArgumentException("Invalid AT Command start")
        }

        val commandType = buffer.get()
        val payloadLength = buffer.get().toInt() and 0xFF
        if (payloadLength < 1) {
            throw IllegalArgumentException("AT Command payload length must include payload type")
        }
        if (buffer.remaining() != payloadLength + COMMAND_END.size) {
            throw IllegalArgumentException("AT Command payload length mismatch")
        }

        val payloadType = buffer.get()
        val payload = ByteArray(payloadLength - 1).apply {
            buffer.get(this, 0, payloadLength - 1)
        }

        val endBytes = ByteArray(COMMAND_END.size).apply {
            buffer.get(this, 0, COMMAND_END.size)
        }
        if (!endBytes.contentEquals(COMMAND_END)) {
            throw IllegalArgumentException("Invalid AT Command end")
        }

        if (DEBUG) Log.d(
            TAG, "decodeFromHex: " +
                    "commandType=$commandType, " +
                    "payloadType=$payloadType, " +
                    "payload=${payload.toHexString()}"
        )
        return Frame(commandType, Payload(payloadType, payload))
    }
}
