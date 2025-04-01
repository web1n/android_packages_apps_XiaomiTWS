package org.lineageos.xiaomi_tws.mma

import android.util.Log
import org.lineageos.xiaomi_tws.EarbudsConstants
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt

open class MMAResponse protected constructor(
    val opCode: Byte,
    val opCodeSN: Byte,
    val status: Byte,
    val data: ByteArray
) {

    val ok = status == EarbudsConstants.XIAOMI_MMA_RESPONSE_STATUS_OK

    override fun toString(): String {
        return "MMAResponse{opCode=$opCode, opCodeSN=$opCodeSN, status=$status, data=${data.contentToString()}}"
    }

    companion object {
        private val TAG = MMAResponse::class.java.simpleName
        private const val DEBUG = true

        fun fromPacket(packet: ByteArray): MMAResponse? {
            if (packet.size < 6) {
                if (DEBUG) Log.d(TAG, "fromPacket: packet length < 6")
                return null
            }

            val type = if ((packet[0].toInt() and 0x80) == 0) 0 else 1
            val responseFlag = if ((packet[0].toInt() and 0x40) == 0) 0 else 1
            val opCode = packet[1]
            val parameterLength = bytesToInt(packet[2], packet[3])
            val status = packet[4]
            val opCodeSN = packet[5]

            if (type != 0 || responseFlag != 0) {
                if (DEBUG) Log.d(TAG, "fromPacket: type or responseFlag not 0")
                //            return null;
            }
            if (parameterLength < 2 || packet.size - 4 != parameterLength) {
                if (DEBUG) Log.d(TAG, "fromPacket: parameterLength not equal")
                return null
            }
            if (status.toInt() != 0) {
                if (DEBUG) Log.d(TAG, "fromPacket: status not success")
            }

            val data = ByteArray(parameterLength - 2).apply {
                System.arraycopy(packet, 6, this, 0, parameterLength - 2)
            }

            return MMAResponse(opCode, opCodeSN, status, data).also {
                if (DEBUG) Log.d(TAG, "fromPacket: $it")
            }
        }
    }
}
