package org.lineageos.xiaomi_tws.mma

import android.util.Log
import java.nio.ByteBuffer
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.mma.MMAPacket.Request
import org.lineageos.xiaomi_tws.mma.MMAPacketBuilder.RequestBuilder
import org.lineageos.xiaomi_tws.mma.configs.*
import org.lineageos.xiaomi_tws.utils.ByteUtils
import org.lineageos.xiaomi_tws.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.getLowByte

abstract class Config<T : ConfigData> {
    interface Encoder<T : ConfigData> {
        fun encode(value: T): ByteArray
    }

    abstract val configId: Int
    open val validBytesLength: Int? = null

    abstract fun decode(bytes: ByteArray): T

    fun get(): RequestBuilder<T> {
        val requestData = encodeConfigIds(intArrayOf(configId))
        val request = Request(XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG, requestData)

        return RequestBuilder(request) { response ->
            val (configId, value) = parseConfigResponse(response.data)
            val result = decode(configId, value)

            @Suppress("UNCHECKED_CAST")
            return@RequestBuilder result as Any as T
        }
    }

    fun set(value: T): RequestBuilder<Boolean> {
        val bytes = encode(value)

        val requestData = buildList {
            add((bytes.size + 2).toByte())
            add(configId.getHighByte())
            add(configId.getLowByte())
            addAll(bytes.toList())
        }.toByteArray()

        val request = Request(XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG, requestData)

        return RequestBuilder(request) { it.ok }
    }

    companion object {
        private const val DEBUG = true
        private val TAG = Config::class.java.simpleName

        private const val VALUE_FEATURE_NOT_SUPPORTED: Byte = -1

        private val CONFIGS_MAP = mapOf(
            ConfigData.AutoAnswerCalls::class.java to AutoAnswerCalls,
            ConfigData.EqualizerMode::class.java to EqualizerMode,
            ConfigData.FindEarbuds::class.java to FindEarbuds,
            ConfigData.Gesture::class.java to Gesture,
            ConfigData.InEarState::class.java to InEarState,
            ConfigData.MultiConnect::class.java to MultiConnect,
            ConfigData.NoiseCancellationList::class.java to NoiseCancellationList,
            ConfigData.NoiseCancellationMode::class.java to NoiseCancellationMode,
            ConfigData.SerialNumber::class.java to SerialNumber,
        )
        private val CONFIGS_BY_ID = CONFIGS_MAP.values.associateBy { it.configId }

        private fun encodeConfigIds(configs: IntArray): ByteArray {
            return ByteArray(configs.size * 2).apply {
                configs.forEachIndexed { index, config ->
                    this[index * 2] = config.getHighByte()
                    this[index * 2 + 1] = config.getLowByte()
                }
            }
        }

        private fun parseConfigResponse(data: ByteArray): Pair<Int, ByteArray> {
            val buffer = ByteBuffer.wrap(data)

            require(buffer.remaining() >= 3) {
                "Response data too short: ${buffer.remaining()}"
            }

            val length = buffer.get().toInt()
            require(length >= 2 && length <= buffer.remaining() + 1) {
                "Invalid config data length: $length"
            }

            val configId = ByteUtils.bytesToInt(buffer.get(), buffer.get())
            val value = ByteArray(length - 2).apply { buffer.get(this, 0, size) }
            return configId to value
        }

        fun decode(configId: Int, value: ByteArray): ConfigData {
            if (isNotSupportedBytes(value)) {
                throw NotImplementedError("Config $configId is not supported on this device")
            }

            val config = CONFIGS_BY_ID[configId]
                ?: throw IllegalArgumentException("Unknown config id: $configId")
            if (config.validBytesLength != null && config.validBytesLength != value.size) {
                throw IllegalArgumentException("Invalid length for $config: expected ${config.validBytesLength}, got ${value.size}")
            }

            val result = config.decode(value)

            if (DEBUG) Log.d(TAG, "decode: bytes=${value.contentToString()}, result=$result")
            return result
        }

        fun encode(data: ConfigData): ByteArray {
            val config = CONFIGS_MAP[data::class.java]
                ?: throw IllegalArgumentException("Unknown config data: $data")
            if (config !is Encoder<*>) {
                throw IllegalArgumentException("Config type $config $data is not encodable")
            }

            @Suppress("UNCHECKED_CAST")
            val encoder = config as Encoder<ConfigData>
            val result = encoder.encode(data)

            if (DEBUG) Log.d(TAG, "encode: data=$data result=${result.contentToString()}")
            return result
        }

        private fun isNotSupportedBytes(bytes: ByteArray): Boolean {
            return bytes.size == 1 && bytes[0] == VALUE_FEATURE_NOT_SUPPORTED
        }
    }
}
