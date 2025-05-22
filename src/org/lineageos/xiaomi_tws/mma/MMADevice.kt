package org.lineageos.xiaomi_tws.mma

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_SPP_UUIDS
import java.io.ByteArrayOutputStream
import java.io.IOException

internal class MMADevice(val device: BluetoothDevice) {

    private var socket: BluetoothSocket? = null
    private var opCodeSN: Byte? = null

    val isDeviceConnected: Boolean
        get() = device.isConnected

    val isConnected: Boolean
        get() = socket?.isConnected == true

    private fun checkConnected() {
        check(isConnected) { "MMADevice not connected" }
    }

    val newOpCodeSN: Byte
        get() {
            if (opCodeSN == null) {
                opCodeSN = (Byte.MIN_VALUE..Byte.MAX_VALUE).random().toByte()
            }

            opCodeSN = (opCodeSN!! + 1).toByte()
            return opCodeSN!!
        }

    fun sendRequest(request: MMARequest) {
        checkConnected()

        val data = request.toBytes()
        if (DEBUG) Log.d(TAG, "sendRequest: ${data.contentToString()}")

        try {
            socket!!.outputStream.apply {
                write(data)
                flush()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send request", e)
            throw e
        }
    }

    fun getResponsePacket(): ByteArray {
        findPacketStart()

        return readPacketData()
    }

    private fun findPacketStart() {
        val buffer = ByteArray(3)
        var filled = 0

        while (true) {
            checkConnected()

            val current = readSingleByte()

            if (filled < 3) {
                buffer[filled] = current
                filled++
            } else {
                buffer[0] = buffer[1]
                buffer[1] = buffer[2]
                buffer[2] = current
            }

            // Check for start sequence FE DC BA
            if (filled == 3 && buffer.contentEquals(PACKET_HEADER)) {
                return
            }
        }
    }

    private fun readPacketData(): ByteArray {
        val packetData = ByteArrayOutputStream()

        while (true) {
            checkConnected()

            val current = readSingleByte()

            // Check for end sequence EF
            if (current == PACKET_END[0]) {
                return packetData.toByteArray()
            }

            packetData.write(current.toInt())
        }
    }

    private fun readSingleByte(): Byte {
        val inputStream = socket!!.inputStream
        val value = inputStream.read()

        if (value == -1) {
            throw IOException("End of stream reached")
        }
        return value.toByte()
    }

    @RequiresPermission(
        allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT]
    )
    fun connect() {
        if (DEBUG) Log.d(TAG, "connect")
        if (isConnected) {
            if (DEBUG) Log.d(TAG, "Already connected")
            return
        }
        if (!isDeviceConnected) {
            throw IOException("Device ${device.address} not connected")
        }

        // cancel discovery before connect
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        connectSocket()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectSocket() {
        for (uuid in XIAOMI_SPP_UUIDS) {
            if (!isDeviceConnected) {
                throw IOException("Device not connected")
            }
            if (DEBUG) Log.d(TAG, "createSocket: try connect uuid: $uuid")

            var tempSocket: BluetoothSocket? = null
            try {
                tempSocket = device.createInsecureRfcommSocketToServiceRecord(uuid).apply {
                    connect()
                }
                this.socket = tempSocket
                return
            } catch (e: Exception) {
                Log.w(TAG, "connectSocket: Failed to connect uuid: $uuid", e)
                tempSocket?.runCatching { close() }
            }
        }

        throw IOException("Failed to connect to any UUID")
    }

    fun close() {
        if (!isConnected) return
        if (DEBUG) Log.d(TAG, "close")

        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "close: ", e)
        } finally {
            socket = null
        }
    }

    companion object {
        private val TAG = MMADevice::class.java.simpleName
        private const val DEBUG = true

        private val PACKET_HEADER = byteArrayOf(0xFE.toByte(), 0xDC.toByte(), 0xBA.toByte())
        private val PACKET_END = byteArrayOf(0xEF.toByte())
    }
}
