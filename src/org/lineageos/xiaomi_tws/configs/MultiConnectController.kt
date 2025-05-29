package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.mma.configs.MultiConnect

class MultiConnectController(preferenceKey: String, device: BluetoothDevice) :
    SwitchController(preferenceKey, device) {

    override val config = MultiConnect()

}
