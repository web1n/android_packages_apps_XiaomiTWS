package org.lineageos.xiaomi_tws.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter

class SettingsUtils private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun <T> get(key: String, defaultValue: T): T {
        val value = when (defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue)
            is String -> sharedPreferences.getString(key, defaultValue)
            else -> return defaultValue
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun <T : Any> put(key: String, value: T) {
        sharedPreferences.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
            apply()
        }
    }

    var enableSystemIntegration: Boolean
        get() = get(KEY_ENABLE_SYSTEM_INTEGRATION, true)
        set(value) = put(KEY_ENABLE_SYSTEM_INTEGRATION, value)

    var enableNotification: Boolean
        get() = get(KEY_ENABLE_NOTIFICATION, true)
        set(value) = put(KEY_ENABLE_NOTIFICATION, value)

    var enableBleScan: Boolean
        get() = get(KEY_ENABLE_BLE_SCAN, false)
        set(value) = put(KEY_ENABLE_BLE_SCAN, value)

    fun isAutoSwitchDeviceEnabled(device: BluetoothDevice): Boolean {
        return get("${KEY_ENABLE_AUTO_SWITCH_DEVICE}_${device.address}", false)
    }

    fun setAutoSwitchDeviceEnabled(device: BluetoothDevice, enabled: Boolean) {
        put("${KEY_ENABLE_AUTO_SWITCH_DEVICE}_${device.address}", enabled)
    }

    fun isSwitchDeviceAllowed(device: BluetoothDevice): Boolean {
        return get("${KEY_ALLOW_SWITCH_DEVICE}_${device.address}", false)
    }

    fun setSwitchDeviceAllowed(device: BluetoothDevice, enabled: Boolean) {
        put("${KEY_ALLOW_SWITCH_DEVICE}_${device.address}", enabled)
    }

    fun isAutoConnectDeviceEnabled(device: BluetoothDevice): Boolean {
        return get("${KEY_ENABLE_AUTO_CONNECT_DEVICE}_${device.address}", false)
    }

    fun setAutoConnectDeviceEnabled(device: BluetoothDevice, enabled: Boolean) {
        put("${KEY_ENABLE_AUTO_CONNECT_DEVICE}_${device.address}", enabled)
    }

    fun getDeviceForAccountKey(accountKey: String): BluetoothDevice? {
        sharedPreferences.all.forEach { (key, value) ->
            if (key.startsWith(KEY_ACCOUNT_KEY) && value == accountKey) {
                val address = key.removePrefix("${KEY_ACCOUNT_KEY}_")

                return getBluetoothAdapter().getRemoteDevice(address)
            }
        }

        return null
    }

    fun getAccountKeyForDevice(device: BluetoothDevice): String? {
        return get("${KEY_ACCOUNT_KEY}_${device.address}", "").let {
            if (it.isEmpty()) null else it
        }
    }

    fun putAccountKeyForDevice(device: BluetoothDevice, key: String) {
        put("${KEY_ACCOUNT_KEY}_${device.address}", key)
    }

    companion object {
        private const val KEY_ENABLE_SYSTEM_INTEGRATION = "enable_system_integration"
        private const val KEY_ENABLE_NOTIFICATION = "enable_notification"
        private const val KEY_ENABLE_BLE_SCAN = "enable_ble_scan"

        private const val KEY_ENABLE_AUTO_SWITCH_DEVICE = "enable_auto_switch_device"
        private const val KEY_ALLOW_SWITCH_DEVICE = "allow_switch_device"
        private const val KEY_ENABLE_AUTO_CONNECT_DEVICE = "enable_auto_connect_device"
        private const val KEY_ACCOUNT_KEY = "account_key"

        @Volatile
        private var INSTANCE: SettingsUtils? = null

        fun getInstance(context: Context): SettingsUtils {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsUtils(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
