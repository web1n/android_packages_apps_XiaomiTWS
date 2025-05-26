package org.lineageos.xiaomi_tws.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.configs.ConfigController
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.MMARequestBuilder.Companion.batteryInfo
import org.lineageos.xiaomi_tws.mma.MMARequestBuilder.Companion.getConfig
import org.lineageos.xiaomi_tws.mma.MMARequestBuilder.Companion.setConfig
import org.lineageos.xiaomi_tws.mma.MMARequestBuilder.Companion.vidPid
import org.lineageos.xiaomi_tws.utils.PreferenceUtils.createAllControllers

@SuppressLint("MissingPermission")
class EarbudsInfoFragment : PreferenceFragmentCompat() {

    private val manager: MMAManager by lazy { MMAManager.getInstance(requireContext()) }
    private val mmaListener = object : MMAListener() {
        override fun onDeviceConnected(device: BluetoothDevice) = handleDeviceConnected(device)

        override fun onDeviceDisconnected(device: BluetoothDevice) =
            handleDeviceDisconnected(device)

        override fun onDeviceBatteryChanged(device: BluetoothDevice, earbuds: Earbuds) =
            handleDeviceBatteryChanged(device, earbuds)

        override fun onDeviceConfigChanged(device: BluetoothDevice, config: Int, value: ByteArray) =
            handleOnConfigChanged(device, config, value)
    }

    private val configControllers = HashSet<ConfigController>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var device: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeDevice()
    }

    override fun onResume() {
        super.onResume()

        manager.registerConnectionListener(mmaListener)
    }

    override fun onStop() {
        super.onStop()

        manager.unregisterConnectionListener(mmaListener)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.earbuds_settings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindPreferenceControllers()
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.coroutineContext.cancel()
        unbindPreferenceControllers()
    }

    private fun initializeDevice() {
        device = IntentCompat.getParcelableExtra(
            requireActivity().intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )!!

        requireActivity().title = device.alias
        if (DEBUG) Log.d(TAG, "Initialized with device: $device")
    }

    private fun reloadConfig() {
        if (DEBUG) Log.d(TAG, "reloadConfig")

        val configIds = configControllers
            .map { it.configId }
            .filter { it != ConfigController.CONFIG_ID_INVALID }
            .distinct()
            .toTypedArray()
        if (DEBUG) Log.d(TAG, "reloadConfig: ids: ${configIds.contentToString()}")

        coroutineScope.launch {
            runCatching {
                val battery = manager.request(device, batteryInfo())
                val (vid, pid) = manager.request(device, vidPid())
                val configs = HashMap<Int, ByteArray?>().apply {
                    configIds.forEach { id ->
                        val value = manager.request(device, getConfig(id))
                        put(id, value)
                    }
                }

                configs.forEach { (configId, value) ->
                    configControllers.filter { controller ->
                        controller.configId == configId
                    }.forEach { controller ->
                        controller.setBatteryData(battery)
                        controller.setVendorData(vid, pid)
                        controller.configValue = value
                    }
                }
            }.onSuccess {
                updateUI { refreshPreferences() }
            }.onFailure {
                handleError("Config reload failed", it)
            }
        }
    }

    private fun bindPreferenceControllers() {
        if (DEBUG) Log.d(TAG, "bindPreferenceControllers")

        createAllControllers(requireContext(), R.xml.earbuds_settings).onEach {
            bindControllerToPreference(it)
        }.also {
            configControllers.addAll(it)
        }
    }

    private fun bindControllerToPreference(controller: ConfigController) {
        findPreference<Preference>(controller.preferenceKey)?.let {
            controller.displayPreference(it)
            setupPreferenceListener(it)
        }
    }

    private fun unbindPreferenceControllers() {
        if (DEBUG) Log.d(TAG, "unbindPreferenceControllers")

        configControllers.mapNotNull {
            findPreference(it.preferenceKey)
        }.forEach {
            it.onPreferenceChangeListener = null
            it.onPreferenceClickListener = null
        }

        configControllers.clear()
    }

    private fun setupPreferenceListener(preference: Preference) {
        preference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { p, newValue ->
                findController(p)?.let { controller ->
                    if (controller is Preference.OnPreferenceChangeListener) {
                        val update = controller.onPreferenceChange(p, newValue)
                        if (!update) return@let
                    }

                    handlePreferenceChange(controller, p, newValue)
                }

                false
            }

        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { p ->
            findController(p)?.let { controller ->
                if (controller is Preference.OnPreferenceClickListener) {
                    return@OnPreferenceClickListener controller.onPreferenceClick(p)
                }
            }

            false
        }
    }

    private fun handlePreferenceChange(
        controller: ConfigController, preference: Preference, newValue: Any
    ) {
        coroutineScope.launch {
            runCatching {
                controller.transNewValue(newValue).also {
                    manager.request(device, setConfig(controller.configId, it))
                }
            }.onSuccess {
                controller.configValue = it
                updateUI { controller.updateState(preference) }
            }.onFailure {
                handleError("Save config failed", it)
            }
        }
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        if (device != this.device) return

        reloadConfig()
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        if (device != this.device) return

        activity?.finish()
    }

    private fun handleDeviceBatteryChanged(device: BluetoothDevice, earbuds: Earbuds) {
        if (device != this.device) return

        configControllers.forEach { it.setBatteryData(earbuds) }
    }

    private fun handleOnConfigChanged(device: BluetoothDevice, configId: Int, value: ByteArray) {
        if (device != this.device) return

        configControllers.filter {
            it.configId == configId && it.configValue != value
        }.forEach { controller ->
            controller.configValue = value

            findPreference<Preference>(controller.preferenceKey)?.let { preference ->
                controller.updateState(preference)
            }
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

    private fun refreshPreferences() {
        configControllers.forEach { controller ->
            findPreference<Preference>(controller.preferenceKey)?.let { preference ->
                controller.updateState(preference)
            }
        }
    }

    private fun findController(preference: Preference): ConfigController? {
        val controller = configControllers.firstOrNull { controller ->
            controller.preferenceKey == preference.key
        }

        if (controller == null) {
            Log.w(TAG, "Unknown controller for: ${preference.key}")
        }
        return controller
    }

    private fun showToast(content: String) {
        if (DEBUG) Log.d(TAG, "showToast: $content")

        updateUI { Toast.makeText(activity, content, Toast.LENGTH_SHORT).show() }
    }

    private fun handleError(message: String, e: Throwable) {
        Log.e(TAG, message, e)
        showToast("$message: $e")
    }

    companion object {
        private val TAG = EarbudsInfoFragment::class.java.simpleName
        private const val DEBUG = true

        const val ACTION_EARBUDS_INFO = "org.lineageos.xiaomi_tws.action.EARBUDS_INFO"
    }
}
