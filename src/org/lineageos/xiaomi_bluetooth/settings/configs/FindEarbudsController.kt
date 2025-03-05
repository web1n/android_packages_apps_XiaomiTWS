package org.lineageos.xiaomi_bluetooth.settings.configs

import android.content.Context
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_FIND_EARBUDS
import org.lineageos.xiaomi_bluetooth.R
import org.lineageos.xiaomi_bluetooth.mma.MMADevice

class FindEarbudsController(context: Context, preferenceKey: String) :
    SwitchController(context, preferenceKey) {

    override val enabledState = ConfigState(ENABLED_CONFIG, R.string.find_earbuds_on)
    override val disabledState = ConfigState(DISABLED_CONFIG, R.string.find_earbuds_off)

    override val configId = XIAOMI_MMA_CONFIG_FIND_EARBUDS
    override val expectedConfigLength = 2

    override val isEnabled
        get() = configValue?.get(0) == ENABLE_FLAG

    override fun saveConfig(device: MMADevice, value: Any): Boolean {
        require(value is Boolean) {
            "Invalid value type: ${value.javaClass.simpleName}"
        }

        val earbuds = device.batteryInfo
        check(earbuds.leftOrRightValid) {
            "Both earbuds disconnected"
        }

        val configValue = byteArrayOf(
            if (value) ENABLE_FLAG else DISABLE_FLAG,
            earbuds.run {
                val left = if (left.valid) LEFT_EARBUD_FLAG else 0
                val right = if (right.valid) RIGHT_EARBUD_FLAG else 0

                (left or right).toByte()
            }
        )

        return super.saveConfig(device, configValue)
    }

    companion object {
//        private val TAG = FindEarbudsController::class.java.simpleName
//        private const val DEBUG: Boolean = true

        private const val LEFT_EARBUD_FLAG = 0x01
        private const val RIGHT_EARBUD_FLAG = 0x02
        private const val ENABLE_FLAG: Byte = 0x01
        private const val DISABLE_FLAG: Byte = 0x00

        private val ENABLED_CONFIG = byteArrayOf(
            ENABLE_FLAG, (LEFT_EARBUD_FLAG or RIGHT_EARBUD_FLAG).toByte()
        )
        private val DISABLED_CONFIG = byteArrayOf(
            DISABLE_FLAG, (LEFT_EARBUD_FLAG or RIGHT_EARBUD_FLAG).toByte()
        )
    }
}
