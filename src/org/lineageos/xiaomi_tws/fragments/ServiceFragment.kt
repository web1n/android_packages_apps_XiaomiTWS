package org.lineageos.xiaomi_tws.fragments

import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.xiaomi_tws.PersistentApplication.Companion.enableSystemIntegration
import org.lineageos.xiaomi_tws.R

class ServiceFragment : SettingsBasePreferenceFragment() {

    private val systemIntegrationPreference: SwitchPreferenceCompat
        get() = findPreference(KEY_SYSTEM_INTEGRATION_STATUS)!!

    private val systemIntegrationDisabledPreference: Preference
        get() = findPreference(KEY_SYSTEM_INTEGRATION_DISABLED)!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.service)
    }

    override fun onResume() {
        super.onResume()

        if (DEBUG) Log.d(TAG, "onResume: enableSystemIntegration: $enableSystemIntegration")
        systemIntegrationPreference.isChecked = enableSystemIntegration
        systemIntegrationDisabledPreference.isVisible = !enableSystemIntegration
    }

    companion object {
        private val TAG = ServiceFragment::class.java.simpleName
        private const val DEBUG = true

        private const val KEY_SYSTEM_INTEGRATION_STATUS = "system_integration_status"
        private const val KEY_SYSTEM_INTEGRATION_DISABLED = "system_integration_disabled"
    }
}
