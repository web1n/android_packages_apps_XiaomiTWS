package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

abstract class BooleanConfig(configId: Int) : ConfigRequestBuilder<Boolean>(configId) {

    abstract val enableBytes: ByteArray
    abstract val disableBytes: ByteArray

    override fun bytesToValue(bytes: ByteArray): Boolean {
        return when {
            bytes.contentEquals(enableBytes) -> true
            bytes.contentEquals(disableBytes) -> false
            else -> throw NotImplementedError("Not supported value: ${bytes.toHexString()}")
        }
    }

    override fun valueToBytes(value: Boolean): ByteArray {
        return when (value) {
            true -> enableBytes
            false -> disableBytes
        }
    }
}
