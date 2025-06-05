package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.SwitchPreference
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.SettingsUtils

class AutoConnectDeviceController(preferenceKey: String, device: BluetoothDevice) :
    BaseConfigController<SwitchPreference>(preferenceKey, device),
    BaseConfigController.OnPreferenceChangeListener<SwitchPreference, Boolean> {

    override suspend fun initData(manager: MMAManager) {}

    override fun postUpdateValue(preference: SwitchPreference) {
        preference.isChecked = SettingsUtils.getInstance(preference.context)
            .isAutoConnectDeviceEnabled(device)
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: SwitchPreference,
        newValue: Boolean
    ): Boolean {
        SettingsUtils.getInstance(preference.context)
            .setAutoConnectDeviceEnabled(device, newValue)

        return true
    }

}
