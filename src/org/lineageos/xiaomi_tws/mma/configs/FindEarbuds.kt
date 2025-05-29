package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder

class FindEarbuds : ConfigRequestBuilder<Pair<Boolean, List<FindEarbuds.Position>>>(CONFIG_ID) {

    enum class Position(internal val value: Int) {
        Left(POSITION_LEFT),
        Right(POSITION_RIGHT);
    }

    private enum class Type(val value: Byte) {
        Enabled(TYPE_ENABLED),
        Disabled(TYPE_DISABLED);
    }

    override fun bytesToValue(bytes: ByteArray): Pair<Boolean, List<Position>> {
        if (bytes.size != VALID_BYTES_LENGTH) {
            throw NotImplementedError()
        }

        val type = Type.entries
            .find { it.value == bytes[0] }
            ?: throw NotImplementedError()
        val positions = Position.entries
            .filter { (it.value and bytes[1].toInt()) != 0 }

        return (type == Type.Enabled) to positions
    }

    override fun valueToBytes(value: Pair<Boolean, List<Position>>): ByteArray {
        val (enabled, positions) = value

        val typeByte = if (enabled) Type.Enabled.value else Type.Disabled.value
        val positionByte = positions.fold(0) { acc, position -> acc or position.value }

        return byteArrayOf(typeByte, positionByte.toByte())
    }

    companion object {
        private const val CONFIG_ID = 0x0009
        private const val VALID_BYTES_LENGTH = 2

        private const val POSITION_LEFT = 0x01
        private const val POSITION_RIGHT = 0x02

        private const val TYPE_DISABLED: Byte = 0x00
        private const val TYPE_ENABLED: Byte = 0x01
    }

}
