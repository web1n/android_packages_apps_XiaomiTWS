package org.lineageos.xiaomi_bluetooth.configs

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_DISABLED
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_NEXT_TRACK
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_NOISE_CONTROL
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_PLAY_PAUSE
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_PREVIOUS_TRACK
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_SCREENSHOT
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_VOICE_ASSISTANT
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_VOLUME_DOWN
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_VOLUME_UP
import org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_MODE
import org.lineageos.xiaomi_bluetooth.R
import org.lineageos.xiaomi_bluetooth.mma.MMADevice
import org.lineageos.xiaomi_bluetooth.utils.ByteUtils.hexToBytes
import java.nio.ByteBuffer

class ButtonController(
    context: Context,
    preferenceKey: String
) : ListController(context, preferenceKey) {

    private enum class Type(val value: Byte) {
        SINGLE_CLICK(0x04), DOUBLE_CLICK(0x01), TREBLE_CLICK(0x02), LONG_PRESS(0x03);

        companion object {
            fun fromByte(value: Byte): Type? {
                for (type: Type in entries) {
                    if (type.value == value) return type
                }

                Log.w(TAG, "Unknown Type value: 0x%02x".format(value))
                return null
            }
        }
    }

    private enum class Position { LEFT, RIGHT }

    private data class ButtonConfig(val type: Type, val position: Position, var value: Byte) {
        fun toBytes() = byteArrayOf(
            type.value,
            if (position == Position.LEFT) value else VALUE_NOT_MODIFIED,
            if (position == Position.RIGHT) value else VALUE_NOT_MODIFIED
        )

        override fun toString() = "%02x".format(value)

        companion object {
            fun parseFromBytes(value: ByteArray): List<ButtonConfig> {
                if (value.size % 3 != 0) {
                    Log.w(TAG, "Length must be multiple of 3. Actual: ${value.size}")
                    return emptyList()
                }

                val configs = ArrayList<ButtonConfig>()
                val buffer = ByteBuffer.wrap(value)

                while (buffer.remaining() >= 3) {
                    val typeByte: Byte = buffer.get()
                    val leftValue: Byte = buffer.get()
                    val rightValue: Byte = buffer.get()

                    val type = Type.fromByte(typeByte)
                    if (type != null) {
                        configs.add(ButtonConfig(type, Position.LEFT, leftValue))
                        configs.add(ButtonConfig(type, Position.RIGHT, rightValue))
                    }
                }

                return configs
            }
        }
    }

    private val type: Type
    private val position: Position

    init {
        val lastUnderlineIndex = preferenceKey.lastIndexOf('_')
        require(lastUnderlineIndex != -1) {
            "Invalid preference key format: $preferenceKey"
        }

        try {
            type = Type.valueOf(preferenceKey.substring(0, lastUnderlineIndex).uppercase())
            position = Position.valueOf(preferenceKey.substring(lastUnderlineIndex + 1).uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid type or position in preference key: $preferenceKey", e
            )
        }
    }

    override val configId = XIAOMI_MMA_CONFIG_BUTTON_MODE
    override val expectedConfigLength = 1

    override val configStates = setOf(
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_DISABLED), R.string.function_disabled),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_VOICE_ASSISTANT), R.string.function_voice_assistant),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_PLAY_PAUSE), R.string.function_play_pause),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_PREVIOUS_TRACK), R.string.function_previous_track),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_NEXT_TRACK), R.string.function_next_track),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_VOLUME_UP), R.string.function_volume_up),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_VOLUME_DOWN), R.string.function_volume_down),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_NOISE_CONTROL), R.string.function_noise_control),
        ConfigState(byteArrayOf(XIAOMI_MMA_CONFIG_BUTTON_FUNCTION_SCREENSHOT), R.string.function_screenshot)
    )
    override val defaultState = configStates.first()

    override val summary: String?
        get() = buttonConfig?.let {
            context.getString(
                configStates.first { state -> state.configValue[0] == it.value }.summaryResId
            )
        }

    override fun isValidValue(value: ByteArray?): Boolean {
        return value?.let { ButtonConfig.parseFromBytes(value).isNotEmpty() } == true
    }

    override fun updateValue(preference: Preference) {
        val buttonConfig = buttonConfig ?: let {
            Log.w(TAG, "No button config found for update")
            return
        }

        (preference as ListPreference).value = buttonConfig.toString()
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun saveConfig(device: MMADevice, value: Any): Boolean {
        require(value is String) {
            "Invalid value type: ${value.javaClass.simpleName}"
        }

        val buttonConfig = checkNotNull(buttonConfig) {
            "No existing button config to update"
        }.apply {
            this.value = value.hexToBytes()[0]
        }.toBytes()

        return super.saveConfig(device, buttonConfig)
    }

    private val buttonConfig: ButtonConfig?
        get() = configValue?.let {
            ButtonConfig.parseFromBytes(it).firstOrNull { config ->
                config.type == type && config.position == position
            }
        }

    companion object {
        private val TAG = ButtonController::class.java.simpleName
//        private const val DEBUG = true

        private const val VALUE_NOT_MODIFIED: Byte = -1
    }
}
