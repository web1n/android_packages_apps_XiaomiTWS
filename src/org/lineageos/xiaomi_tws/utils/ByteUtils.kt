package org.lineageos.xiaomi_tws.utils

import java.io.ByteArrayInputStream

object ByteUtils {

    fun Byte.isBitSet(bit: Int) = (toInt() shr bit) and 1 != 0

    fun bytesToInt(highByte: Byte, lowByte: Byte, bigEndian: Boolean = true): Int {
        val high = highByte.toInt() and 0xFF
        val low = lowByte.toInt() and 0xFF

        return if (bigEndian) {
            (high shl 8) or low
        } else {
            (low shl 8) or high
        }
    }

    fun Int.getHighByte(): Byte {
        return (this shr 8).toByte()
    }

    fun Int.getLowByte(): Byte {
        return (this and 0xFF).toByte()
    }

    fun ByteArray.toHexString(separator: String = ""): String {
        return joinToString(separator) { "%02X".format(it) }
    }

    fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) {
            "Hex string must have an even length"
        }

        val bytes = ByteArray(length / 2)
        for (i in indices step 2) {
            val high = this[i].digitToIntOrNull(16)
            val low = this[i + 1].digitToIntOrNull(16)
            check(high != null && low != null) {
                "Invalid hex character at position $i"
            }

            bytes[i / 2] = (high shl 4 or low).toByte()
        }

        return bytes
    }

    fun parseTLVMap(data: ByteArray, singleByteTag: Boolean): Map<Int, ByteArray> {
        val map = HashMap<Int, ByteArray>()
        val stream = ByteArrayInputStream(data)

        while (stream.available() > 1) {
            val length = stream.read()
            if (stream.available() < length) break

            val tlv = stream.readNBytes(length)
            if (singleByteTag) {
                if (tlv.size < 1) break
                map[tlv[0].toInt()] = tlv.drop(1).toByteArray()
            } else {
                if (tlv.size < 2) break
                map[bytesToInt(tlv[0], tlv[1])] = tlv.drop(2).toByteArray()
            }
        }
        return map
    }

}
