package org.lineageos.xiaomi_tws.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.IntentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.configs.BaseConfigController
import org.lineageos.xiaomi_tws.configs.BaseConfigController.OnPreferenceChangeListener
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.PreferenceUtils.createAllControllers

@SuppressLint("MissingPermission")
class EarbudsInfoFragment : PreferenceFragmentCompat(), MMAListener {

    private val manager: MMAManager by lazy { MMAManager.getInstance(requireContext()) }

    private val configControllers = HashSet<BaseConfigController<Preference>>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var device: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeDevice()
    }

    override fun onResume() {
        super.onResume()

        if (!device.isConnected) {
            activity?.finish()
            return
        }

        manager.registerConnectionListener(this)
    }

    override fun onStop() {
        super.onStop()

        manager.unregisterConnectionListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.setStorageDeviceProtected()

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

        configControllers.forEach { controller ->
            findPreference(controller)?.let { controller.preInitView(it) }
        }

        coroutineScope.launch {
            configControllers.forEach { controller ->
                val success = controller
                    .runCatching { initData(manager) }
                    .onFailure {
                        Log.e(TAG, "controller ${controller.preferenceKey} init failed", it)
                    }
                    .isSuccess

                updateUI {
                    findPreference(controller)?.let {
                        if (success) {
                            controller.postInitView(it)
                            controller.postUpdateValue(it)
                        } else {
                            it.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun bindPreferenceControllers() {
        if (DEBUG) Log.d(TAG, "bindPreferenceControllers")

        createAllControllers(requireContext(), R.xml.earbuds_settings, device).onEach {
            bindControllerToPreference(it)
        }.also {
            configControllers.addAll(it)
        }
    }

    private fun bindControllerToPreference(controller: BaseConfigController<Preference>) {
        findPreference<Preference>(controller.preferenceKey)?.let {
            controller.preInitView(it)
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
                if (!p.isEnabled || !p.isSelectable) return@OnPreferenceChangeListener false
                findController(p)?.let { controller ->
                    handlePreferenceChange(controller, preference, newValue)
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

    override fun onDeviceEvent(event: DeviceEvent) {
        if (event.device != device) return

        when (event) {
            is DeviceEvent.Connected -> reloadConfig()
            is DeviceEvent.Disconnected -> {
                activity?.finish()
                return
            }

            else -> {}
        }

        configControllers.forEach { controller ->
            val preference = findPreference(controller) ?: return@forEach

            if (controller is MMAListener) {
                controller.onDeviceEvent(event)
                controller.postUpdateValue(preference)
            }
        }
    }

    private fun handlePreferenceChange(
        controller: BaseConfigController<Preference>,
        preference: Preference,
        newValue: Any
    ): Boolean {
        if (controller !is OnPreferenceChangeListener<*, *>) {
            return false
        }

        coroutineScope.launch {
            @Suppress("UNCHECKED_CAST")
            val result = (controller as OnPreferenceChangeListener<Preference, Any>)
                .runCatching { onPreferenceChange(manager, preference, newValue) }
                .onFailure {
                    Log.w(TAG, "Unable to handle on preference change", it)
                }.getOrElse { false }
            if (result) {
                updateUI { controller.postUpdateValue(preference) }
            }
        }

        return true
    }

    private fun updateUI(action: () -> Unit) {
        activity?.runOnUiThread {
            if (activity?.isFinishing != false || !isAdded) {
                return@runOnUiThread
            }

            action()
        }
    }

    private fun findController(preference: Preference): BaseConfigController<Preference>? {
        val controller = configControllers.firstOrNull { controller ->
            controller.preferenceKey == preference.key
        }

        if (controller == null) {
            Log.w(TAG, "Unknown controller for: ${preference.key}")
        }
        return controller
    }

    private fun findPreference(controller: BaseConfigController<Preference>): Preference? {
        return findPreference(controller.preferenceKey)
    }

    companion object {
        private val TAG = EarbudsInfoFragment::class.java.simpleName
        private const val DEBUG = true

        const val ACTION_EARBUDS_INFO = "org.lineageos.xiaomi_tws.action.EARBUDS_INFO"
    }
}
