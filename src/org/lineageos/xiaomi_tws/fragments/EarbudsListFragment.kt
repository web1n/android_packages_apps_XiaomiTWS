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

class EarbudsListFragment : PreferenceFragmentCompat() {

    private val manager: MMAManager by lazy { MMAManager.getInstance(requireContext()) }
    private val mmaListener = object : MMAListener() {
        override fun onDeviceConnected(device: BluetoothDevice) = addDevice(device)

        override fun onDeviceDisconnected(device: BluetoothDevice) = removeDevice(device)

        override fun onDeviceBatteryChanged(device: BluetoothDevice, earbuds: Earbuds) =
            updateEarbudsPreference(device, earbuds)
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

        reloadDevices()
    }

    override fun onPause() {
        super.onPause()

        manager.unregisterConnectionListener(mmaListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.coroutineContext.cancel()
    }

    private fun reloadDevices() {
        if (DEBUG) Log.d(TAG, "reloadDevices")

        removeAllDevices()
        manager.registerConnectionListener(mmaListener)
    }

    private fun addDevice(device: BluetoothDevice) {
        addEarbudsPreference(device)
        updateEmptyState()

        coroutineScope.launch {
            manager.runCatching {
                request(devicce, batteryInfo())
            }.onSuccess {
                Log.d(TAG, "Battery info received: $it")
                updateUI { updateEarbudsPreference(device, it) }
            }.onError {
                Log.e(TAG, "Failed to get battery info", it)
                updateUI { updateEarbudsPreference(device, it) }
            }
        }
    }

    private fun removeDevice(device: BluetoothDevice) {
        earbudsListCategory.findPreference<Preference>(device.address)?.let {
            earbudsListCategory.removePreference(it)
        }
        updateEmptyState()
    }

    private fun removeAllDevices() {
        if (DEBUG) Log.d(TAG, "removeAllDevices")

        earbudsListCategory.removeAll()
        updateEmptyState()
    }

    private fun updateUI(action: () -> Unit) {
        activity?.runOnUiThread {
            if (activity?.isFinishing != false || !isAdded) {
                return@runOnUiThread
            }

            action()
        }
    }

    private fun addEarbudsPreference(device: BluetoothDevice) {
        if (DEBUG) Log.d(TAG, "Adding preference for: $device")

        val earbudsPreference = findPreference(device.address)
            ?: Preference(requireContext()).apply {
                key = device.address
                earbudsListCategory.addPreference(this)
            }

        val infoIntent = Intent(EarbudsInfoFragment.ACTION_EARBUDS_INFO).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            setPackage(requireContext().packageName)
        }

        earbudsPreference.title = device.alias
        earbudsPreference.summary = getString(R.string.earbuds_list_device_connecting)
        earbudsPreference.intent = infoIntent
        earbudsPreference.isSelectable = false
    }

    private fun updateEarbudsPreference(device: BluetoothDevice, result: Any) {
        if (DEBUG) Log.d(TAG, "Updating preference for device: $device")

        findPreference<Preference>(device.address)?.apply {
            summary = if (result is Earbuds) {
                result.readableString
            } else {
                result.toString()
            }
            isSelectable = result is Earbuds
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
