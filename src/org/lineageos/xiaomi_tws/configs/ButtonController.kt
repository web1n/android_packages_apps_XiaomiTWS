package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.preference.ListPreference
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.Gesture
import org.lineageos.xiaomi_tws.mma.configs.Gesture.Function
import org.lineageos.xiaomi_tws.mma.configs.Gesture.Position
import org.lineageos.xiaomi_tws.mma.configs.Gesture.Type

class ButtonController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<ListPreference, String, Map<Pair<Position, Type>, Function>>
        (preferenceKey, device) {

    override val config = Gesture()

    private val type: Type
    private val position: Position

    init {
        val lastUnderlineIndex = preferenceKey.lastIndexOf('_')
        require(lastUnderlineIndex != -1) {
            "Invalid preference key format: $preferenceKey"
        }

        try {
            type = preferenceKey
                .substring(0, lastUnderlineIndex)
                .replace("_", "")
                .lowercase()
                .let { name ->
                    Type.entries.find { it.name.lowercase() == name }!!
                }
            position = preferenceKey
                .substring(lastUnderlineIndex + 1)
                .lowercase()
                .let { name ->
                    Position.entries.find { it.name.lowercase() == name }!!
                }
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid type or position in preference key: $preferenceKey", e
            )
        }
    }

    override suspend fun initData(manager: MMAManager) {
        super.initData(manager)

        value = value?.filter { it.key == position to type }
    }

    override fun preInitView(preference: ListPreference) {
        super.preInitView(preference)

        preference.entries = Function.entries
            .map { functionToString(preference.context, it) }
            .toTypedArray()
        preference.entryValues = Function.entries
            .map { it.name }
            .toTypedArray()
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }

    override fun postUpdateValue(preference: ListPreference) {
        if (value == null) return

        val function = value!![position to type] ?: Function.Disabled
        preference.value = function.name
    }

    override fun preferenceValueToValue(value: String): Map<Pair<Position, Type>, Function> {
        return mapOf((position to type) to Function.valueOf(value))
    }

    private fun functionToString(context: Context, function: Function): String {
        val stringRes = when (function) {
            Function.Disabled -> R.string.function_disabled
            Function.VoiceAssistant -> R.string.function_voice_assistant
            Function.PlayPause -> R.string.function_play_pause
            Function.PreviousTrack -> R.string.function_previous_track
            Function.NextTrack -> R.string.function_next_track
            Function.VolumeUp -> R.string.function_volume_up
            Function.VolumeDown -> R.string.function_volume_down
            Function.NoiseControl -> R.string.function_noise_control
            Function.Screenshot -> R.string.function_screenshot
        }

        return context.getString(stringRes)
    }

}
