package org.lineageos.xiaomi_tws.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.MMADevice
import org.lineageos.xiaomi_tws.configs.ConfigController
import org.lineageos.xiaomi_tws.utils.CommonUtils.executeWithTimeout
import org.lineageos.xiaomi_tws.utils.PreferenceUtils.createAllControllers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EarbudsInfoFragment : PreferenceFragmentCompat() {

    private val actionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null) {
                return
            }

            if (intent.action == ACTION_RELOAD_CONFIG) {
                reloadConfig()
            } else {
                if (DEBUG) Log.w(TAG, "unknown action ${intent.action}")
            }
        }
    }

    private val configControllers = HashSet<ConfigController>()
    private lateinit var earbudsExecutor: ExecutorService
    private lateinit var device: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeDevice()
        initializeExecutor()
        registerActionReceiver()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.earbuds_settings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindPreferenceControllers()
    }

    override fun onResume() {
        super.onResume()

        reloadConfig()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterActionReceiver()
        shutdownExecutor()
        unbindPreferenceControllers()
    }

    @SuppressLint("MissingPermission")
    private fun initializeDevice() {
        device = IntentCompat.getParcelableExtra(
            requireActivity().intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )!!

        requireActivity().title = device.alias
        if (DEBUG) Log.d(TAG, "Initialized with device: $device")
    }

    private fun initializeExecutor() {
        earbudsExecutor = Executors.newSingleThreadExecutor()
    }

    private fun shutdownExecutor() {
        if (earbudsExecutor.isShutdown) {
            return
        }

        earbudsExecutor.shutdownNow()
    }

    private fun registerActionReceiver() {
        ContextCompat.registerReceiver(
            requireContext(),
            actionReceiver,
            IntentFilter(ACTION_RELOAD_CONFIG),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterActionReceiver() {
        requireContext().unregisterReceiver(actionReceiver)
    }

    @SuppressLint("MissingPermission")
    private fun reloadConfig() {
        if (DEBUG) Log.d(TAG, "reloadConfig")

        val configIds = configControllers
            .map { it.configId }
            .filter { it != ConfigController.CONFIG_ID_INVALID }
            .distinct()
            .toTypedArray()
        if (DEBUG) Log.d(TAG, "reloadConfig: ids: ${configIds.contentToString()}")

        executeBackgroundTask {
            runCatching {
                MMADevice(device).use {
                    it.apply {
                        connect()
                    }.run {
                        val vidPid = checkNotNull(it.vidPid) {
                            "Unable to check device model"
                        }
                        val configs = HashMap<Int, ByteArray?>().apply {
                            configIds.forEach { id ->
                                put(id, runCatching { getDeviceConfig(id) }.getOrNull())
                            }
                        }

                        object {
                            val vid = vidPid.first
                            val pid = vidPid.second
                            val configs = configs
                        }
                    }
                }
            }.onSuccess {
                it.configs.forEach { (configId, value) ->
                    configControllers.filter { controller ->
                        controller.configId == configId
                    }.forEach { controller ->
                        controller.setVendorData(it.vid, it.pid)
                        controller.configValue = value
                    }
                }

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

                    executeBackgroundTask { handlePreferenceChange(controller, p, newValue) }
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

    @SuppressLint("MissingPermission")
    private fun handlePreferenceChange(
        controller: ConfigController,
        preference: Preference,
        newValue: Any
    ) {
        runCatching {
            executeWithTimeout(MMA_DEVICE_CHECK_TIMEOUT_MS) {
                MMADevice(device).use {
                    controller.saveConfig(it, newValue)
                }
            }
        }.onSuccess {
            updateUI { controller.updateState(preference) }
        }.onFailure {
            handleError("Save config failed", it)
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
        const val ACTION_RELOAD_CONFIG = "org.lineageos.xiaomi_tws.action.RELOAD_CONFIG"

        private const val MMA_DEVICE_CHECK_TIMEOUT_MS: Long = 2000
    }
}
