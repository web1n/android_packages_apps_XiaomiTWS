package org.lineageos.xiaomi_tws.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SettingsUtils private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.createDeviceProtectedStorageContext())

    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    private fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    var enableSystemIntegration: Boolean
        get() = getBoolean(KEY_ENABLE_SYSTEM_INTEGRATION, true)
        set(value) = putBoolean(KEY_ENABLE_SYSTEM_INTEGRATION, value)

    var enableNotification: Boolean
        get() = getBoolean(KEY_ENABLE_NOTIFICATION, true)
        set(value) = putBoolean(KEY_ENABLE_NOTIFICATION, value)

    var enableBleScan: Boolean
        get() = getBoolean(KEY_ENABLE_BLE_SCAN, true)
        set(value) = putBoolean(KEY_ENABLE_BLE_SCAN, value)

    fun isAutoSwitchDeviceEnabled(device: BluetoothDevice): Boolean {
        return getBoolean("${KEY_ENABLE_AUTO_SWITCH_DEVICE}_${device.address}", false)
    }

    fun setAutoSwitchDeviceEnabled(device: BluetoothDevice, enabled: Boolean) {
        putBoolean("${KEY_ENABLE_AUTO_SWITCH_DEVICE}_${device.address}", enabled)
    }

    fun isSwitchDeviceAllowed(device: BluetoothDevice): Boolean {
        return getBoolean("${KEY_ALLOW_SWITCH_DEVICE}_${device.address}", false)
    }

    fun setSwitchDeviceAllowed(device: BluetoothDevice, enabled: Boolean) {
        putBoolean("${KEY_ALLOW_SWITCH_DEVICE}_${device.address}", enabled)
    }

    fun isAutoConnectDeviceEnabled(device: BluetoothDevice): Boolean {
        return getBoolean("${KEY_ENABLE_AUTO_CONNECT_DEVICE}_${device.address}", false)
    }

    fun setAutoConnectDeviceEnabled(device: BluetoothDevice, enabled: Boolean) {
        putBoolean("${KEY_ENABLE_AUTO_CONNECT_DEVICE}_${device.address}", enabled)
    }

    companion object {
        private const val KEY_ENABLE_SYSTEM_INTEGRATION = "enable_system_integration"
        private const val KEY_ENABLE_NOTIFICATION = "enable_notification"
        private const val KEY_ENABLE_BLE_SCAN = "enable_ble_scan"

        private const val KEY_ENABLE_AUTO_SWITCH_DEVICE = "enable_auto_switch_device"
        private const val KEY_ALLOW_SWITCH_DEVICE = "allow_switch_device"
        private const val KEY_ENABLE_AUTO_CONNECT_DEVICE = "enable_auto_connect_device"

        @Volatile
        private var INSTANCE: SettingsUtils? = null

        fun getInstance(context: Context): SettingsUtils {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsUtils(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
