package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder

class EqualizerMode : ConfigRequestBuilder<EqualizerMode.Mode>(CONFIG_ID) {

    enum class Mode(internal val value: Byte) {
        Default(MODE_DEFAULT),
        VocalEnhance(MODE_VOCAL_ENHANCE),
        BassBoost(MODE_BASS_BOOST),
        TrebleBoost(MODE_TREBLE_BOOST),
        VolumeBoost(MODE_VOLUME_BOOST),
        Harman(MODE_HARMAN),
        HarmanMaster(MODE_HARMAN_MASTER);

        companion object {
            internal fun fromValue(value: Byte) = entries.find { it.value == value }
        }
    }

    override fun bytesToValue(bytes: ByteArray): Mode {
        if (bytes.size != VALID_BYTES_LENGTH) {
            throw NotImplementedError()
        }

        return Mode.fromValue(bytes[0]) ?: Mode.Default
    }

    override fun valueToBytes(value: Mode): ByteArray {
        return byteArrayOf(value.value)
    }

    companion object {
        private const val CONFIG_ID = 0x0007
        private const val VALID_BYTES_LENGTH = 1

        private const val MODE_DEFAULT: Byte = 0x00
        private const val MODE_VOCAL_ENHANCE: Byte = 0x01
        private const val MODE_BASS_BOOST: Byte = 0x05
        private const val MODE_TREBLE_BOOST: Byte = 0x06
        private const val MODE_VOLUME_BOOST: Byte = 0x07
        private const val MODE_HARMAN: Byte = 0x14
        private const val MODE_HARMAN_MASTER: Byte = 0x15
    }

}
