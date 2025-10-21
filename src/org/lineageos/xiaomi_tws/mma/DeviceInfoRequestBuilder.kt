package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_INFO_SET_IN_EAR_DETECT
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_BATTERY
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_VERSION
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_VID_PID
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_INFO
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_INFO
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.MMAPacket.Request
import org.lineageos.xiaomi_tws.mma.MMAPacket.Response
import org.lineageos.xiaomi_tws.mma.MMAPacketBuilder.RequestBuilder
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt

class DeviceInfoRequestBuilder {
    companion object {
        private fun <T> createGetDeviceInfoRequest(
            mask: Int,
            expectedSize: (Int) -> Boolean,
            dataProcessor: (ByteArray) -> T
        ): RequestBuilder<T> {
            val request = Request(
                XIAOMI_MMA_OPCODE_GET_DEVICE_INFO,
                byteArrayOf(0x00, 0x00, 0x00, (1 shl mask).toByte())
            )

            return RequestBuilder(request) { response ->
                val content = validateDeviceInfoResponse(response.data, mask, expectedSize)
                dataProcessor(content)
            }
        }

        private fun createSetDeviceInfoRequest(
            config: Byte,
            bytes: ByteArray,
        ): RequestBuilder<Boolean> {
            val requestData = buildList<Byte> {
                add((bytes.size + 1).toByte())
                add(config)
                addAll(bytes.toList())
            }.toByteArray()

            val request = Request(XIAOMI_MMA_OPCODE_SET_DEVICE_INFO, requestData)
            return RequestBuilder(request) { it.ok }
        }

        private fun validateDeviceInfoResponse(
            data: ByteArray,
            expectedMask: Int,
            expectedSize: (Int) -> Boolean
        ): ByteArray {
            require(data.size >= 2) {
                "Response config data too short. Actual: ${data.size}"
            }
            val length = data[0].toInt()
            val mask = data[1].toInt()
            val content = data.drop(2).toByteArray()

            require(length == data.size - 1) {
                "Response config data length mismatch. Expected: $length, Actual: ${data.size - 1}"
            }
            require(mask == expectedMask) {
                "Response config data mask mismatch. Expected: $expectedMask, Actual: $mask"
            }
            require(expectedSize(content.size)) {
                "Response data length mismatch. Actual: ${content.size}"
            }

            return content
        }

        fun batteryInfo(): RequestBuilder<Earbuds> {
            return createGetDeviceInfoRequest(XIAOMI_MMA_MASK_GET_BATTERY, 3::equals) {
                Earbuds.fromBytes(it[0], it[1], it[2])
            }
        }

        fun softwareVersion(): RequestBuilder<String> {
            return createGetDeviceInfoRequest(XIAOMI_MMA_MASK_GET_VERSION, setOf(2, 4)::contains) {
                Integer.toHexString(bytesToInt(it[0], it[1]))
            }
        }

        fun vidPid(): RequestBuilder<Pair<Int, Int>> {
            return createGetDeviceInfoRequest(XIAOMI_MMA_MASK_GET_VID_PID, 4::equals) {
                val vendorId = bytesToInt(it[0], it[1])
                val productId = bytesToInt(it[2], it[3])
                vendorId to productId
            }
        }

        fun disableHeadsetInEarDetect(): RequestBuilder<Boolean> {
            return createSetDeviceInfoRequest(XIAOMI_MMA_INFO_SET_IN_EAR_DETECT, byteArrayOf(0x01))
        }
    }

}
