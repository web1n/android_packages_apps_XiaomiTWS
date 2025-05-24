package org.lineageos.xiaomi_tws

import android.app.Application
import android.content.Intent
import android.util.Log
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.utils.SettingsUtils
import kotlin.properties.Delegates

class PersistentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        enableSystemIntegration = SettingsUtils.getInstance(this).enableSystemIntegration
        MMAManager.getInstance(this)
        if (enableSystemIntegration) {
            startService(Intent(this, EarbudsService::class.java))
        }
    }

    companion object {
        private val TAG = PersistentApplication::class.java.simpleName
        private const val DEBUG = true

        var enableSystemIntegration by Delegates.notNull<Boolean>()
            private set
    }
}
