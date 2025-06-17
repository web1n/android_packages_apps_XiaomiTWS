package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.SwitchPreference
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.HeadsetManager.Companion.SUPPORT_XIAOMI_AT_COMMAND
import org.lineageos.xiaomi_tws.utils.SettingsUtils

class AutoConnectDeviceController(preferenceKey: String, device: BluetoothDevice) :
    BaseConfigController<SwitchPreference>(preferenceKey, device),
    BaseConfigController.OnPreferenceChangeListener<SwitchPreference, Boolean> {

    override suspend fun initData(manager: MMAManager) {
        if (!SUPPORT_XIAOMI_AT_COMMAND) {
            throw NotImplementedError("Xiaomi AT command not supported")
        }
    }

    override fun preInitView(preference: SwitchPreference) {
        super.preInitView(preference)

        if (!SettingsUtils.getInstance(preference.context).enableBleScan) {
            preference.isEnabled = false
            preference.setSummary(R.string.auto_connect_device_summary_ble_scan_disabled)
            return
        }
    }

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
