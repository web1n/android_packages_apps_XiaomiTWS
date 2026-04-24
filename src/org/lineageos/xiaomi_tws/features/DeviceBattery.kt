package org.lineageos.xiaomi_tws.features

import org.lineageos.xiaomi_tws.utils.ByteUtils.isBitSet

data class DeviceBattery(val left: Battery?, val right: Battery?, val case: Battery?) {
    data class Battery(val battery: Int, val charging: Boolean) {
        companion object {
            fun fromByte(byte: Byte): Battery? {
                val battery = byte.toInt() and 0x7F
                val charging = byte.isBitSet(7)
                val valid = battery in 1..100

                return if (valid) Battery(battery, charging) else null
            }
        }
    }

    val readableString: String
        get() {
            return ArrayList<String>().apply {
                if (left != null) add("\uD83C\uDFA7 Left: ${left.battery}% ${if (left.charging) "charging" else ""}")
                if (right != null) add("\uD83C\uDFA7 Right: ${right.battery}% ${if (right.charging) "charging" else ""}")
                if (case != null) add("\uD83D\uDD0B Case: ${case.battery}% ${if (case.charging) "charging" else ""}")
            }.joinToString()
        }

    companion object {
        fun fromBytes(left: Byte, right: Byte, case: Byte): DeviceBattery? {
            val battery = DeviceBattery(
                Battery.fromByte(left),
                Battery.fromByte(right),
                Battery.fromByte(case)
            )
            val valid = battery.left != null || battery.right != null || battery.case != null
            return if (valid) battery else null
        }
    }
}
