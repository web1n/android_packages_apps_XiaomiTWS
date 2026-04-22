package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigData.MultiConnect

object MultiConnect : BooleanConfig<MultiConnect>() {

    override val configId = 0x0004
    override val enableBytes = byteArrayOf(0x01)
    override val disableBytes = byteArrayOf(0x00)

    override fun create(enabled: Boolean) = MultiConnect(enabled)
}
