package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder

abstract class SwitchController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<TwoStatePreference, Boolean>(preferenceKey, device) {

    abstract override val config: ConfigRequestBuilder<Boolean>

    override fun preInitView(preference: TwoStatePreference) {
        preference.isSelectable = false
    }

    override fun postInitView(preference: TwoStatePreference) {
        preference.isSelectable = true
    }

    override fun postUpdateValue(preference: TwoStatePreference) {
        if (value == null) return

        preference.isChecked = value!!
    }

}
