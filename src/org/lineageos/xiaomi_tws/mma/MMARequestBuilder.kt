package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_BATTERY
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_UBOOT_VERSION
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_VERSION
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_MASK_GET_VID_PID
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_INFO
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.getLowByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.toVersionString
import java.nio.ByteBuffer

open class MMARequestBuilder<T>(
    internal var request: MMARequest,
    internal var handler: ((MMAResponse) -> T)
) {
    companion object {
        fun batteryInfo(): MMARequestBuilder<Earbuds> {
            return createDeviceInfoRequest(XIAOMI_MMA_MASK_GET_BATTERY, 5) {
                Earbuds.fromBytes(it.device.address, it.data[2], it.data[3], it.data[4])
            }
        }

        fun uBootVersion(): MMARequestBuilder<String> {
            return createDeviceInfoRequest(XIAOMI_MMA_MASK_GET_UBOOT_VERSION, 4) {
                bytesToInt(it.data[2], it.data[3]).toVersionString()
            }
        }

        fun softwareVersion(): MMARequestBuilder<String> {
            return createDeviceInfoRequest(XIAOMI_MMA_MASK_GET_VERSION, 6) {
                bytesToInt(it.data[2], it.data[3]).toVersionString()
            }
        }

        fun vidPid(): MMARequestBuilder<Pair<Int, Int>> {
            return createDeviceInfoRequest(XIAOMI_MMA_MASK_GET_VID_PID, 6) {
                val vendorId = bytesToInt(it.data[2], it.data[3])
                val productId = bytesToInt(it.data[4], it.data[5])
                vendorId to productId
            }
        }

        private fun <T> createDeviceInfoRequest(
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

        fun getConfig(config: Int): MMARequestBuilder<ByteArray> {
            val requestData = encodeConfigIds(intArrayOf(config))
            val request = MMARequest(XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG, requestData)

            return MMARequestBuilder(request) { response ->
                parseConfigResponse(response.data, config)
            }
        }

        private fun parseConfigResponse(data: ByteArray, expectedConfig: Int): ByteArray {
            val buffer = ByteBuffer.wrap(data)

            require(buffer.remaining() >= 4) { "Response data too short: ${buffer.remaining()}" }

            val length = buffer.get().toInt()
            require(length >= 2 && length <= buffer.remaining() + 1) {
                "Invalid config data length: $length"
            }

            val actualConfig = bytesToInt(buffer.get(), buffer.get())
            require(actualConfig == expectedConfig) {
                "Config key mismatch. Expected: $expectedConfig, Actual: $actualConfig"
            }

            return ByteArray(length - 2).apply {
                buffer.get(this, 0, size)
            }
        }

        fun setConfig(config: Int, value: ByteArray): MMARequestBuilder<Boolean> {
            val requestData = buildList<Byte> {
                add((value.size + 2).toByte())
                add(config.getHighByte())
                add(config.getLowByte())
                addAll(value.toList())
            }.toByteArray()

            val request = MMARequest(XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG, requestData)
            return MMARequestBuilder(request) { it.ok }
        }

        private fun encodeConfigIds(configs: IntArray): ByteArray {
            return ByteArray(configs.size * 2).apply {
                configs.forEachIndexed { index, config ->
                    this[index * 2] = config.getHighByte()
                    this[index * 2 + 1] = config.getLowByte()
                }
            }
        }
    }
}
