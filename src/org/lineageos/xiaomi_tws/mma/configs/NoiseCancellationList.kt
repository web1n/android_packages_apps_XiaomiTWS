package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder

class NoiseCancellationList :
    ConfigRequestBuilder<Map<NoiseCancellationList.Position, List<NoiseCancellationList.Mode>>>
        (CONFIG_ID) {

    enum class Position { Left, Right; }

    enum class Mode(internal val value: Int) {
        Off(MODE_OFF),
        On(MODE_ON),
        Transparency(MODE_TRANSPARENCY);
    }

    override fun bytesToValue(bytes: ByteArray): Map<Position, List<Mode>> {
        if (bytes.size != VALID_BYTES_LENGTH) {
            throw NotImplementedError()
        }

        return mapOf(
            Position.Left to getModesFromByte(bytes[0]),
            Position.Right to getModesFromByte(bytes[1])
        )
    }

    private fun getModesFromByte(byte: Byte): List<Mode> {
        return Mode.entries.filter { mode -> (byte.toInt() and (1 shl mode.value)) != 0 }
    }

    override fun valueToBytes(value: Map<Position, List<Mode>>): ByteArray {
        val leftByte = value[Position.Left]
            ?.fold(0) { acc, mode -> acc or (1 shl mode.value) }
            ?: MODES_NOT_MODIFY
        val rightByte = value[Position.Right]
            ?.fold(0) { acc, mode -> acc or (1 shl mode.value) }
            ?: MODES_NOT_MODIFY

        return byteArrayOf(leftByte.toByte(), rightByte.toByte())
    }

    companion object {

        private const val CONFIG_ID = 0x000A
        private const val VALID_BYTES_LENGTH = 2

        private const val MODE_OFF = 0x00
        private const val MODE_ON = 0x01
        private const val MODE_TRANSPARENCY = 0x02
        private const val MODES_NOT_MODIFY: Byte = -1
    }

}
