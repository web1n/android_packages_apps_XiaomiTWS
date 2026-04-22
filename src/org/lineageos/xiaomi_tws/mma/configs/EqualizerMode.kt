package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.EqualizerMode
import org.lineageos.xiaomi_tws.mma.ConfigData.EqualizerMode.Mode

object EqualizerMode : Config<EqualizerMode>(), Config.Encoder<EqualizerMode> {

    override val configId = 0x0007
    override val validBytesLength = 1

    override fun decode(bytes: ByteArray): EqualizerMode {
        return EqualizerMode(Mode.entries.find { it.value == bytes[0] } ?: Mode.Default)
    }

    override fun encode(value: EqualizerMode): ByteArray {
        return byteArrayOf(value.mode.value)
    }
}
