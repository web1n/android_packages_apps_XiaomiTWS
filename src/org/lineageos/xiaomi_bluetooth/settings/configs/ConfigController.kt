package org.lineageos.xiaomi_bluetooth.settings.configs

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.preference.Preference
import org.lineageos.xiaomi_bluetooth.mma.MMADevice
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.toHexString

abstract class ConfigController(protected val context: Context, val preferenceKey: String) {

    enum class Available {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }

    protected class ConfigState internal constructor(
        val configValue: ByteArray, @StringRes val summaryResId: Int
    )

    var vid: Int? = null
        private set
    var pid: Int? = null
        private set

    var configValue: ByteArray? = null
        set(value) {
            field = if (value == null || !isValidValue(value)) {
                byteArrayOf(VALUE_FEATURE_NOT_SUPPORTED)
            } else {
                value
            }
        }

    abstract val configId: Int
    abstract val expectedConfigLength: Int

    fun setVendorData(vid: Int, pid: Int) {
        this.vid = vid
        this.pid = pid
    }

    open fun isValidValue(value: ByteArray?): Boolean {
        return value != null && value.size == expectedConfigLength
    }

    open val isAvailable: Available
        get() {
            return if (configValue == null) {
                Available.UNKNOWN
            } else if (isValidValue(configValue) && !isNotSupported) {
                Available.AVAILABLE
            } else {
                Available.UNAVAILABLE
            }
        }

    private val isNotSupported: Boolean
        get() = configValue != null
                && configValue!!.size == 1
                && configValue!![0] == VALUE_FEATURE_NOT_SUPPORTED

    open fun displayPreference(preference: Preference) {
        preference.isPersistent = false
        updateState(preference)
    }

    open fun updateState(preference: Preference) {
        updateValue(preference)
        updateSummary(preference)
        updateVisible(preference)
    }

    open val summary: String?
        get() = null

    protected open fun updateValue(preference: Preference) {
    }

    private fun updateSummary(preference: Preference) {
        preference.summary = if (isAvailable == Available.AVAILABLE) summary else null
    }

    private fun updateVisible(preference: Preference) {
        val available = isAvailable

        preference.isVisible = available != Available.UNAVAILABLE
        preference.isSelectable = available == Available.AVAILABLE

        updateParentVisibility(preference)
    }

    open fun saveConfig(device: MMADevice, value: Any): Boolean {
        require(value is ByteArray) {
            "Invalid value type: ${value.javaClass.simpleName}"
        }

        if (value.contentEquals(configValue)) {
            if (DEBUG) Log.d(TAG, "saveConfig: config not change")
            return true
        }

        return device.setDeviceConfig(configId, value).also {
            if (DEBUG) Log.d(
                TAG,
                "Config save ${if (it) "successful" else "failed"} " +
                        "for ${javaClass.simpleName}: ${value.toHexString()}"
            )
            if (it) configValue = value
        }
    }

    companion object {
        private val TAG = ConfigController::class.java.simpleName
        private const val DEBUG = true

        const val CONFIG_ID_INVALID = 0x01
        protected const val VALUE_FEATURE_NOT_SUPPORTED: Byte = -1

        private fun updateParentVisibility(preference: Preference) {
            val parent = preference.parent ?: return

            val hasVisibleChildren = preference.isVisible or (0 until parent.preferenceCount).map {
                parent.getPreference(it)
            }.any {
                it.isVisible
            }

            parent.isVisible = hasVisibleChildren
        }
    }
}
