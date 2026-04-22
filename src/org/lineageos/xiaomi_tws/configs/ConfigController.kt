package org.lineageos.xiaomi_tws.configs

import android.util.Log
import android.bluetooth.BluetoothDevice
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager

abstract class ConfigController<T : Preference, U, R : ConfigData>(
    preferenceKey: String,
    device: BluetoothDevice
) :
    BaseConfigController<T>(preferenceKey, device), MMAListener,
    BaseConfigController.OnPreferenceChangeListener<T, U> {

    protected abstract val config: Config<R>
    protected var value: R? = null

    protected abstract fun preferenceValueToValue(value: U): R

    override suspend fun initData(manager: MMAManager) {
        value = manager.request(device, config.get())
    }

    override fun preInitView(preference: T) {
        super.preInitView(preference)

        preference.isPersistent = false
    }

    override fun onDeviceEvent(event: DeviceEvent) {
        if (event !is DeviceEvent.ConfigChanged || event.configId != config.configId) {
            return
        }

        @Suppress("UNCHECKED_CAST")
        value = event.value as R?
    }

    final override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: T,
        newValue: U
    ): Boolean {
        val realValue = preferenceValueToValue(newValue)

        val result = manager
            .runCatching { request(device, config.set(realValue)) }
            .onFailure { Log.e(TAG, "Failed to set config $config $realValue") }
            .getOrElse { false }
        if (result) value = realValue
        return result
    }

    companion object {
        private val TAG = ConfigController::class.simpleName
    }
}
