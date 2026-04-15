package org.lineageos.xiaomi_tws.fragments

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.headset.HeadsetManager
import org.lineageos.xiaomi_tws.headset.commands.notify.AutoSwitchDevice
import org.lineageos.xiaomi_tws.R

class AutoConnectDeviceFragment : BaseDeviceListFragment() {

    override val layoutResId: Int = R.xml.auto_connect_device
    override val deviceListCategoryKey: String = "auto_connect_device_list"
    override val emptyStatePrefKey: String = "auto_connect_device_empty"
    override val preferenceKeyPrefix: String = "auto_connect_device"
    override val logTag: String = TAG

    override fun isDeviceSupported(device: BluetoothDevice): Boolean {
        // return settingsUtils.isAutoConnectDeviceSupported(device)
        return false // TODO
    }

    override fun isDeviceEnabled(device: BluetoothDevice): Boolean {
        // return settingsUtils.isAutoConnectDeviceEnabled(device)
        return false // TODO
    }

    override fun setDeviceEnabled(headsetManager: HeadsetManager, device: BluetoothDevice, enabled: Boolean) {
        // settingsUtils.setAutoConnectDeviceEnabled(device, enabled)
        // headsetManager.sendATCommand(device, AutoSwitchDevice(enabled))
        // TODO
    }

    companion object {
        private val TAG = AutoConnectDeviceFragment::class.java.simpleName
    }
}
