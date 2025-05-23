package org.lineageos.xiaomi_tws.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object CommonUtils {

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            setData(Uri.fromParts("package", activity.packageName, null))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(intent)
    }

}
