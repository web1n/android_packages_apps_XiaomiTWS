package org.lineageos.xiaomi_tws.mma

import org.lineageos.xiaomi_tws.mma.MMAPacket.Request
import org.lineageos.xiaomi_tws.mma.MMAPacket.Response

abstract class MMAPacketBuilder() {
    abstract val packet: MMAPacket

    open class RequestBuilder<T>(
        internal val request: Request,
        internal val handler: ((Response) -> T)
    ) : MMAPacketBuilder() {
        override val packet = request
    }

    open class RequestNoResponseBuilder(val request: Request) : MMAPacketBuilder() {
        override val packet = request
    }

    open class ResponseBuilder(val response: Response) : MMAPacketBuilder() {
        override val packet = response
    }

}
