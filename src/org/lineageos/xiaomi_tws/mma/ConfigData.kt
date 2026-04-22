package org.lineageos.xiaomi_tws.mma

sealed interface ConfigData {

    abstract class BooleanData : ConfigData {
        abstract val enabled: Boolean
    }

    data class AutoAnswerCalls(override val enabled: Boolean) : BooleanData()

    data class EqualizerMode(val mode: Mode) : ConfigData {
        enum class Mode(val value: Byte) {
            Default(0x00),
            VocalEnhance(0x01),
            BassBoost(0x05),
            TrebleBoost(0x06),
            VolumeBoost(0x07),
            Harman(0x0E),
            HarmanMaster(0x0F),
        }
    }

    data class FindEarbuds(val enabled: Boolean, val positions: List<Position>) : ConfigData {
        enum class Position(val value: Int) { Left(0x01), Right(0x02); }
    }

    data class Gesture(val value: Map<Pair<Position, Type>, Function>) : ConfigData {
        enum class Position { Left, Right; }
        enum class Type(val value: Byte) {
            SingleClick(0x04),
            DoubleClick(0x01),
            TrebleClick(0x02),
            LongPress(0x03),
            Swipe(0x05),
        }

        enum class Function(val value: Byte) {
            Disabled(0x08),
            VoiceAssistant(0x00),
            PlayPause(0x01),
            PreviousTrack(0x02),
            NextTrack(0x03),
            VolumeUp(0x04),
            VolumeDown(0x05),
            NoiseControl(0x06),
            Screenshot(0x09),
            VolumeControl(0x0B),
        }
    }

    data class InEarState(val left: State, val right: State) : ConfigData {
        enum class State { InEar, InCase, Outside }
    }

    data class MultiConnect(override val enabled: Boolean) : BooleanData()

    data class NoiseCancellationList(
        val left: List<NoiseCancellationMode.Mode>?,
        val right: List<NoiseCancellationMode.Mode>?
    ) : ConfigData

    data class NoiseCancellationMode(val value: Mode) : ConfigData {
        enum class Mode(val value: Byte) { Off(0x00), On(0x01), Transparency(0x02), }
    }

    data class SerialNumber(val value: String) : ConfigData

}
