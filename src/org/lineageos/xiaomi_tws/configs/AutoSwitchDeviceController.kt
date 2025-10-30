package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.disableHeadsetInEarDetect
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.AutoAnswerCalls
import org.lineageos.xiaomi_tws.utils.SettingsUtils

class AutoSwitchDeviceController(preferenceKey: String, device: BluetoothDevice) :
    BaseConfigController<TwoStatePreference>(preferenceKey, device),
    BaseConfigController.OnPreferenceChangeListener<TwoStatePreference, Boolean> {

    override suspend fun initData(manager: MMAManager) {
        if (!isInEarGestureSupported(manager)) {
            throw NotImplementedError("In-ear gesture not supported")
        }
    }

    override fun postUpdateValue(preference: TwoStatePreference) {
        preference.isChecked = SettingsUtils.getInstance(preference.context)
            .isAutoSwitchDeviceEnabled(device)
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: TwoStatePreference,
        newValue: Boolean
    ): Boolean {
        // disable headset builtin in ear detect
        manager.request(device, disableHeadsetInEarDetect())

        SettingsUtils.getInstance(preference.context)
            .setAutoSwitchDeviceEnabled(device, newValue)
        return true
    }

    private suspend fun isInEarGestureSupported(manager: MMAManager): Boolean {
        return manager
            .runCatching { request(device, AutoAnswerCalls().get()) }
            .isSuccess
    }

}
