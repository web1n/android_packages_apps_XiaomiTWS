package org.lineageos.xiaomi_tws.earbuds

import android.bluetooth.BluetoothDevice
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothDevice

data class Earbuds(
    val address: String,
    val left: Earbud,
    val right: Earbud,
    val case: Earbud
) {

    val caseValid = this.case.valid
    val leftOrRightValid = this.left.valid || this.right.valid
    val valid = this.leftOrRightValid || this.caseValid

    val device: BluetoothDevice
        get() = getBluetoothDevice(address)

    val readableString: String
        get() = ArrayList<String>().apply {
            if (left.valid) add("\uD83C\uDFA7 Left: ${left.battery}% ${if (left.charging) "charging" else ""}")
            if (right.valid) add("\uD83C\uDFA7 Right: ${right.battery}% ${if (right.charging) "charging" else ""}")
            if (case.valid) add("\uD83D\uDD0B Case: ${case.battery}% ${if (case.charging) "charging" else ""}")
        }.joinToString()

    companion object {
        private val TAG = Earbuds::class.java.simpleName
        private const val DEBUG = true

        fun fromBytes(address: String, left: Byte, right: Byte, case: Byte): Earbuds {
            return Earbuds(address, Earbud(left), Earbud(right), Earbud(case))
        }
    }
}
