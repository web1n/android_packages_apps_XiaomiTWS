package org.lineageos.xiaomi_tws.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.xiaomi_tws.headset.HeadsetManager
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.utils.SettingsUtils

abstract class BaseDeviceListFragment : SettingsBasePreferenceFragment() {

    protected val headsetManager: HeadsetManager by lazy { HeadsetManager.getInstance(requireContext()) }
    protected val settingsUtils: SettingsUtils by lazy { SettingsUtils.getInstance(requireContext()) }

    protected abstract val layoutResId: Int
    protected abstract val deviceListCategoryKey: String
    protected abstract val emptyStatePrefKey: String
    protected abstract val preferenceKeyPrefix: String
    protected abstract val logTag: String

    private val deviceListCategory: PreferenceCategory
        get() = findPreference(deviceListCategoryKey)!!

    private val emptyStatePreference: Preference
        get() = findPreference(emptyStatePrefKey)!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(layoutResId)
    }

    override fun onResume() {
        super.onResume()
        updateDevicePreferences()
    }

    protected abstract fun isDeviceSupported(device: BluetoothDevice): Boolean
    protected abstract fun isDeviceEnabled(device: BluetoothDevice): Boolean
    protected abstract fun setDeviceEnabled(device: BluetoothDevice, enabled: Boolean)

    protected open fun getDeviceSummary(device: BluetoothDevice): String {
        return device.address
    }

    private fun updateUI(action: () -> Unit) {
        activity?.runOnUiThread {
            if (activity?.isFinishing != false || !isAdded) {
                return@runOnUiThread
            }

            action()
        }
    }

    private fun updateDevicePreferences() {
        updateUI { deviceListCategory.removeAll() }

        val supportedDevices = BluetoothUtils.bondedDevices
            .filter { device -> isDeviceSupported(device) }

        if (DEBUG) {
            Log.d(logTag, "updateDevicePreferences: ${supportedDevices.size} supported devices found")
        }

        supportedDevices.forEach { device ->
            updateUI {
                val preference = SwitchPreferenceCompat(requireContext()).apply {
                    key = "${preferenceKeyPrefix}_${device.address}"
                    title = getDeviceName(device)
                    summary = getDeviceSummary(device)
                    isPersistent = false
                    isChecked = isDeviceEnabled(device)

                    setOnPreferenceChangeListener { _, newValue ->
                        val enabled = newValue as Boolean
                        setDeviceEnabled(device, enabled)
                        if (DEBUG) {
                            Log.d(logTag, "$preferenceKeyPrefix ${if (enabled) "enabled" else "disabled"} for ${device.address}")
                        }
                        true
                    }
                }
                deviceListCategory.addPreference(preference)
            }
        }

        updateEmptyState(supportedDevices.isEmpty())
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return device.alias ?: device.name ?: device.address
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStatePreference.isVisible = isEmpty
    }

    companion object {
        const val DEBUG = true
    }
}
