package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.BooleanData
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

abstract class BooleanConfig<T : BooleanData> : Config<BooleanData>(), Config.Encoder<BooleanData> {

    abstract val enableBytes: ByteArray
    abstract val disableBytes: ByteArray
    abstract fun create(enabled: Boolean): T

    override fun decode(bytes: ByteArray): BooleanData {
        val enabled = when {
            bytes.contentEquals(enableBytes) -> true
            bytes.contentEquals(disableBytes) -> false
            else -> throw NotImplementedError("Not supported value: ${bytes.toHexString()}")
        }

        return create(enabled)
    }

    override fun encode(value: BooleanData): ByteArray {
        return when (value.enabled) {
            true -> enableBytes
            false -> disableBytes
        }
    }
}
