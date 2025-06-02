package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.SummaryProvider
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationList
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationList.Mode
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationList.Position

class NoiseCancellationListController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<MultiSelectListPreference, Map<Position, List<Mode>>>(preferenceKey, device),
    BaseConfigController.OnPreferenceChangeListener<MultiSelectListPreference> {

    override val config = NoiseCancellationList()

    private val position: Position = preferenceKey.split("_".toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()[0]
        .lowercase()
        .let { name ->
            Position.entries.find { it.name.lowercase() == name }!!
        }

    override fun preInitView(preference: MultiSelectListPreference) {
        super.preInitView(preference)

        preference.entryValues = Mode.entries
            .map { it.name }
            .toTypedArray()
        preference.entries = Mode.entries
            .map { modeToString(preference.context, it) }
            .toTypedArray()
        preference.summaryProvider = object : SummaryProvider<MultiSelectListPreference> {
            override fun provideSummary(preference: MultiSelectListPreference): CharSequence {
                return preference.values.mapNotNull { value ->
                    val index = preference.entryValues.indexOf(value)
                    if (index >= 0) preference.entries[index] else null
                }.joinToString(", ")
            }
        }
    }

    override fun postUpdateValue(preference: MultiSelectListPreference) {
        if (value == null) return

        val modes = value!![position] ?: emptySet()
        preference.values = modes.map { it.name }.toSet()
    }

    private fun modeToString(context: Context, mode: Mode): String {
        val stringRes = when (mode) {
            Mode.Off -> R.string.noise_cancellation_mode_off
            Mode.On -> R.string.noise_cancellation_mode_on
            Mode.Transparency -> R.string.noise_cancellation_mode_transparency
        }

        return context.getString(stringRes)
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: MultiSelectListPreference,
        newValue: Any
    ): Boolean {
        require((newValue as Set<*>).size >= 2) {
            "Require at least two modes selected, got: ${value!!.size}"
        }

        val newConfigValue = mapOf(position to newValue.map { Mode.valueOf(it as String) })

        return super.onPreferenceChange(manager, preference, newConfigValue)
    }

}
