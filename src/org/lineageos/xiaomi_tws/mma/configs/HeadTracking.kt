package org.lineageos.xiaomi_tws.mma.configs

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.HeadTracking
import org.lineageos.xiaomi_tws.mma.ConfigData.HeadTracking.Rotation

object HeadTracking : Config<HeadTracking>() {

    override val configId = 0x0021
    override val validBytesLength = 13

    override fun decode(bytes: ByteArray): HeadTracking {
        val buffer = ByteBuffer
            .wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)

        val rotation = Rotation(
            buffer.getFloat(1),
            buffer.getFloat(5),
            buffer.getFloat(9)
        )
        return HeadTracking(rotation)
    }

}
