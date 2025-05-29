package org.lineageos.xiaomi_tws.mma.configs

import android.util.Log
import org.lineageos.xiaomi_tws.mma.ConfigRequestBuilder

class Gesture :
    ConfigRequestBuilder<Map<Pair<Gesture.Position, Gesture.Type>, Gesture.Function>>(CONFIG_ID) {

    enum class Position { Left, Right; }

    enum class Type(internal val value: Byte) {
        SingleClick(TYPE_SINGLE_CLICK),
        DoubleClick(TYPE_DOUBLE_CLICK),
        TrebleClick(TYPE_TREBLE_CLICK),
        LongPress(TYPE_LONG_PRESS);

        companion object {
            fun fromByte(value: Byte) = entries.find { it.value == value }
        }
    }

    enum class Function(internal val value: Byte) {
        Disabled(FUNCTION_DISABLED),
        VoiceAssistant(FUNCTION_VOICE_ASSISTANT),
        PlayPause(FUNCTION_PLAY_PAUSE),
        PreviousTrack(FUNCTION_PREVIOUS_TRACK),
        NextTrack(FUNCTION_NEXT_TRACK),
        VolumeUp(FUNCTION_VOLUME_UP),
        VolumeDown(FUNCTION_VOLUME_DOWN),
        NoiseControl(FUNCTION_NOISE_CONTROL),
        Screenshot(FUNCTION_SCREENSHOT);

        companion object {
            internal fun fromValue(value: Byte) = entries.find { it.value == value }
        }
    }

    override fun bytesToValue(bytes: ByteArray): Map<Pair<Position, Type>, Function> {
        if (bytes.size % 3 != 0) {
            Log.w(TAG, "Length must be multiple of 3. Actual: ${bytes.size}")
            throw NotImplementedError()
        }

        return bytes.asSequence()
            .chunked(3)
            .mapNotNull { chunk ->
                val type = Type.fromByte(chunk[0]) ?: return@mapNotNull null
                val leftFunction = Function.fromValue(chunk[1]) ?: Function.Disabled
                val rightFunction = Function.fromValue(chunk[2]) ?: Function.Disabled

                listOf(
                    (Position.Left to type) to leftFunction,
                    (Position.Right to type) to rightFunction
                )
            }
            .flatten()
            .toMap()
    }

    override fun valueToBytes(value: Map<Pair<Position, Type>, Function>): ByteArray {
        return value.entries
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

    companion object {
        private val TAG = Gesture::class.java.simpleName

        private const val CONFIG_ID = 0x0002

        private const val TYPE_SINGLE_CLICK: Byte = 0x04
        private const val TYPE_DOUBLE_CLICK: Byte = 0x01
        private const val TYPE_TREBLE_CLICK: Byte = 0x02
        private const val TYPE_LONG_PRESS: Byte = 0x03

        private const val FUNCTION_DISABLED: Byte = 0x08
        private const val FUNCTION_VOICE_ASSISTANT: Byte = 0x00
        private const val FUNCTION_PLAY_PAUSE: Byte = 0x01
        private const val FUNCTION_PREVIOUS_TRACK: Byte = 0x02
        private const val FUNCTION_NEXT_TRACK: Byte = 0x03
        private const val FUNCTION_VOLUME_UP: Byte = 0x04
        private const val FUNCTION_VOLUME_DOWN: Byte = 0x05
        private const val FUNCTION_NOISE_CONTROL: Byte = 0x06
        private const val FUNCTION_SCREENSHOT: Byte = 0x09
        private const val FUNCTION_NOT_MODIFY: Byte = -1
    }

}
