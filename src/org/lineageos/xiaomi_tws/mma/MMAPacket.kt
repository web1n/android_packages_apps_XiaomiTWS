package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.getHighByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.getLowByte
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

sealed class MMAPacket protected constructor(
    val type: Type,
    open val needReply: Boolean,
    open val opCode: Byte,
    open var opCodeSN: Byte,
    open val data: ByteArray
) {
    enum class Type(internal val value: Int) { Request(0x80), Response(0x00) }
    enum class Status(internal val value: Int) { Ok(0x00), Unknown(0xFF) }

    class Request(
        override val opCode: Byte,
        override val data: ByteArray,
        override var opCodeSN: Byte = 0,
        override var needReply: Boolean = true
    ) : MMAPacket(Type.Request, needReply, opCode, opCodeSN, data)

    class Response(
        override val opCode: Byte,
        override var opCodeSN: Byte,
        val status: Status,
        override val data: ByteArray
    ) : MMAPacket(Type.Response, false, opCode, opCodeSN, data) {

        val ok = status == Status.Ok

        companion object {
            fun reply(request: Request, status: Status, data: ByteArray): Response {
                return Response(request.opCode, request.opCodeSN, status, data)
            }
        }
    }

    fun toBytes(): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // header
        outputStream.write(0xFE)
        outputStream.write(0xDC)
        outputStream.write(0xBA)

        outputStream.write((type.value + if (needReply) 0x40 else 0x00))
        outputStream.write(opCode.toInt())

        if (this is Request) {
            val length = data.size + 1 // add opCodeSN
            outputStream.write(byteArrayOf(length.getHighByte(), length.getLowByte()))

            outputStream.write(opCodeSN.toInt())
        } else if (this is Response) {
            val length = data.size + 2 // add opCodeSN & status
            outputStream.write(byteArrayOf(length.getHighByte(), length.getLowByte()))

            outputStream.write(status.value.toInt())
            outputStream.write(opCodeSN.toInt())
        }

        // data
        outputStream.write(data)

        // footer
        outputStream.write(0xEF)

        return outputStream.toByteArray()
    }

    override fun toString(): String {
        return "MMAPacket{type=${type.name}, needReply=$needReply, " +
                "opCode=$opCode, opCodeSN=$opCodeSN, data=${data.toHexString()}}"
    }

    companion object {
        fun fromPacket(packet: ByteArray): MMAPacket {
            val stream = ByteArrayInputStream(packet)
            require(stream.available() > 3) { "Packet too short" }

            val byte1 = stream.read()
            val type = if ((byte1 and 0x80) == 0) Type.Response else Type.Request
            val needReply = (byte1 and 0x40) != 0
            val opCode = stream.read().toByte()
            val parameterLength = bytesToInt(stream.read().toByte(), stream.read().toByte())
            require(parameterLength == stream.available()) { "Packet size not valid" }

            return when (type) {
                Type.Request -> {
                    val opCodeSN = stream.read().toByte()
                    val data = stream.readAllBytes()

                    Request(opCode, data, opCodeSN, needReply)
                }

                Type.Response -> {
                    val status = stream.read().let { status ->
                        Status.entries.find { it.value == status } ?: Status.Unknown
                    }
                    val opCodeSN = stream.read().toByte()
                    val data = stream.readAllBytes()

                    Response(opCode, opCodeSN, status, data)
                }
            }
        }
    }
}
