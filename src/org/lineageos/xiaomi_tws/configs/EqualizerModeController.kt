package org.lineageos.xiaomi_tws.configs

import android.content.Context
import android.util.Log
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_BASS_BOOST
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_DEFAULT
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_HARMAN
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_TREBLE_BOOST
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOCAL_ENHANCE
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOLUME_BOOST
import org.lineageos.xiaomi_tws.R

class EqualizerModeController(
    context: Context,
    preferenceKey: String
) : ListController(context, preferenceKey) {

    data class EqualizerDevice(val vendorId: Int, val productId: Int, val supportedModes: Set<Int>)

    override val configId = XIAOMI_MMA_CONFIG_EQUALIZER_MODE
    override val expectedConfigLength = 1

    override val configStates: Set<ConfigState>
        get() {
            val supportedModes = DEVICE_SUPPORTED_MODES.find {
                it.vendorId == vid && it.productId == pid
            }?.supportedModes ?: return setOf(defaultState)
            if (DEBUG) Log.d(TAG, "Supported modes: ${supportedModes.joinToString()}")

            return CONFIG_STATES.filter {
                supportedModes.contains(it.configValue[0].toInt())
            }.toSet()
        }

    override val defaultState = CONFIG_STATES.first()

    companion object {
        private val TAG = EqualizerModeController::class.java.simpleName
        private const val DEBUG = true

        private val DEVICE_SUPPORTED_MODES = arrayOf(
            EqualizerDevice(0x2717, 0x5035, setOf(0, 1, 5, 6, 11, 12)),
            EqualizerDevice(0x2717, 0x503B, setOf(0, 5, 6, 12)),
            EqualizerDevice(0x2717, 0x506A, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x506B, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x506C, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x506D, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x506F, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x5075, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x507F, setOf(0, 1, 6)),
            EqualizerDevice(0x2717, 0x5080, setOf(0, 1, 6)),
            EqualizerDevice(0x2717, 0x5081, setOf(1, 6, 10, 11, 13, 14)),
            EqualizerDevice(0x2717, 0x5082, setOf(1, 6, 10, 11, 13, 14)),
            EqualizerDevice(0x2717, 0x5088, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x5089, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x508A, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x508B, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x5095, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x509A, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x509B, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x509C, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x509D, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x509E, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x509F, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x50A0, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x50AB, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50AC, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50AD, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50AF, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x50B4, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50B9, setOf(0, 1, 5, 6, 7, 10)),
            EqualizerDevice(0x5A4D, 0xEA03, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x5A4D, 0xEA0D, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x5A4D, 0xEA0E, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x5A4D, 0xEA0F, setOf(0, 1, 5, 6))
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
