package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.SummaryProvider
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.ConfigData
import org.lineageos.xiaomi_tws.mma.ConfigData.NoiseCancellationMode.Mode
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationList

class NoiseCancellationListController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<MultiSelectListPreference, Set<String>, ConfigData.NoiseCancellationList>
        (preferenceKey, device) {

    private enum class Position { Left, Right; }

    override val config = NoiseCancellationList

    private val position: Position = preferenceKey.split("_".toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()[0]
        .lowercase()
        .let { name -> Position.entries.find { it.name.lowercase() == name }!! }

    override fun preInitView(preference: MultiSelectListPreference) {
        super.preInitView(preference)

        preference.entryValues = Mode.entries
            .map { it.name }
            .toTypedArray()
        preference.entries = Mode.entries
            .map { modeToString(preference.context, it) }
            .toTypedArray()
        preference.summaryProvider = SummaryProvider<MultiSelectListPreference> { preference ->
            preference.values.mapNotNull { value ->
                val index = preference.entryValues.indexOf(value)
                if (index >= 0) preference.entries[index] else null
            }.joinToString(", ")
        }
    }

    override fun postUpdateValue(preference: MultiSelectListPreference) {
        val modes = when (position) {
            Position.Left -> value?.left
            Position.Right -> value?.right
        }
        if (modes == null) return

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

    override fun preferenceValueToValue(value: Set<String>): ConfigData.NoiseCancellationList {
        require(value.size >= 2) {
            "Require at least two modes selected, got: ${value.size}"
        }
        val modes = value.map { Mode.valueOf(it) }

        return when (position) {
            Position.Left -> ConfigData.NoiseCancellationList(modes, null)
            Position.Right -> ConfigData.NoiseCancellationList(null, modes)
        }
    }
}
