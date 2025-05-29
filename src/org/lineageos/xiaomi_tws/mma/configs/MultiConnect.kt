package org.lineageos.xiaomi_tws.mma.configs

class MultiConnect() : BooleanConfig(CONFIG_ID) {

    override val enableBytes = CONFIG_VALUE_ENABLED
    override val disableBytes = CONFIG_VALUE_DISABLED

    companion object {
        private const val CONFIG_ID = 0x0004

        private val CONFIG_VALUE_ENABLED = byteArrayOf(0x01)
        private val CONFIG_VALUE_DISABLED = byteArrayOf(0x00)
    }
}
