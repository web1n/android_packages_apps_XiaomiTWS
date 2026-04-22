package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.FindEarbuds
import org.lineageos.xiaomi_tws.mma.ConfigData.FindEarbuds.Position

object FindEarbuds : Config<FindEarbuds>(), Config.Encoder<FindEarbuds> {
    private const val VALUE_ENABLED: Byte = 0x01
    private const val VALUE_DISABLED: Byte = 0x00

    override val configId = 0x0009
    override val validBytesLength = 2

    override fun decode(bytes: ByteArray): FindEarbuds {
        val enabled = bytes[0] == VALUE_ENABLED
        val positions = Position.entries
            .filter { (it.value and bytes[1].toInt()) != 0 }

        return FindEarbuds(enabled, positions)
    }

    override fun encode(value: FindEarbuds): ByteArray {
        return byteArrayOf(
            if (value.enabled) VALUE_ENABLED else VALUE_DISABLED,
            value.positions.fold(0) { acc, position -> acc or position.value }.toByte()
        )
    }
}
