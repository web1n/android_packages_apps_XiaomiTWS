package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.MMAListener
import org.lineageos.xiaomi_tws.mma.MMAManager

abstract class ConfigController<T : Preference, U, R>(
    preferenceKey: String,
    device: BluetoothDevice
) :
    BaseConfigController<T>(preferenceKey, device), MMAListener,
    BaseConfigController.OnPreferenceChangeListener<T, U> {

    protected abstract val config: ConfigRequestBuilder<R>
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
        if (event !is DeviceEvent.ConfigChanged || event.config != config.configId) {
            return
        }

        config.runCatching {
            bytesToValue(event.value)
        }.onSuccess {
            value = it
        }.onFailure {
            Log.w(TAG, "Failed to parse config value", it)
        }
    }

    final override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: T,
        newValue: U
    ): Boolean {
        val realValue = preferenceValueToValue(newValue)

        val result = manager
            .runCatching { request(device, config.set(realValue)) }
            .getOrElse { false }
        if (result) value = realValue
        return result
    }

    companion object {
        private val TAG = ConfigController::class.java.simpleName
    }

}
