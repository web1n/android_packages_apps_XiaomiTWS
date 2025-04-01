package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.getLowByte

data class MMARequest(
    val opCode: Byte,
    val opCodeSN: Byte,
    val data: ByteArray,
    val needReceive: Boolean = true
) {

    fun toBytes() = ByteArray(5 + data.size + 4).apply {
        // header
        this[0] = 0xFE.toByte()
        this[1] = 0xDC.toByte()
        this[2] = 0xBA.toByte()

        this[3] = (if (needReceive) 0b1_1_000_000 else 0b1_0_000_000).toByte()
        this[4] = opCode

        val length = data.size + 1 // add opCodeSN
        this[5] = length.getHighByte()
        this[6] = length.getLowByte()

        this[7] = opCodeSN
        System.arraycopy(data, 0, this, 8, data.size)

        // footer
        this[this.size - 1] = 0xEF.toByte()
    }

}
