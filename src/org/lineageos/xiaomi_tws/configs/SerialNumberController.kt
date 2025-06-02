package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.configs.SerialNumber

class SerialNumberController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<Preference, Nothing, String>(preferenceKey, device) {

    override val config = SerialNumber()

    override fun postUpdateValue(preference: Preference) {
        if (value == null) return

        preference.summary = value
    }

    override fun preferenceValueToValue(value: Nothing): String {
        throw NotImplementedError()
    }

}
