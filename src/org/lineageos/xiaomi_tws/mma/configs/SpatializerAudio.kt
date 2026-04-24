package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.SpatializerAudio
import org.lineageos.xiaomi_tws.mma.ConfigData.SpatializerAudio.Mode
import org.lineageos.xiaomi_tws.utils.ByteUtils.isBitSet
import org.lineageos.xiaomi_tws.utils.ByteUtils.withBitSet

object SpatializerAudio : Config<SpatializerAudio>(), Config.Encoder<SpatializerAudio> {

    const val BIT_ENABLED = 0
    const val BIT_SPATIALIZER_OFFLOAD = 1
    const val BIT_HEAD_TRACKING_ENABLED = 3

    override val configId = 0x001E
    override val configIdSet = 0x001D
    override val validBytesLength = 1

    override fun decode(bytes: ByteArray): SpatializerAudio {
        val byte = bytes[0]

        val enabled = byte.isBitSet(BIT_ENABLED)
        val headTrackingEnabled = byte.isBitSet(BIT_HEAD_TRACKING_ENABLED)
        val offload = byte.isBitSet(BIT_SPATIALIZER_OFFLOAD)

        val mode = when {
            !enabled -> Mode.Off
            headTrackingEnabled -> Mode.HeadTracking
            else -> Mode.On
        }

        return SpatializerAudio(mode, offload)
    }

    override fun encode(value: SpatializerAudio): ByteArray {
        var byte: Byte = 0

        if (value.mode != Mode.Off) byte = byte.withBitSet(BIT_ENABLED)
        if (value.mode == Mode.HeadTracking) byte = byte.withBitSet(BIT_HEAD_TRACKING_ENABLED)
        if (value.offload) byte = byte.withBitSet(BIT_SPATIALIZER_OFFLOAD)

        return byteArrayOf(byte)
    }
}
