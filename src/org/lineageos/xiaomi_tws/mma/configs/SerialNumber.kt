package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.SerialNumber

object SerialNumber : Config<SerialNumber>() {

    override val configId = 0x0027
    override val validBytesLength = 20

    override fun decode(bytes: ByteArray): SerialNumber {
        val result = String(bytes, Charsets.UTF_8)
        return SerialNumber(result)
    }
}
