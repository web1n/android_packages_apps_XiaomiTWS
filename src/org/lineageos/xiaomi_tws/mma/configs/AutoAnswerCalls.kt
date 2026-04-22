package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigData.AutoAnswerCalls

object AutoAnswerCalls : BooleanConfig<AutoAnswerCalls>() {

    override val configId: Int = 0x0003
    override val enableBytes = byteArrayOf(0x01)
    override val disableBytes = byteArrayOf(0x00)

    override fun create(enabled: Boolean) = AutoAnswerCalls(enabled)
}
