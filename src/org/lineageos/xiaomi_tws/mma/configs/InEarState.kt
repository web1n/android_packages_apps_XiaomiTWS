package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder
import org.lineageos.xiaomi_tws.mma.configs.InEarState.State
import org.lineageos.xiaomi_tws.utils.ByteUtils.isBitSet
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString

class InEarState : ConfigRequestBuilder<Pair<State, State>>(CONFIG_ID) {

    enum class State { InEar, InCase, Outside }

    override fun bytesToValue(bytes: ByteArray): Pair<State, State> {
        return parseConfigValue(bytes)
    }

    override fun valueToBytes(value: Pair<State, State>): ByteArray {
        throw NotImplementedError()
    }

    companion object {
        const val CONFIG_ID = 0x000C
        private const val VALID_BYTES_LENGTH = 1

        fun parseConfigValue(bytes: ByteArray): Pair<State, State> {
            if (bytes.size != VALID_BYTES_LENGTH) {
                throw NotImplementedError("Not supported value: ${bytes.toHexString()}")
            }
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

            return left to right
        }
    }

}
