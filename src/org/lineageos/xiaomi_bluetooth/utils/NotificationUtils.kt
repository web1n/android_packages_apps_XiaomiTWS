package org.lineageos.xiaomi_bluetooth.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_bluetooth.R
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds
import org.lineageos.xiaomi_bluetooth.fragments.EarbudsInfoFragment.Companion.ACTION_EARBUDS_INFO

object NotificationUtils {

    const val CHANNEL_ID_EARBUDS_INFO = "channel_earbuds_info"

    private fun createEarbudsNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID_EARBUDS_INFO,
            context.getString(R.string.notification_channel_earbuds_info),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            isBlockable = true
            setSound(null, null)
        }

        context.getSystemService(NotificationManager::class.java).run {
            if (getNotificationChannel(CHANNEL_ID_EARBUDS_INFO) == null) {
                createNotificationChannel(channel)
            }
        }
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS]
    )
    fun updateEarbudsNotification(context: Context, earbuds: Earbuds) {
        createEarbudsNotificationChannel(context)

        if (earbuds.caseOpened && earbuds.case.valid) {
            createEarbudsNotification(context, earbuds)
        } else {
            cancelEarbudsNotification(context, earbuds.device)
        }
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS]
    )
    private fun createEarbudsNotification(context: Context, earbuds: Earbuds) = earbuds.run {
        val contentString = ArrayList<String>().apply {
            if (left.valid) add("\uD83C\uDFA7 Left: ${left.battery}% ${if (left.charging) "charging" else ""}")
            if (right.valid) add("\uD83C\uDFA7 Right: ${right.battery}% ${if (right.charging) "charging" else ""}")
            if (case.valid) add("\uD83D\uDD0B Case: ${case.battery}% ${if (case.charging) "charging" else ""}")
        }.joinToString()

        val pendingIntent = Intent(ACTION_EARBUDS_INFO).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            setPackage(context.packageName)
        }.let {
            PendingIntent.getActivity(
                context, device.address.hashCode(), it, PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = Notification.Builder(context, CHANNEL_ID_EARBUDS_INFO).apply {
            setContentTitle(device.name)
            setContentText(contentString)
            setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            setVisibility(Notification.VISIBILITY_SECRET)
            setContentIntent(pendingIntent)
        }.build()

        context.getSystemService(NotificationManager::class.java)
            .notify(address.hashCode(), notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun cancelEarbudsNotification(context: Context, bluetoothDevice: BluetoothDevice) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(bluetoothDevice.address.hashCode())
    }

}
