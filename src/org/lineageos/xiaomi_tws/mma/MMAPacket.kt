package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

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

    override fun toString(): String {
        return "MMAPacket{type=${type.name}, needReply=$needReply, " +
                "opCode=$opCode, opCodeSN=$opCodeSN, data=${data.toHexString()}}"
    }
}
