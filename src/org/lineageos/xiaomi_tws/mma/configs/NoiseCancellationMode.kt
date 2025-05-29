package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder

class NoiseCancellationMode : ConfigRequestBuilder<NoiseCancellationMode.Mode>(CONFIG_ID) {

    enum class Mode(internal val value: Byte) {
        Off(MODE_OFF),
        On(MODE_ON),
        Transparency(MODE_TRANSPARENCY);

        companion object {
            fun fromByte(value: Byte) = entries.find { it.value == value }
        }
    }

    override fun bytesToValue(bytes: ByteArray): Mode {
        if (bytes.size != VALID_BYTES_LENGTH) {
            throw NotImplementedError()
        }

        return Mode.fromByte(bytes[0]) ?: Mode.Off
    }

    override fun valueToBytes(value: Mode): ByteArray {
        return byteArrayOf(value.value, 0x00)
    }

    companion object {

        private const val CONFIG_ID = 0x000B
        private const val VALID_BYTES_LENGTH = 2

        private const val MODE_OFF: Byte = 0x00
        private const val MODE_ON: Byte = 0x01
        private const val MODE_TRANSPARENCY: Byte = 0x02
    }
}