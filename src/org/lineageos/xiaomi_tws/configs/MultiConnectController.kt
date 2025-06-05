package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.SwitchPreference
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_tws.mma.configs.MultiConnect

class MultiConnectController(preferenceKey: String, device: BluetoothDevice) :
    SwitchController(preferenceKey, device) {

    override val config = MultiConnect()

    override fun postUpdateValue(preference: TwoStatePreference) {
        super.postUpdateValue(preference)

        preference.parent?.findPreference<SwitchPreference>("allow_switch_device")
            ?.isEnabled = !preference.isChecked
    }
}
