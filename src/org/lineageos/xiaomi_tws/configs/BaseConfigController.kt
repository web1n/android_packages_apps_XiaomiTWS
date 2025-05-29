package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.MMAManager

abstract class BaseConfigController<T : Preference>(
    val preferenceKey: String,
    protected val device: BluetoothDevice
) {
    interface OnPreferenceChangeListener<T : Preference> {
        suspend fun onPreferenceChange(manager: MMAManager, preference: T, newValue: Any): Boolean
    }

    abstract suspend fun initData(manager: MMAManager)

    open fun preInitView(preference: T) {}

    open fun postInitView(preference: T) {
        updateParentVisibility(preference)
    }

    open fun postUpdateValue(preference: T) {}

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
