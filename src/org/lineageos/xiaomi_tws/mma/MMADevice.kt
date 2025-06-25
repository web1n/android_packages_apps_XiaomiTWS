package org.lineageos.xiaomi_tws.mma

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_SPP_UUIDS
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.getLowByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
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

    fun sendPacket(packet: MMAPacket) {
        if (DEBUG) Log.d(TAG, "sendPacket: $packet")
        checkConnected()
        val stream = socket!!.outputStream

        try {
            // header
            stream.write(PACKET_HEADER)

            stream.write((packet.type.value + if (packet.needReply) 0x40 else 0x00))
            stream.write(packet.opCode.toInt())

            if (packet is MMAPacket.Request) {
                val length = packet.data.size + 1 // add opCodeSN
                stream.write(byteArrayOf(length.getHighByte(), length.getLowByte()))

                stream.write(packet.opCodeSN.toInt())
            } else if (packet is MMAPacket.Response) {
                val length = packet.data.size + 2 // add opCodeSN & status
                stream.write(byteArrayOf(length.getHighByte(), length.getLowByte()))

                stream.write(packet.status.value.toInt())
                stream.write(packet.opCodeSN.toInt())
            }

            // data
            stream.write(packet.data)

            // footer
            stream.write(PACKET_END)

            stream.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send request", e)
            throw e
        }
    }

    fun getPacket(): MMAPacket? {
        findPacketStart()
        val packet = readPacket()
        checkPacketEnd()

        return packet
    }

    private fun findPacketStart() {
        checkConnected()
        val inputStream = socket!!.inputStream

        val buffer = ByteArray(3)
        var filled = 0

        while (isConnected) {
            val current = inputStream.read().toByte()

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

    private fun checkPacketEnd() {
        checkConnected()
        val stream = socket!!.inputStream

        if (stream.available() < PACKET_END.size) {
            Log.w(TAG, "Invalid packet end")
            return
        }

        val endBytes = stream.readNBytes(PACKET_END.size)
        if (!endBytes.contentEquals(PACKET_END)) {
            Log.w(TAG, "Invalid packet end: ${endBytes.toHexString()}")
        }
    }

    private fun readPacket(): MMAPacket? {
        checkConnected()

        val stream = socket!!.inputStream
        if (stream.available() < 5) {
            Log.w(TAG, "Not valid packet data length: ${stream.available()}")
            return null
        }

        val byte1 = stream.read()
        val type = if ((byte1 and 0x80) == 0) MMAPacket.Type.Response else MMAPacket.Type.Request
        val needReply = (byte1 and 0x40) != 0
        val opCode = stream.read().toByte()
        val parameterLength = bytesToInt(stream.read().toByte(), stream.read().toByte())
        if (stream.available() < parameterLength) {
            Log.w(TAG, "Packet size not valid, need $parameterLength but ${stream.available()}")
            return null
        }

        return when (type) {
            MMAPacket.Type.Request -> {
                val opCodeSN = stream.read().toByte()
                val data = stream.readNBytes(parameterLength - 1)

                MMAPacket.Request(opCode, data, opCodeSN, needReply)
            }

            MMAPacket.Type.Response -> {
                val status = stream.read().let { status ->
                    MMAPacket.Status.entries.find { it.value == status } ?: MMAPacket.Status.Unknown
                }
                val opCodeSN = stream.read().toByte()
                val data = stream.readNBytes(parameterLength - 2)

                MMAPacket.Response(opCode, opCodeSN, status, data)
            }
        }
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
