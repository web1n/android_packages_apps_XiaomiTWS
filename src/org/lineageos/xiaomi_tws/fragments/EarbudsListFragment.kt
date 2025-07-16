package org.lineageos.xiaomi_tws.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.lineageos.xiaomi_tws.PersistentApplication.Companion.enableSystemIntegration
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.nearby.NearbyDevice
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceListener
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.widgets.EarbudsPreference
import org.lineageos.xiaomi_tws.widgets.EarbudsPreference.EarbudsState

class EarbudsListFragment : PreferenceFragmentCompat() {

    private val mmaManager: MMAManager by lazy { MMAManager.getInstance(requireContext()) }
    private val nearbyDeviceScanner by lazy { NearbyDeviceScanner.getInstance(requireContext()) }

    private val mmaListener = object : MMAListener {
        override fun onDeviceEvent(event: DeviceEvent) {
            if (event is DeviceEvent.Connected || event is DeviceEvent.Disconnected) {
                updateMMADevice(event.device, event is DeviceEvent.Connected)
            }
        }
    }

    private val nearbyDeviceListener = object : NearbyDeviceListener {
        override fun onDevicesChanged(devices: Set<NearbyDevice>) = updateNearbyDevices(devices)
    }

    private val earbudsListCategory: PreferenceCategory
        get() = findPreference(KEY_EARBUDS_LIST)!!

    private val emptyStatePreference: Preference
        get() = findPreference(KEY_EARBUDS_LIST_EMPTY)!!

    private val mmaDevices = mutableSetOf<BluetoothDevice>()
    private val nearbyDevices = mutableSetOf<BluetoothDevice>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.earbuds_list)
    }

    override fun onResume() {
        super.onResume()
        if (!enableSystemIntegration) {
            updateEmptyState()
            return
        }

        mmaDevices.clear()
        nearbyDevices.clear()
        nearbyDevices.addAll(nearbyDeviceScanner.devices.mapNotNull { it.getDevice(requireContext()) })

        updateDevicePreferences()

        mmaManager.registerConnectionListener(mmaListener)
        nearbyDeviceScanner.registerNearbyListener(nearbyDeviceListener)
    }

    override fun onPause() {
        super.onPause()
        if (!enableSystemIntegration) {
            return
        }

        mmaManager.unregisterConnectionListener(mmaListener)
        nearbyDeviceScanner.unregisterNearbyListener(nearbyDeviceListener)
    }

    private fun updateUI(action: () -> Unit) {
        activity?.runOnUiThread {
            if (activity?.isFinishing != false || !isAdded) {
                return@runOnUiThread
            }

            action()
        }
    }

    private fun updateMMADevice(device: BluetoothDevice, connected: Boolean) {
        if (connected) {
            mmaDevices.add(device)
        } else {
            mmaDevices.remove(device)
        }

        updateDevicePreferences()
    }

    private fun updateNearbyDevices(devices: Set<NearbyDevice>) {
        if (DEBUG) Log.d(TAG, "updateNearbyDevices: ${devices.size} devices found")

        nearbyDevices.clear()
        nearbyDevices.addAll(devices.mapNotNull { it.getDevice(requireContext()) })

        updateDevicePreferences()
    }

    private fun updateDevicePreferences() {
        updateUI { earbudsListCategory.removeAll() }

        val localDevices = BluetoothUtils.bondedDevices
            .filter { device -> BluetoothUtils.isDeviceMetadataSet(requireContext(), device) }
        val devices = (mmaDevices + nearbyDevices + localDevices).toSet()

        devices.forEach { device ->
            val state = determineDeviceState(device)

            updateUI {
                val preference = EarbudsPreference(requireContext(), device).apply {
                    this.state = state
                    title = getDeviceName(device)
                }
                earbudsListCategory.addPreference(preference)
            }
        }

        updateEmptyState()
    }

    private fun determineDeviceState(device: BluetoothDevice): EarbudsState {
        val isMMA = device in mmaDevices
        val isNearby = device in nearbyDevices
        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED

        return when {
            isNearby && isMMA -> EarbudsState.NEARBY_CONNECTED
            isNearby && isBonded -> EarbudsState.NEARBY_BONDED
            isMMA -> EarbudsState.CONNECTED
            isNearby -> EarbudsState.NEARBY
            isBonded -> EarbudsState.BONDED
            else -> EarbudsState.UNKNOWN
        }
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return device.alias
            ?: device.name
            ?: nearbyDeviceScanner.devices.find { it.getDevice(requireContext()) == device }?.name
            ?: device.address
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
