package org.lineageos.xiaomi_tws.mma

import com.xiaomi.aivsbluetoothsdk.impl.BluetoothAuth.getEncryptedAuthCheckData
import com.xiaomi.aivsbluetoothsdk.impl.BluetoothAuth.getRandomAuthCheckData
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_NOTIFY_AUTH
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_SEND_AUTH
import org.lineageos.xiaomi_tws.mma.MMAPacket.Request
import org.lineageos.xiaomi_tws.mma.MMAPacketBuilder.RequestBuilder
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

object AuthRequest {

    fun requireDeviceSideAuth(): RequestBuilder<Unit> {
        val randomData = getRandomAuthCheckData()
        val realCode = getEncryptedAuthCheckData(randomData)

        val request = Request(XIAOMI_MMA_OPCODE_SEND_AUTH, byteArrayOf(0x01, *randomData))
        return RequestBuilder(request) { response ->
            require(byteArrayOf(0x01, *realCode).contentEquals(response.data)) {
                "Required auth result: ${realCode.toHexString()}, but: ${response.data.toHexString()}"
            }
        }
    }

    fun sendAuthStatus(): RequestBuilder<Unit> {
        val packet = Request(XIAOMI_MMA_OPCODE_NOTIFY_AUTH, byteArrayOf(0x01, 0x00))

        return RequestBuilder(packet) { response ->
            require(byteArrayOf(0x01).contentEquals(response.data))
        }
    }

    fun calcAuthRequestResponseData(packet: Request): ByteArray {
        return if (packet.data.size == 17) {
            val randomCode = packet.data.drop(1).toByteArray()
            byteArrayOf(0x01, *getEncryptedAuthCheckData(randomCode))
        } else {
            byteArrayOf()
        }
    }

}
