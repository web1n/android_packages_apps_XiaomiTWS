package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.utils.ByteUtils
import org.lineageos.xiaomi_tws.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.getLowByte
import java.nio.ByteBuffer

abstract class ConfigRequestBuilder<T>(val configId: Int) {

    abstract fun bytesToValue(bytes: ByteArray): T
    abstract fun valueToBytes(value: T): ByteArray

    fun get(): MMARequestBuilder<T> {
        val requestData = encodeConfigIds(intArrayOf(configId))
        val request = MMARequest(XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG, requestData)

        return MMARequestBuilder(request) { response ->
            val bytes = parseConfigResponse(response.data, configId)
            if (isNotSupportedBytes(bytes)) {
                throw NotImplementedError()
            }

            bytesToValue(bytes)
        }
    }

    fun set(value: T): MMARequestBuilder<Boolean> {
        val bytes = valueToBytes(value)

        val requestData = buildList<Byte> {
            add((bytes.size + 2).toByte())
            add(configId.getHighByte())
            add(configId.getLowByte())
            addAll(bytes.toList())
        }.toByteArray()

        val request = MMARequest(XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG, requestData)
        return MMARequestBuilder(request) { it.ok }
    }

    companion object {
        private const val VALUE_FEATURE_NOT_SUPPORTED: Byte = -1

        private fun isNotSupportedBytes(bytes: ByteArray): Boolean {
            return bytes.size == 1 && bytes[0] == VALUE_FEATURE_NOT_SUPPORTED
        }

        private fun encodeConfigIds(configs: IntArray): ByteArray {
            return ByteArray(configs.size * 2).apply {
                configs.forEachIndexed { index, config ->
                    this[index * 2] = config.getHighByte()
                    this[index * 2 + 1] = config.getLowByte()
                }
            }
        }

        private fun parseConfigResponse(data: ByteArray, expectedConfig: Int): ByteArray {
            val buffer = ByteBuffer.wrap(data)

            require(buffer.remaining() >= 4) { "Response data too short: ${buffer.remaining()}" }

            val length = buffer.get().toInt()
            require(length >= 2 && length <= buffer.remaining() + 1) {
                "Invalid config data length: $length"
            }

            val actualConfig = ByteUtils.bytesToInt(buffer.get(), buffer.get())
            require(actualConfig == expectedConfig) {
                "Config key mismatch. Expected: $expectedConfig, Actual: $actualConfig"
            }

            return ByteArray(length - 2).apply {
                buffer.get(this, 0, size)
            }
        }
    }
}
