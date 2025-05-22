package org.lineageos.xiaomi_tws

import android.app.Application
import android.content.Intent
import android.util.Log
import org.lineageos.xiaomi_tws.mma.MMAManager

class PersistentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        MMAManager.getInstance(this)
        startService(Intent(this, EarbudsService::class.java))
    }

    companion object {
        private val TAG = PersistentApplication::class.java.simpleName
        private const val DEBUG = true
    }
}
