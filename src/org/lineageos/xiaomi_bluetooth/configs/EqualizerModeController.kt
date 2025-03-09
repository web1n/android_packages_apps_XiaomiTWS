package org.lineageos.xiaomi_bluetooth.configs

import android.content.Context
import android.util.Log
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_BASS_BOOST
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_DEFAULT
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_HARMAN
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_TREBLE_BOOST
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOCAL_ENHANCE
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOLUME_BOOST
import org.lineageos.xiaomi_bluetooth.R

class EqualizerModeController(
    context: Context,
    preferenceKey: String
) : ListController(context, preferenceKey) {

    override val configId = XIAOMI_MMA_CONFIG_EQUALIZER_MODE
    override val expectedConfigLength = 1

    override val configStates: Set<ConfigState>
        get() {
            val vidPid = ((vid ?: 0) shl 16) or (pid ?: 0)
            val supportedModes =
                DEVICE_SUPPORTED_STATES[vidPid] ?: return setOf(defaultState)
            if (DEBUG) Log.d(TAG, "Supported modes: ${supportedModes.joinToString()}")

            return CONFIG_STATES.filter {
                supportedModes.contains(it.configValue[0].toInt())
            }.toSet()
        }

    override val defaultState = CONFIG_STATES.first()

    companion object {
        private val TAG = EqualizerModeController::class.java.simpleName
        private const val DEBUG = true

        private val DEVICE_SUPPORTED_STATES = mapOf(
            0x2717_5035 to setOf(0, 1, 5, 6, 11, 12),
            0x2717_503B to setOf(0, 5, 6, 12),
            0x2717_506A to setOf(0, 1, 5, 6),
            0x2717_506B to setOf(0, 1, 5, 6),
            0x2717_506C to setOf(0, 1, 5, 6, 10),
            0x2717_506D to setOf(0, 1, 5, 6, 10),
            0x2717_506F to setOf(0, 1, 5, 6, 10),
            0x2717_5075 to setOf(0, 1, 5, 6),
            0x2717_507F to setOf(0, 1, 6),
            0x2717_5080 to setOf(0, 1, 6),
            0x2717_5081 to setOf(1, 6, 10, 11, 13, 14),
            0x2717_5082 to setOf(1, 6, 10, 11, 13, 14),
            0x2717_5088 to setOf(0, 1, 5, 6, 7),
            0x2717_5089 to setOf(0, 1, 5, 6, 7),
            0x2717_508A to setOf(0, 1, 5, 6, 10),
            0x2717_508B to setOf(0, 1, 5, 6, 10),
            0x2717_5095 to setOf(0, 1, 5, 6),
            0x2717_509A to setOf(0, 1, 5, 6, 7),
            0x2717_509B to setOf(0, 1, 5, 6, 7),
            0x2717_509C to setOf(0, 1, 5, 6, 7),
            0x2717_509D to setOf(0, 1, 5, 6, 10),
            0x2717_509E to setOf(0, 1, 5, 6, 10),
            0x2717_509F to setOf(0, 1, 5, 6, 10),
            0x2717_50A0 to setOf(0, 1, 5, 6, 10),
            0x2717_50AF to setOf(0, 1, 5, 6, 10),
            0x5A4D_EA03 to setOf(0, 1, 5, 6),
            0x5A4D_EA0D to setOf(0, 1, 5, 6),
            0x5A4D_EA0E to setOf(0, 1, 5, 6),
            0x5A4D_EA0F to setOf(0, 1, 5, 6)
        )

        private val CONFIG_STATES = setOf(
            ConfigState(
                byteArrayOf(XIAOMI_MMA_CONFIG_EQUALIZER_MODE_DEFAULT),
                R.string.equalizer_mode_default
            ),
            ConfigState(
                byteArrayOf(XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOCAL_ENHANCE),
                R.string.equalizer_mode_vocal_enhance
            ),
            ConfigState(
                byteArrayOf(XIAOMI_MMA_CONFIG_EQUALIZER_MODE_BASS_BOOST),
                R.string.equalizer_mode_bass_boost
            ),
            ConfigState(
                byteArrayOf(XIAOMI_MMA_CONFIG_EQUALIZER_MODE_TREBLE_BOOST),
                R.string.equalizer_mode_treble_boost
            ),
            ConfigState(
                byteArrayOf(XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOLUME_BOOST),
                R.string.equalizer_mode_volume_boost
            ),
            ConfigState(
                byteArrayOf(XIAOMI_MMA_CONFIG_EQUALIZER_MODE_HARMAN),
                R.string.equalizer_mode_harman
            )
        )
    }
}
