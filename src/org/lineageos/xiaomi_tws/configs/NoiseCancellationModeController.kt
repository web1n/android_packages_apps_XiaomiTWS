package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.preference.ListPreference
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationMode
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationMode.Mode

class NoiseCancellationModeController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<ListPreference, Mode>(preferenceKey, device) {

    override val config = NoiseCancellationMode()

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
        if (value == null) return

        preference.value = value!!.name
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: ListPreference,
        newValue: Any
    ): Boolean {
        val newConfigValue = Mode.valueOf(newValue as String)

        return super.onPreferenceChange(manager, preference, newConfigValue)
    }

    private fun modeToString(context: Context, mode: Mode): String {
        val stringRes = when (mode) {
            Mode.Off -> R.string.noise_cancellation_mode_off
            Mode.On -> R.string.noise_cancellation_mode_on
            Mode.Transparency -> R.string.noise_cancellation_mode_transparency
        }

        return context.getString(stringRes)
    }

}
