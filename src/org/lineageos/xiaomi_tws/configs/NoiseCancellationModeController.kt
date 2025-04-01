package org.lineageos.xiaomi_tws.configs

import android.content.Context
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_ON
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY
import org.lineageos.xiaomi_tws.R

class NoiseCancellationModeController(
    context: Context,
    preferenceKey: String
) : ListController(context, preferenceKey) {

    override val configId = XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE
    override val expectedConfigLength = 2
    override val configNeedReceive = false

    override val configStates = setOf(
        ConfigState(
            byteArrayOf(XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF, 0x00),
            R.string.noise_cancellation_mode_off
        ),
        ConfigState(
            byteArrayOf(XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_ON, 0x00),
            R.string.noise_cancellation_mode_on
        ),
        ConfigState(
            byteArrayOf(XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY, 0x00),
            R.string.noise_cancellation_mode_transparency
        )
    )
    override val defaultState = configStates.first()

}
