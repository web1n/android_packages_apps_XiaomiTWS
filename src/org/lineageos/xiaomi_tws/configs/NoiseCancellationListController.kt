package org.lineageos.xiaomi_tws.configs

import android.content.Context
import android.util.Log
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_LIST
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_ON
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY
import org.lineageos.xiaomi_tws.R
import java.util.Locale

class NoiseCancellationListController(
    context: Context,
    preferenceKey: String
) : MultiListController(context, preferenceKey) {

    private enum class Position { LEFT, RIGHT }

    private val position =
        Position.valueOf(
            preferenceKey.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].uppercase(
                Locale.getDefault()
            )
        )

    override val configId = XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_LIST
    override val expectedConfigLength = 2

    override val configStates = setOf(
        ConfigState(
            byteArrayOf(XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF),
            R.string.noise_cancellation_mode_off
        ),
        ConfigState(
            byteArrayOf(XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_ON),
            R.string.noise_cancellation_mode_on
        ),
        ConfigState(
            byteArrayOf(XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY),
            R.string.noise_cancellation_mode_transparency
        )
    )

    override val checkedStates: Set<ConfigState>
        get() {
            if (DEBUG) Log.d(TAG, "getCheckedStates")

            if (configValue == null || !isValidValue(configValue)) {
                return setOf()
            }

            val configByte = configValue!![if (position == Position.LEFT) 0 else 1]

            return configStates.filter { state ->
                (configByte.toInt() and (1 shl state.configValue[0].toInt())) > 0
            }.toSet()
        }

    override fun transNewValue(value: Any): ByteArray {
        require((value as Set<*>).size >= 2) {
            "Require at least two modes selected, got: ${value.size}"
        }

        val valueByte = value
            .map { s -> 1 shl (s as String).toInt() }
            .reduce { a, b -> a + b }
            .toByte()

        return if (position == Position.LEFT) {
            byteArrayOf(valueByte, VALUE_NOT_MODIFIED)
        } else {
            byteArrayOf(VALUE_NOT_MODIFIED, valueByte)
        }
    }

    companion object {
        private val TAG = ListController::class.java.simpleName
        private const val DEBUG = true

        private const val VALUE_NOT_MODIFIED: Byte = -1
    }
}
