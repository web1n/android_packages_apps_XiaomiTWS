package org.lineageos.xiaomi_bluetooth.configs

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.lineageos.xiaomi_bluetooth.mma.MMADevice
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.hexToBytes
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.toHexString

abstract class ListController(context: Context, preferenceKey: String) :
    ConfigController(context, preferenceKey) {

    private var isModesUpdated = false

    protected abstract val configStates: Set<ConfigState>
    protected abstract val defaultState: ConfigState

    override fun updateState(preference: Preference) {
        updateSupportedModes(preference as ListPreference)
        super.updateState(preference)
    }

    override val isAvailable: Available
        get() = when (super.isAvailable) {
            Available.AVAILABLE -> if (configStates.isNotEmpty()) {
                Available.AVAILABLE
            } else {
                Available.UNAVAILABLE
            }

            Available.UNAVAILABLE -> Available.UNAVAILABLE
            Available.UNKNOWN -> Available.UNKNOWN
        }

    override val summary: String?
        get() = configValue?.let {
            context.getString(
                configStates.first { state -> state.configValue.contentEquals(configValue) }.summaryResId
            )
        }

    override fun updateValue(preference: Preference) {
        configValue?.run {
            (preference as ListPreference).value = toHexString()
        }
    }

    private fun updateSupportedModes(preference: ListPreference) {
        if (isAvailable != Available.AVAILABLE || isModesUpdated) {
            return
        }
        isModesUpdated = true

        val entryValues = configStates
            .map { state -> state.configValue.toHexString() }
            .toTypedArray()
        val entries = configStates
            .map { state -> context.getString(state.summaryResId) }
            .toTypedArray()

        if (DEBUG) Log.d(
            TAG,
            "updateSupportedModes: " +
                    "entryValues=${entryValues.contentToString()}, " +
                    "entries=${entries.contentToString()}"
        )

        preference.entryValues = entryValues
        preference.entries = entries
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun saveConfig(device: MMADevice, value: Any): Boolean {
        return super.saveConfig(device, if (value is String) value.hexToBytes() else value)
    }

    companion object {
        private val TAG = ListController::class.java.simpleName
        private const val DEBUG = true
    }
}
