package org.lineageos.xiaomi_tws.earbuds

data class Earbuds(
    val left: Earbud,
    val right: Earbud,
    val case: Earbud
) {

    val caseValid = this.case.valid
    val leftOrRightValid = this.left.valid || this.right.valid
    val valid = this.leftOrRightValid || this.caseValid

    val readableString: String
        get() = ArrayList<String>().apply {
            if (left.valid) add("\uD83C\uDFA7 Left: ${left.battery}% ${if (left.charging) "charging" else ""}")
            if (right.valid) add("\uD83C\uDFA7 Right: ${right.battery}% ${if (right.charging) "charging" else ""}")
            if (case.valid) add("\uD83D\uDD0B Case: ${case.battery}% ${if (case.charging) "charging" else ""}")
        }.joinToString()

    companion object {
        fun fromBytes(left: Byte, right: Byte, case: Byte): Earbuds {
            return Earbuds(Earbud(left), Earbud(right), Earbud(case))
        }
    }
}
