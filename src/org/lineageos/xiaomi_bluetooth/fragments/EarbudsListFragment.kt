package org.lineageos.xiaomi_bluetooth.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.lineageos.xiaomi_bluetooth.R
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds
import org.lineageos.xiaomi_bluetooth.mma.MMADevice
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils.executeWithTimeout
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils.openAppSettings
import org.lineageos.xiaomi_bluetooth.utils.PermissionUtils.getMissingRuntimePermissions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EarbudsListFragment : PreferenceFragmentCompat() {

    private lateinit var earbudsExecutor: ExecutorService
    private lateinit var permissionRequestLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeExecutor()
        initializePermissionHandler()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.earbuds_list)
    }

    override fun onResume() {
        super.onResume()

        reloadDevices()
    }

    override fun onDestroy() {
        super.onDestroy()

        shutdownExecutor()
    }

    private fun initializeExecutor() {
        earbudsExecutor = Executors.newSingleThreadExecutor()
    }

    private fun shutdownExecutor() {
        if (!earbudsExecutor.isShutdown) {
            earbudsExecutor.shutdownNow()
        }
    }

    private fun initializePermissionHandler() {
        permissionRequestLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val allGranted = !result.containsValue(false)
            if (allGranted) {
                reloadDevices()
            } else {
                openAppSettings(requireActivity())
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val missingPermissions = requireContext().getMissingRuntimePermissions()
        if (missingPermissions.isEmpty()) return true

        permissionRequestLauncher.launch(missingPermissions)
        return false
    }

    @SuppressLint("MissingPermission")
    private fun reloadDevices() {
        if (!checkPermissions()) return
        if (DEBUG) Log.d(TAG, "reloadDevices")

        BluetoothUtils.connectedHeadsetA2DPDevices.forEach { processDevices(it) }
    }

    @SuppressLint("MissingPermission")
    private fun processDevices(device: BluetoothDevice) {
        addEarbudsPreference(device)

        executeBackgroundTask {
            runCatching {
                executeWithTimeout(MMA_DEVICE_CHECK_TIMEOUT_MS) {
                    MMADevice(device).use {
                        it.apply { connect() }.batteryInfo
                    }
                }
            }.onSuccess {
                updateUI { updateEarbudsPreference(device, it) }
            }.onFailure {
                Log.e(TAG, "Error processing device: ${device.name}", it)
                updateUI { updateEarbudsPreference(device, null) }
            }
        }
    }

    private fun executeBackgroundTask(task: Runnable) {
        if (earbudsExecutor.isShutdown) {
            return
        }

        earbudsExecutor.execute(task)
    }

    private fun updateUI(action: Runnable) {
        requireActivity().runOnUiThread {
            if (requireActivity().isFinishing) {
                return@runOnUiThread
            }
            action.run()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addEarbudsPreference(device: BluetoothDevice) {
        if (DEBUG) Log.d(TAG, "Adding preference for: $device")

        val earbudsPreference = findPreference(device.address)
            ?: Preference(requireContext()).apply {
                key = device.address
                preferenceScreen.addPreference(this)
            }

        val infoIntent = Intent(EarbudsInfoFragment.ACTION_EARBUDS_INFO).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            setPackage(requireContext().packageName)
        }

        earbudsPreference.title = device.name
        earbudsPreference.summary = getString(R.string.device_connecting)
        earbudsPreference.intent = infoIntent
        earbudsPreference.isSelectable = false
        earbudsPreference.isIconSpaceReserved = false
    }

    private fun updateEarbudsPreference(device: BluetoothDevice, earbuds: Earbuds?) {
        if (DEBUG) Log.d(TAG, "Updating preference for device: $device")

        val earbudsPreference = findPreference<Preference>(device.address) ?: return

        if (earbuds != null) {
            earbudsPreference.isSelectable = true
            earbudsPreference.summary = earbuds.toString()
        } else {
            earbudsPreference.summary = getString(R.string.not_xiaomi_earbuds)
        }
    }

    companion object {
        private val TAG = EarbudsListFragment::class.java.simpleName
        private const val DEBUG = true

        private const val MMA_DEVICE_CHECK_TIMEOUT_MS: Long = 2000
    }
}
