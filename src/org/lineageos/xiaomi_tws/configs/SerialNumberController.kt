package org.lineageos.xiaomi_tws.configs

import android.content.Context
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_SN
import java.nio.charset.StandardCharsets

class SerialNumberController(
    context: Context,
    preferenceKey: String
) : ConfigController(context, preferenceKey) {

    override val configId = XIAOMI_MMA_CONFIG_SN
    override val expectedConfigLength = 20

    override val summary: String?
        get() = configValue?.let {
            runCatching { String(it, StandardCharsets.UTF_8) }.getOrNull()
        }

    override fun transNewValue(value: Any) = TODO()

}
