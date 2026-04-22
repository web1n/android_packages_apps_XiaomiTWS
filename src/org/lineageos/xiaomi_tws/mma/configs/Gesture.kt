package org.lineageos.xiaomi_tws.mma.configs

import org.lineageos.xiaomi_tws.mma.Config
import org.lineageos.xiaomi_tws.mma.ConfigData.Gesture
import org.lineageos.xiaomi_tws.mma.ConfigData.Gesture.Function
import org.lineageos.xiaomi_tws.mma.ConfigData.Gesture.Position
import org.lineageos.xiaomi_tws.mma.ConfigData.Gesture.Type

object Gesture : Config<Gesture>(), Config.Encoder<Gesture> {

    private const val FUNCTION_NOT_MODIFY: Byte = -1

    private val CLICK_SUPPORTED_FUNCTIONS = setOf(
        Function.Disabled,
        Function.PlayPause,
        Function.PreviousTrack,
        Function.NextTrack,
        Function.VolumeUp,
        Function.VolumeDown,
        Function.Screenshot,
    )
    private val LONG_PRESS_SUPPORTED_FUNCTIONS = setOf(
        Function.Disabled,
        Function.VoiceAssistant,
        Function.NoiseControl,
    )
    private val SWIPE_SUPPORTED_FUNCTIONS = setOf(
        Function.Disabled,
        Function.VolumeControl
    )

    override val configId = 0x0002

    override fun decode(bytes: ByteArray): Gesture {
        if (bytes.size % 3 != 0) {
            throw IllegalArgumentException("Invalid byte array size: ${bytes.size}")
        }

        val value = bytes.asSequence()
            .chunked(3)
            .mapNotNull { chunk ->
                val type = Type.entries.find { it.value == chunk[0] } ?: return@mapNotNull null
                val left = Function.entries.find { it.value == chunk[1] } ?: Function.Disabled
                val right = Function.entries.find { it.value == chunk[2] } ?: Function.Disabled

                listOf((Position.Left to type) to left, (Position.Right to type) to right)
            }
            .flatten()
            .toMap()
        return Gesture(value)
    }

    override fun encode(value: Gesture): ByteArray {
        return value.value.entries
            .groupBy { it.key.second }
            .flatMap { (type, entries) ->
                val positionMap = entries.associate { it.key.first to it.value.value }

                listOf(
                    type.value,
                    positionMap[Position.Left] ?: FUNCTION_NOT_MODIFY,
                    positionMap[Position.Right] ?: FUNCTION_NOT_MODIFY
                )
            }
            .toByteArray()
    }

    fun getSupportedFunctions(type: Type): Set<Function> {
        return when (type) {
            Type.SingleClick, Type.DoubleClick, Type.TrebleClick -> CLICK_SUPPORTED_FUNCTIONS
            Type.LongPress -> LONG_PRESS_SUPPORTED_FUNCTIONS
            Type.Swipe -> SWIPE_SUPPORTED_FUNCTIONS
        }
    }

}
