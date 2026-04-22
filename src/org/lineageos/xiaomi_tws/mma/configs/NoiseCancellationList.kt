package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.NoiseCancellationList
import org.lineageos.xiaomi_tws.mma.ConfigData.NoiseCancellationMode.Mode

object NoiseCancellationList :
    Config<NoiseCancellationList>(), Config.Encoder<NoiseCancellationList> {

    private const val MODES_NOT_MODIFY: Byte = -1

    override val configId = 0x000A
    override val validBytesLength = 2

    override fun decode(bytes: ByteArray): NoiseCancellationList {
        val (leftByte, rightByte) = bytes

        val left = getModes(leftByte)
        val right = getModes(rightByte)
        return NoiseCancellationList(left, right)
    }

    override fun encode(value: NoiseCancellationList): ByteArray {
        val leftByte = value.left
            ?.fold(0) { acc, mode -> acc or (1 shl mode.value.toInt()) }
            ?.toByte()
            ?: MODES_NOT_MODIFY
        val rightByte = value.right
            ?.fold(0) { acc, mode -> acc or (1 shl mode.value.toInt()) }
            ?.toByte()
            ?: MODES_NOT_MODIFY

        return byteArrayOf(leftByte, rightByte)
    }

    private fun getModes(byte: Byte): List<Mode> {
        return Mode.entries.filter { mode -> (byte.toInt() and (1 shl mode.value.toInt())) != 0 }
    }
}
