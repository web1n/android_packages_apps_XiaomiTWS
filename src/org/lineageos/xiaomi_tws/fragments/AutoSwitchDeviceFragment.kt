package org.lineageos.xiaomi_tws.fragments

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.headset.HeadsetManager
import org.lineageos.xiaomi_tws.R

class AutoSwitchDeviceFragment : BaseDeviceListFragment() {

    override val layoutResId: Int = R.xml.auto_switch_device
    override val deviceListCategoryKey: String = "auto_switch_device_list"
    override val emptyStatePrefKey: String = "auto_switch_device_empty"
    override val preferenceKeyPrefix: String = "auto_switch_device"
    override val logTag: String = TAG

    override fun isDeviceSupported(device: BluetoothDevice): Boolean {
        // return settingsUtils.isAutoSwitchDeviceSupported(device)
        return false // TODO
    }

    override fun isDeviceEnabled(device: BluetoothDevice): Boolean {
        // return settingsUtils.isAutoSwitchDeviceEnabled(device)
        return false // TODO
    }

    override fun setDeviceEnabled(headsetManager: HeadsetManager, device: BluetoothDevice, enabled: Boolean) {
        // settingsUtils.setAutoSwitchDeviceEnabled(device, enabled)
        // TODO
    }

    companion object {
        private val TAG = AutoSwitchDeviceFragment::class.java.simpleName
    }
}
