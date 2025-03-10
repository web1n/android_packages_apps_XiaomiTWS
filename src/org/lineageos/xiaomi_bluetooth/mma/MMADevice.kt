package org.lineageos.xiaomi_bluetooth.mma

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_bluetooth.EarbudsConstants
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.getLowByte
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.toVersionString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class MMADevice(val device: BluetoothDevice) : AutoCloseable {

    private var socket: BluetoothSocket? = null
    private var opCodeSN: Byte? = null

    val isConnected: Boolean
        get() = socket?.isConnected == true

    private fun checkConnected() {
        check(isConnected) { "MMADevice not connected" }
    }

    private val newOpCodeSN: Byte
        get() {
            if (opCodeSN == null) {
                opCodeSN = (Byte.MIN_VALUE..Byte.MAX_VALUE).random().toByte()
            }

            opCodeSN = (opCodeSN!! + 1).toByte()
            return opCodeSN!!
        }

    private fun sendRequest(request: MMARequest) {
        checkConnected()

        val data = request.toBytes()
        if (DEBUG) Log.d(TAG, "sendRequest: ${data.contentToString()}")

        try {
            socket!!.outputStream.apply {
                write(data)
                flush()
            }
        } catch (e: IOException) {
            Log.e(TAG, "sendReceivePacket: ", e)
            throw e
        }
    }

    private fun readResponse(): MMAResponse? {
        checkConnected()

        try {
            val responsePacket = getResponsePacket(socket!!.inputStream)

            return MMAResponse.fromPacket(responsePacket)
        } catch (e: IOException) {
            Log.e(TAG, "sendReceivePacket: ", e)
            throw e
        }
    }

    @Synchronized
    private fun sendReceive(request: MMARequest): MMAResponse? {
        checkConnected()

        sendRequest(request)

        if (!request.needReceive) {
            return null
        }

        for (i in 0..4) {
            val response = readResponse() ?: continue
            if (DEBUG) Log.d(TAG, "sendReceive: $i $response")

            if (response.opCode != request.opCode || response.opCodeSN != request.opCodeSN) {
                continue
            }
            return response
        }

        throw IllegalStateException("Unable to get mma response")
    }

    private fun getDeviceInfo(mask: Int, expectedLength: Int? = null): ByteArray {
        val request = MMARequest(
            EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_INFO, newOpCodeSN,
            byteArrayOf(0x00, 0x00, 0x00, (1 shl mask).toByte())
        )

        val response = checkNotNull(sendReceive(request)) {
            "MMAResponse is null"
        }
        check(response.data.size == expectedLength) {
            "MMAResponse data length not equal to expected. Expected: $expectedLength, Actual: ${response.data.size}"
        }
        check(response.data[0].toInt() == response.data.size - 1) {
            "Response config data length not equal to expected. Expected: ${response.data[0]}, Actual: ${response.data.size - 1}"
        }
        check(response.data[1].toInt() == mask) {
            "Response config data mask not equal to expected. Expected: $mask, Actual: ${response.data[1]}"
        }

        return response.data
    }

    val uBootVersion: String
        get() {
            val data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_UBOOT_VERSION, 4)

            val version = bytesToInt(data[2], data[3]).toVersionString()

            if (DEBUG) Log.d(TAG, "getUBootVersion: $version")
            return version
        }

    val softwareVersion: String
        get() {
            val data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_VERSION, 6)

            val primaryVersion = bytesToInt(data[2], data[3]).toVersionString()
            val secondaryVersion = bytesToInt(data[4], data[5]).toVersionString()

            if (DEBUG) Log.d(
                TAG,
                "getSoftwareVersion: primary: $primaryVersion, secondary: $secondaryVersion"
            )
            return primaryVersion
        }

    val batteryInfo: Earbuds
        get() {
            val data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_BATTERY, 5)

            val earbuds = Earbuds.fromBytes(device.address, data[2], data[3], data[4])
            check(earbuds.valid) {
                "Unable to get active earbuds battery info"
            }

            if (DEBUG) Log.d(TAG, "getBatteryInfo: $earbuds")
            return earbuds
        }

    val vidPid: Pair<Int, Int>
        get() {
            val data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_VID_PID, 6)

            val vendorId = bytesToInt(data[2], data[3])
            val productId = bytesToInt(data[4], data[5])

            if (DEBUG) Log.d(TAG, "getVidPid: vid: $vendorId, pid: $productId")
            return vendorId to productId
        }

    fun getDeviceConfig(configs: IntArray): Map<Int, ByteArray?> {
        val requestData = ByteArray(configs.size * 2).apply {
            for (i in configs.indices) {
                this[i * 2] = configs[i].getHighByte()
                this[(i * 2) + 1] = configs[i].getLowByte()
            }
        }
        if (DEBUG) Log.d(TAG, "getDeviceConfig: ${requestData.contentToString()}")

        val request = MMARequest(
            EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG,
            newOpCodeSN,
            requestData
        )
        val response = checkNotNull(sendReceive(request))

        val buffer = ByteBuffer.wrap(response.data)
        val configValues = HashMap<Int, ByteArray?>()

        while (buffer.remaining() >= 4) {
            val length = buffer.get().toInt()
            if (length < 2 || length > buffer.remaining()) {
                break
            }
            val key = bytesToInt(buffer.get(), buffer.get())
            val value = ByteArray(length - 2).apply {
                buffer.get(this, 0, size)
            }

            if (!configs.contains(key)) {
                continue
            }

            configValues[key] = value
        }

        return configValues
    }

    fun getDeviceConfig(config: Int, expectedLength: Int? = null): ByteArray {
        if (DEBUG) Log.d(TAG, "getDeviceConfig")

        val configs = getDeviceConfig(intArrayOf(config))
        val configBytes = checkNotNull(configs.getOrDefault(config, null)) {
            "Unable to get config for $config, got null"
        }
        check(expectedLength == null || configBytes.size == expectedLength) {
            "Response config data length not equal to expected. Expected: $expectedLength, Actual: ${configBytes.size}"
        }

        return configBytes
    }

    fun setDeviceConfig(configs: Map<Int, ByteArray>, needReceive: Boolean = true): Boolean {
        if (DEBUG) Log.d(TAG, "setDeviceConfig")

        val requestData = ArrayList<Byte>().apply {
            for ((key, value) in configs) {
                add((value.size + 2).toByte())

                add(key.getHighByte())
                add(key.getLowByte())

                value.forEach { add(it) }
            }
        }.toByteArray()

        val request = MMARequest(
            EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG,
            newOpCodeSN,
            requestData,
            needReceive
        )

        return sendReceive(request)?.ok != false
    }

    fun setDeviceConfig(config: Int, value: ByteArray, needReceive: Boolean = true): Boolean {
        return setDeviceConfig(hashMapOf(config to value), needReceive)
    }

    var equalizerMode: Byte
        get() {
            return getDeviceConfig(EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE, 1).let {
                val eqMode = it[0]

                if (eqMode <= EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_HARMAN) {
                    eqMode
                } else {
                    EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_DEFAULT
                }
            }
        }
        set(value) {
            setDeviceConfig(EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE, byteArrayOf(value))
        }

    val deviceSN: String
        get() {
            return String(
                getDeviceConfig(EarbudsConstants.XIAOMI_MMA_CONFIG_SN, 20),
                StandardCharsets.US_ASCII
            )
        }

    var noiseCancellationMode: Byte
        get() {
            return getDeviceConfig(
                EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE, 2
            ).let {
                val noiseCancellationMode = it[0]

                if (noiseCancellationMode <= EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY) {
                    noiseCancellationMode
                } else {
                    EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF
                }
            }
        }
        set(value) {
            setDeviceConfig(
                EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE,
                byteArrayOf(value, 0x00),
                false
            )
        }

    @RequiresPermission(
        allOf = [android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT]
    )
    fun connect() {
        if (DEBUG) Log.d(TAG, "connect")
        if (isConnected) return

        if (!device.isConnected) {
            throw IOException("device not connected")
        }

        // cancel discovery before connect
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        connectSocket()
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectSocket() {
        this.socket = checkNotNull(
            EarbudsConstants.XIAOMI_UUIDS.firstNotNullOfOrNull { uuid ->
                if (DEBUG) Log.d(TAG, "createSocket: try connect uuid: $uuid")

                device.createInsecureRfcommSocketToServiceRecord(uuid.uuid).runCatching {
                    apply { connect() }
                }.onFailure {
                    runCatching { close() }
                    Log.e(TAG, "failed to connect uuid: $uuid", it)
                }.getOrNull()
            }
        ) {
            "Failed to connect to any UUID"
        }
    }

    override fun close() {
        if (!isConnected) return
        if (DEBUG) Log.d(TAG, "close")

        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "close: ", e)
            throw e
        } finally {
            socket = null
        }
    }

    companion object {
        private val TAG = MMADevice::class.java.simpleName
        private const val DEBUG = true

        private fun getResponsePacket(inputStream: InputStream): ByteArray {
            val buffer = ByteArrayOutputStream()
            var startFound = false

            while (true) {
                val current = ByteArray(1).apply { inputStream.read(this) }[0]

                // Check for end sequence EF
                if (startFound && current == 0xEF.toByte()) {
                    return buffer.toByteArray()
                }

                buffer.write(current.toInt())

                // Check for start sequence FE DC BA
                if (!startFound && buffer.size() >= 3) {
                    val data = buffer.toByteArray()
                    if (
                        data[data.size - 3] == 0xFE.toByte()
                        && data[data.size - 2] == 0xDC.toByte()
                        && data[data.size - 1] == 0xBA.toByte()
                    ) {
                        startFound = true
                        buffer.reset()
                    }
                }
            }
        }
    }
}
