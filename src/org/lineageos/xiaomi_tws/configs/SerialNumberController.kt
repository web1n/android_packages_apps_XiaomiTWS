package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.ConfigData
import org.lineageos.xiaomi_tws.mma.configs.SerialNumber

class SerialNumberController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<Preference, Nothing, ConfigData.SerialNumber>(preferenceKey, device) {

    override val config = SerialNumber

    override fun postUpdateValue(preference: Preference) {
        value?.let {
            preference.summary = it.value
        }
    }

    override fun preferenceValueToValue(value: Nothing): ConfigData.SerialNumber {
        throw NotImplementedError()
    }

}
