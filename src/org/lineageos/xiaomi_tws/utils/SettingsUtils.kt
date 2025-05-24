package org.lineageos.xiaomi_tws.utils

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

    companion object {
        private const val KEY_ENABLE_SYSTEM_INTEGRATION = "enable_system_integration"
        private const val KEY_ENABLE_NOTIFICATION = "enable_notification"

        @Volatile
        private var INSTANCE: SettingsUtils? = null

        fun getInstance(context: Context): SettingsUtils {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsUtils(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
