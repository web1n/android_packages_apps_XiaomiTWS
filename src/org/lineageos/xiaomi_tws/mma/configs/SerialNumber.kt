package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder
import java.nio.charset.StandardCharsets

class SerialNumber : ConfigRequestBuilder<String>(CONFIG_ID) {

    override fun bytesToValue(bytes: ByteArray): String {
        if (bytes.size != VALID_BYTES_LENGTH) {
            throw NotImplementedError()
        }

        return String(bytes, StandardCharsets.UTF_8)
    }

    override fun valueToBytes(value: String): ByteArray {
        throw NotImplementedError()
    }

    companion object {
        private const val CONFIG_ID = 0x0027
        private const val VALID_BYTES_LENGTH = 20
    }

}
