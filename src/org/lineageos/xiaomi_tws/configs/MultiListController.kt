package org.lineageos.xiaomi_tws.configs

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.MMADevice
import org.lineageos.xiaomi_tws.utils.ByteUtils.hexToBytes
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

abstract class MultiListController(context: Context, preferenceKey: String) :
    ConfigController(context, preferenceKey) {

    private var isModesUpdated = false

    protected abstract val configStates: Set<ConfigState>
    protected abstract val checkedStates: Set<ConfigState>

    override fun updateState(preference: Preference) {
        updateSupportedModes(preference as MultiSelectListPreference)

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
            checkedStates.joinToString { state ->
                context.getString(state.summaryResId)
            }
        }

    override fun updateValue(preference: Preference) {
        if (DEBUG) Log.d(TAG, "updateValue: $preferenceKey")

        checkedStates
            .map { state -> state.configValue.toHexString() }.toSet()
            .let { (preference as MultiSelectListPreference).values = it }
    }

    private fun updateSupportedModes(preference: MultiSelectListPreference) {
        if (isAvailable != Available.AVAILABLE || isModesUpdated) {
            return
        }
        isModesUpdated = true

        val configStates = configStates
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
        private val TAG = MultiListController::class.java.simpleName
        private const val DEBUG = true
    }
}
