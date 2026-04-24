package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.preference.ListPreference
import org.lineageos.xiaomi_tws.features.DeviceModel
import org.lineageos.xiaomi_tws.features.DeviceModel.Companion.DEFAULT_EQUALIZER_MODES
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.ConfigData
import org.lineageos.xiaomi_tws.mma.ConfigData.EqualizerMode.Mode
import org.lineageos.xiaomi_tws.mma.configs.EqualizerMode

class EqualizerModeController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<ListPreference, String, ConfigData.EqualizerMode>(preferenceKey, device) {

    override val config = EqualizerMode

    override fun preInitView(preference: ListPreference) {
        super.preInitView(preference)

        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }

    override fun postInitView(preference: ListPreference) {
        super.postInitView(preference)

        val supportedModes = model?.equalizerModes ?: DEFAULT_EQUALIZER_MODES
        preference.entries = supportedModes
            .map { modeToString(preference.context, it) }
            .toTypedArray()
        preference.entryValues = supportedModes
            .map { it.name }
            .toTypedArray()
    }

    override fun postUpdateValue(preference: ListPreference) {
        value?.let {
            preference.value = it.mode.name
        }
    }

    override fun preferenceValueToValue(value: String): ConfigData.EqualizerMode {
        return ConfigData.EqualizerMode(Mode.valueOf(value))
    }

    private fun modeToString(context: Context, mode: Mode): String {
        val stringRes = when (mode) {
            Mode.Default -> R.string.equalizer_mode_default
            Mode.VocalEnhance -> R.string.equalizer_mode_vocal_enhance
            Mode.BassBoost -> R.string.equalizer_mode_bass_boost
            Mode.TrebleBoost -> R.string.equalizer_mode_treble_boost
            Mode.VolumeBoost -> R.string.equalizer_mode_volume_boost
            Mode.Custom -> R.string.equalizer_mode_custom
            Mode.Classic -> R.string.equalizer_mode_classic
            Mode.Legendary -> R.string.equalizer_mode_legendary
            Mode.SoothingBoost -> R.string.equalizer_mode_soothing_boost
            Mode.Harman -> R.string.equalizer_mode_harman
            Mode.HarmanMaster -> R.string.equalizer_mode_harman_master
            Mode.Standard -> R.string.equalizer_mode_standard
            Mode.Outdoor -> R.string.equalizer_mode_outdoor
            Mode.UnderWater -> R.string.equalizer_mode_under_water
            Mode.BalancedListening -> R.string.equalizer_mode_balanced_listening
        }

        return context.getString(stringRes)
    }
}
