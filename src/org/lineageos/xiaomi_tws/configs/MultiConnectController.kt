package org.lineageos.xiaomi_tws.configs

import android.content.Context
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_MULTI_CONNECT
import org.lineageos.xiaomi_tws.R

class MultiConnectController(context: Context, preferenceKey: String) :
    SwitchController(context, preferenceKey) {

    override val enabledState = ConfigState(byteArrayOf(1), R.string.multi_connect_summary)
    override val disabledState = ConfigState(byteArrayOf(0), R.string.multi_connect_summary)

    override val configId = XIAOMI_MMA_CONFIG_MULTI_CONNECT
    override val expectedConfigLength = 1

}
