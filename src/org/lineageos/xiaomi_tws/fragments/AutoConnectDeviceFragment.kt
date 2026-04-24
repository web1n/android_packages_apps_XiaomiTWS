package org.lineageos.xiaomi_tws.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import org.lineageos.xiaomi_tws.headset.CommandData.Notify.AutoSwitchDevice
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.features.DeviceBattery
import org.lineageos.xiaomi_tws.headset.CommandData
import org.lineageos.xiaomi_tws.headset.HeadsetManager.HeadsetDeviceListener

import java.util.concurrent.ConcurrentHashMap

class AutoConnectDeviceFragment : BaseDeviceListFragment() {

    override val layoutResId: Int = R.xml.auto_connect_device
    override val deviceListCategoryKey: String = "auto_connect_device_list"
    override val emptyStatePrefKey: String = "auto_connect_device_empty"
    override val preferenceKeyPrefix: String = "auto_connect_device"
    override val logTag: String = TAG

    private val newValueDevice = ConcurrentHashMap.newKeySet<BluetoothDevice>()
    private val headsetListener = object : HeadsetDeviceListener {
        override fun onDeviceConnected(device: BluetoothDevice) {}
        override fun onDeviceDisconnected(device: BluetoothDevice) {}
        override fun onBatteryChanged(device: BluetoothDevice, battery: DeviceBattery) {}

        override fun onDeviceChanged(device: BluetoothDevice, value: CommandData) {
            if (value !is AutoSwitchDevice || !isDeviceEnabled(device)) return

            fixWrongEnableValue(device, value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headsetManager.registerDeviceListener(headsetListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        headsetManager.unregisterDeviceListener(headsetListener)
    }

    override fun isDeviceSupported(device: BluetoothDevice): Boolean {
        return device.isConnected && settingsUtils.isAutoConnectDeviceSupported(device)
    }

    override fun isDeviceEnabled(device: BluetoothDevice): Boolean {
        return settingsUtils.isAutoConnectDeviceEnabled(device)
    }

    override fun setDeviceEnabled(device: BluetoothDevice, enabled: Boolean) {
        settingsUtils.setAutoConnectDeviceEnabled(device, enabled)

        val newEnableValue = settingsUtils.isAutoConnectDeviceNewValue(device)
        setEnabled(device, enabled, newEnableValue)
    }

    @SuppressLint("MissingPermission")
    private fun setEnabled(device: BluetoothDevice, enabled: Boolean, newValue: Boolean) {
        headsetManager.sendATCommand(device, AutoSwitchDevice(enabled, newValue))
    }

    private fun fixWrongEnableValue(device: BluetoothDevice, value: AutoSwitchDevice) {
        if (newValueDevice.remove(device)) {
            Log.d(TAG, "${device.address} reports auto switch ${value.enabled} with new value")
            if (value.enabled) settingsUtils.setAutoConnectDeviceNewValue(device, true)
        } else if (!value.enabled) {
            Log.w(TAG, "${device.address} reports auto switch disabled")
            newValueDevice.add(device)
            setEnabled(device, enabled = true, newValue = true)
        }
    }

    companion object {
        private val TAG = AutoConnectDeviceFragment::class.java.simpleName
    }
}
