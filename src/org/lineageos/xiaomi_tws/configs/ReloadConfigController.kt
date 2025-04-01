package org.lineageos.xiaomi_tws.configs

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.settingslib.widget.BannerMessagePreference
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_SN
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.fragments.EarbudsInfoFragment

class ReloadConfigController(
    context: Context,
    preferenceKey: String
) : ConfigController(context, preferenceKey) {

    override val configId = XIAOMI_MMA_CONFIG_SN
    override val expectedConfigLength = 20

    override val isAvailable
        get() = when (super.isAvailable) {
            Available.AVAILABLE -> Available.UNAVAILABLE
            else -> Available.AVAILABLE
        }

    override fun displayPreference(preference: Preference) {
        super.displayPreference(preference)

        (preference as BannerMessagePreference).apply {
            setPositiveButtonText(R.string.reconnect_device)
            setPositiveButtonOnClickListener {
                val intent = Intent(EarbudsInfoFragment.ACTION_RELOAD_CONFIG).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
            }
        }
    }

}
