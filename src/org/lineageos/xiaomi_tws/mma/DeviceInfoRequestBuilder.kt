package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_INFO_SET_IN_EAR_DETECT
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_BATTERY
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_UBOOT_VERSION
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_VERSION
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_VID_PID
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_INFO
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_INFO
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt

class DeviceInfoRequestBuilder {
    companion object {
        private fun <T> createGetDeviceInfoRequest(
            mask: Int,
            expectedSize: Int,
            dataProcessor: (MMAResponse) -> T
        ): MMARequestBuilder<T> {
            val request = MMARequest(
                XIAOMI_MMA_OPCODE_GET_DEVICE_INFO,
                byteArrayOf(0x00, 0x00, 0x00, (1 shl mask).toByte())
            )

            return MMARequestBuilder(request) { response ->
                validateDeviceInfoResponse(response.data, mask, expectedSize)
                dataProcessor(response)
            }
        }

        private fun createSetDeviceInfoRequest(
            config: Byte,
            bytes: ByteArray,
        ): MMARequestBuilder<Boolean> {
            val requestData = buildList<Byte> {
                add((bytes.size + 1).toByte())
                add(config)
                addAll(bytes.toList())
            }.toByteArray()

            val request = MMARequest(XIAOMI_MMA_OPCODE_SET_DEVICE_INFO, requestData)
            return MMARequestBuilder(request) { it.ok }
        }

        private fun validateDeviceInfoResponse(
            data: ByteArray,
            expectedMask: Int,
            expectedSize: Int
        ) {
            require(data.size == expectedSize) {
                "Response data length mismatch. Expected: $expectedSize, Actual: ${data.size}"
            }
            require(data[0].toInt() == data.size - 1) {
                "Response config data length mismatch. Expected: ${data[0]}, Actual: ${data.size - 1}"
            }
            require(data[1].toInt() == expectedMask) {
                "Response config data mask mismatch. Expected: $expectedMask, Actual: ${data[1]}"
            }
        }

        fun batteryInfo(): MMARequestBuilder<Earbuds> {
            return createGetDeviceInfoRequest(XIAOMI_MMA_MASK_GET_BATTERY, 5) {
                Earbuds.fromBytes(it.data[2], it.data[3], it.data[4])
            }
        }

        fun softwareVersion(): MMARequestBuilder<String> {
            return createGetDeviceInfoRequest(XIAOMI_MMA_MASK_GET_VERSION, 4) {
                Integer.toHexString(bytesToInt(it.data[2], it.data[3]))
            }
        }

        fun vidPid(): MMARequestBuilder<Pair<Int, Int>> {
            return createGetDeviceInfoRequest(XIAOMI_MMA_MASK_GET_VID_PID, 6) {
                val vendorId = bytesToInt(it.data[2], it.data[3])
                val productId = bytesToInt(it.data[4], it.data[5])
                vendorId to productId
            }
        }

        fun disableHeadsetInEarDetect(): MMARequestBuilder<Boolean> {
            return createSetDeviceInfoRequest(XIAOMI_MMA_INFO_SET_IN_EAR_DETECT, byteArrayOf(0x01))
        }
    }

}
