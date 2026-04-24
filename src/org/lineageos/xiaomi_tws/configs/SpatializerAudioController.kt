package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.preference.ListPreference
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.ConfigData
import org.lineageos.xiaomi_tws.mma.configs.SpatializerAudio
import org.lineageos.xiaomi_tws.mma.ConfigData.SpatializerAudio.Mode

class SpatializerAudioController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<ListPreference, String, ConfigData.SpatializerAudio>(preferenceKey, device) {

    override val config = SpatializerAudio

    override fun preInitView(preference: ListPreference) {
        super.preInitView(preference)

        preference.entryValues = Mode.entries
            .map { it.name }
            .toTypedArray()
        preference.entries = Mode.entries
            .map { modeToString(preference.context, it) }
            .toTypedArray()
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }

    override fun postUpdateValue(preference: ListPreference) {
        value?.let {
            // Currently only support render on device
            if (!it.offload) {
                preference.value = Mode.Off.name
            } else {
                preference.value = it.mode.name
            }
        }
    }

    override fun preferenceValueToValue(value: String): ConfigData.SpatializerAudio {
        return ConfigData.SpatializerAudio(
            Mode.valueOf(value),
            true // Currently only support render on device
        )
    }

    private fun modeToString(context: Context, mode: Mode): String {
        val stringRes = when (mode) {
            Mode.Off -> R.string.spatializer_audio_off
            Mode.On -> R.string.spatializer_audio_on
            Mode.HeadTracking -> R.string.spatializer_audio_head_tracking
        }

        return context.getString(stringRes)
    }
}
