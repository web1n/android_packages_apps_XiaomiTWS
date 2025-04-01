package org.lineageos.xiaomi_tws

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.lineageos.xiaomi_tws.utils.ATUtils

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED != intent.action) {
            return
        }
        if (DEBUG) Log.d(TAG, "boot completed")

        ATUtils.checkIfSupportXiaomiATCommand()

        context.startService(Intent(context, EarbudsService::class.java))
    }

    companion object {
        private val TAG = BootCompletedReceiver::class.java.simpleName
        private const val DEBUG = true
    }
}
