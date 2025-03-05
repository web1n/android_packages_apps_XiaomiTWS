package org.lineageos.xiaomi_bluetooth.settings.configs

import android.content.Context
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_bluetooth.mma.MMADevice

abstract class SwitchController(
    context: Context,
    preferenceKey: String
) : ConfigController(context, preferenceKey) {

    protected abstract val enabledState: ConfigState
    protected abstract val disabledState: ConfigState

    protected open val isEnabled: Boolean
        get() = enabledState.configValue.contentEquals(configValue)

    override fun displayPreference(preference: Preference) {
        super.displayPreference(preference)

        (preference as TwoStatePreference).apply {
            setSummaryOn(enabledState.summaryResId)
            setSummaryOff(disabledState.summaryResId)
        }
    }

    override val summary: String?
        get() = configValue?.let {
            context.getString(
                (if (isEnabled) enabledState else disabledState).summaryResId
            )
        }

    override fun saveConfig(device: MMADevice, value: Any): Boolean {
        if (value !is Boolean) {
            return super.saveConfig(device, value)
        }

        val configValue = if (value) {
            enabledState.configValue
        } else {
            disabledState.configValue
        }

        return super.saveConfig(device, configValue)
    }

    override fun updateValue(preference: Preference) {
        (preference as TwoStatePreference).isChecked = isEnabled
    }

}
