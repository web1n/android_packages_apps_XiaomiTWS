package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.InEarState
import org.lineageos.xiaomi_tws.mma.ConfigData.InEarState.State
import org.lineageos.xiaomi_tws.utils.ByteUtils.isBitSet

object InEarState : Config<InEarState>() {

    override val configId = 0x000C
    override val validBytesLength = 1

    override fun decode(bytes: ByteArray): InEarState {
        val status = bytes[0]

        val left = when {
            status.isBitSet(3) -> State.InEar
            status.isBitSet(1) -> State.InCase
            else -> State.Outside
        }
        val right = when {
            status.isBitSet(2) -> State.InEar
            status.isBitSet(0) -> State.InCase
            else -> State.Outside
        }

        return InEarState(left, right)
    }
}
