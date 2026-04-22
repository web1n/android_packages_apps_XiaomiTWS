package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_tws.mma.ConfigData.BooleanData
import org.lineageos.xiaomi_tws.mma.configs.BooleanConfig

abstract class SwitchController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<TwoStatePreference, Boolean, BooleanData>(preferenceKey, device) {

    abstract override val config: BooleanConfig<*>

    override fun postUpdateValue(preference: TwoStatePreference) {
        value?.let {
            preference.isChecked = it.enabled
        }
    }

    override fun preferenceValueToValue(value: Boolean): BooleanData {
        return config.create(value)
    }

}
