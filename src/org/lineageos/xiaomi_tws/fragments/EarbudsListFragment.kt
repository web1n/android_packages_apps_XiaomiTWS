package org.lineageos.xiaomi_tws.fragments

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomi_tws.PersistentApplication.Companion.enableSystemIntegration
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.MMARequestBuilder.Companion.batteryInfo
import org.lineageos.xiaomi_tws.utils.BluetoothUtils

class EarbudsListFragment : PreferenceFragmentCompat() {

    private val manager: MMAManager by lazy { MMAManager.getInstance(requireContext()) }
    private val mmaListener = object : MMAListener() {
        override fun onDeviceConnected(device: BluetoothDevice) {
            addOrUpdatePreference(device)
            fetchBatteryInfo(device)
        }

        override fun onDeviceDisconnected(device: BluetoothDevice) {
            addOrUpdatePreference(device)
        }

        override fun onDeviceBatteryChanged(device: BluetoothDevice, earbuds: Earbuds) {
            addOrUpdatePreference(device, earbuds)
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val earbudsListCategory: PreferenceCategory
        get() = findPreference(KEY_EARBUDS_LIST)!!

    private val emptyStatePreference: Preference
        get() = findPreference(KEY_EARBUDS_LIST_EMPTY)!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.setStorageDeviceProtected()

        addPreferencesFromResource(R.xml.earbuds_list)
    }

    override fun onResume() {
        super.onResume()
        if (!enableSystemIntegration) {
            updateEmptyState()
            return
        }

        earbudsListCategory.removeAll()
        reloadLocalDevices()

        manager.registerConnectionListener(mmaListener)
    }

    override fun onPause() {
        super.onPause()
        if (!enableSystemIntegration) {
            return
        }

        manager.unregisterConnectionListener(mmaListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.coroutineContext.cancel()
    }

    private fun reloadLocalDevices() {
        if (DEBUG) Log.d(TAG, "reloadLocalDevices")

        BluetoothUtils.headsetA2DPDevices.filter { device ->
            context?.let { BluetoothUtils.isDeviceMetadataSet(it, device) } == true
        }.forEach { device ->
            addOrUpdatePreference(device)
        }
    }

    private fun fetchBatteryInfo(device: BluetoothDevice) {
        coroutineScope.launch {
            val result = manager.runCatching { request(device, batteryInfo()) }.getOrElse { it }

            updateUI { addOrUpdatePreference(device, result) }
        }
    }

    private fun updateUI(action: () -> Unit) {
        activity?.runOnUiThread {
            if (activity?.isFinishing != false || !isAdded) {
                return@runOnUiThread
            }

            action()
        }
    }

    private fun addOrUpdatePreference(device: BluetoothDevice, result: Any? = null) {
        if (DEBUG) Log.d(TAG, "Adding or updating preference for: $device")

        val earbudsPreference = findPreference(device.address) ?: addPreference(device)
        updatePreference(earbudsPreference, device, result)
    }

    private fun addPreference(device: BluetoothDevice): Preference {
        val preference = Preference(requireContext()).apply {
            title = device.alias
            key = device.address
            intent = Intent(EarbudsInfoFragment.ACTION_EARBUDS_INFO).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                setPackage(requireContext().packageName)
            }
        }
        earbudsListCategory.addPreference(preference)

        updateEmptyState()
        return preference
    }

    private fun updatePreference(
        preference: Preference,
        device: BluetoothDevice,
        result: Any? = null
    ) {
        preference.isSelectable = result is Earbuds
        preference.summary = if (device.isConnected) {
            if (result != null) {
                if (result is Earbuds) result.readableString else result.toString()
            } else {
                getString(R.string.earbuds_list_device_connecting)
            }
        } else {
            getString(R.string.earbuds_list_device_disconnected)
        }
    }

    private fun updateEmptyState() {
        emptyStatePreference.isVisible = earbudsListCategory.preferenceCount == 0
        if (!enableSystemIntegration) {
            emptyStatePreference.summary =
                getString(R.string.earbuds_list_empty_summary_system_integration_disabled)
        }
    }

    companion object {
        private val TAG = EarbudsListFragment::class.java.simpleName
        private const val DEBUG = true

        private const val KEY_EARBUDS_LIST = "earbuds_list"
        private const val KEY_EARBUDS_LIST_EMPTY = "earbuds_list_empty"
    }
}
