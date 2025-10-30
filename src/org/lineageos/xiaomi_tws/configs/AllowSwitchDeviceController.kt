package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_tws.utils.HeadsetManager
import org.lineageos.xiaomi_tws.utils.HeadsetManager.Companion.SUPPORT_XIAOMI_AT_COMMAND
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.SettingsUtils

class AllowSwitchDeviceController(preferenceKey: String, device: BluetoothDevice) :
    BaseConfigController<TwoStatePreference>(preferenceKey, device),
    BaseConfigController.OnPreferenceChangeListener<TwoStatePreference, Boolean> {

    override suspend fun initData(manager: MMAManager) {
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            throw NotImplementedError("Xiaomi AT command not supported")
        }
    }

    override fun postUpdateValue(preference: TwoStatePreference) {
        preference.isChecked = SettingsUtils.getInstance(preference.context)
            .isSwitchDeviceAllowed(device)
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: TwoStatePreference,
        newValue: Boolean
    ): Boolean {
        SettingsUtils.getInstance(preference.context)
            .setSwitchDeviceAllowed(device, newValue)
        HeadsetManager.getInstance(preference.context)
            .sendSwitchDeviceAllowed(device, newValue)

        return true
    }

}
