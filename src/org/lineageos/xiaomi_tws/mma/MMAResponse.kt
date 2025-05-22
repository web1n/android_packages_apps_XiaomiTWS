package org.lineageos.xiaomi_tws.mma

import android.bluetooth.BluetoothDevice
import android.util.Log
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_RESPONSE_STATUS_OK
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt

open class MMAResponse protected constructor(
    val device: BluetoothDevice,
    val type: Int,
    val flag: Int,
    val opCode: Byte,
    val opCodeSN: Byte,
    val status: Byte,
    val data: ByteArray
) {

    val ok = status == XIAOMI_MMA_RESPONSE_STATUS_OK

    override fun toString(): String {
        return "MMAResponse{device=$device, opCode=$opCode, opCodeSN=$opCodeSN, status=$status, data=${data.contentToString()}}"
    }

    companion object {
        private val TAG = MMAResponse::class.java.simpleName
        private const val DEBUG = true

        fun fromPacket(device: BluetoothDevice, packet: ByteArray): MMAResponse {
            check(packet.size >= 6) { "packet length < 6" }

            val type = if ((packet[0].toInt() and 0x80) == 0) 0 else 1
            val flag = if ((packet[0].toInt() and 0x40) == 0) 0 else 1
            val opCode = packet[1]
            val parameterLength = bytesToInt(packet[2], packet[3])
            val status = packet[4]
            val opCodeSN = packet[5]

            if (type != 0 || flag != 0) {
                if (DEBUG) Log.d(
                    TAG,
                    "Response type or flag not 0: type $type, responseFlag: $flag"
                )
            }
            check(parameterLength >= 2 && packet.size - 4 == parameterLength) {
                "Parameter length not equal, expect $parameterLength, bug ${packet.size - 4}"
            }
            if (status.toInt() != 0) {
                Log.w(TAG, "fromPacket: status not success")
            }

            val data = ByteArray(parameterLength - 2).apply {
                System.arraycopy(packet, 6, this, 0, parameterLength - 2)
            }

            return MMAResponse(device, type, flag, opCode, opCodeSN, status, data).also {
                if (DEBUG) Log.d(TAG, "fromPacket: $it")
            }
        }
    }
}
