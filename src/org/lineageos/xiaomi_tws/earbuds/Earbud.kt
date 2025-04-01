package org.lineageos.xiaomi_tws.earbuds

import com.android.settingslib.bluetooth.BluetoothUtils.META_INT_ERROR

data class Earbud(val raw: Byte) {

    val charging: Boolean
    val battery: Int
    val valid: Boolean

    init {
        val battery = raw.toInt() and EARBUDS_BATTERY_LEVEL_MASK
        val charging = (raw.toInt() and EARBUDS_CHARGING_BIT_MASK) != 0
        val valid = battery <= 100

        this.battery = if (valid) battery else META_INT_ERROR
        this.charging = charging and valid
        this.valid = valid
    }

    override fun toString() = "Earbud{charging=$charging, battery=$battery, valid=$valid}";

    companion object {
        private const val EARBUDS_CHARGING_BIT_MASK = 0x80
        private const val EARBUDS_BATTERY_LEVEL_MASK = 0x7F
    }
}
