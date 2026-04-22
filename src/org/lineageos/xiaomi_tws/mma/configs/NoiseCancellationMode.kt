package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.NoiseCancellationMode
import org.lineageos.xiaomi_tws.mma.ConfigData.NoiseCancellationMode.Mode

object NoiseCancellationMode :
    Config<NoiseCancellationMode>(), Config.Encoder<NoiseCancellationMode> {

    override val configId = 0x000B
    override val validBytesLength = 2

    override fun decode(bytes: ByteArray): NoiseCancellationMode {
        val mode = Mode.entries.find { it.value == bytes[0] } ?: Mode.Off
        return NoiseCancellationMode(mode)
    }

    override fun encode(value: NoiseCancellationMode): ByteArray {
        return byteArrayOf(value.value.value, 0x00)
    }
}
