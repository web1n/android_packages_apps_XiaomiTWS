package org.lineageos.xiaomi_tws.widgets

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.fragments.EarbudsInfoFragment.Companion.ACTION_EARBUDS_INFO

class EarbudsPreference(context: Context, private val device: BluetoothDevice) :
    Preference(context) {

    enum class EarbudsState {
        CONNECTED, BONDED, NEARBY, NEARBY_BONDED, NEARBY_CONNECTED, UNKNOWN;

        val isBonded: Boolean
            get() = this == BONDED || this == NEARBY_BONDED
        val isConnected: Boolean
            get() = this == CONNECTED || this == NEARBY_CONNECTED
        val isNearby: Boolean
            get() = this == NEARBY || this == NEARBY_BONDED || this == NEARBY_CONNECTED
    }

    init {
        key = device.address
        order = key.hashCode()
        title = device.alias ?: device.name ?: device.address
    }

    var state: EarbudsState = EarbudsState.UNKNOWN
        set(value) {
            field = value

            updateSummary()
        }

    private fun updateSummary() {
        val summaryResId = when {
            state.isConnected -> R.string.earbuds_list_device_connected
            state.isNearby && state.isBonded -> R.string.earbuds_list_device_nearby_bonded
            state.isNearby -> R.string.earbuds_list_device_nearby
            else -> R.string.earbuds_list_device_disconnected
        }

        setSummary(summaryResId)
    }

    override fun onClick() {
        when {
            state.isConnected -> {
                val intent = Intent(ACTION_EARBUDS_INFO).apply {
                    putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    setPackage(this@EarbudsPreference.context.packageName)
                }
                context.startActivity(intent)
            }

            state.isBonded -> device.runCatching { connect() }
            !state.isBonded -> device.runCatching { createBond() }
            else -> {}
        }
    }

}
