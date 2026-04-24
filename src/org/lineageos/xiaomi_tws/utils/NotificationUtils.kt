package org.lineageos.xiaomi_tws.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.features.DeviceBattery
import org.lineageos.xiaomi_tws.fragments.DeviceConfigFragment.Companion.ACTION_DEVICE_CONFIG

object NotificationUtils {

    const val CHANNEL_ID_EARBUDS_INFO = "channel_earbuds_info"

    private fun createEarbudsNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID_EARBUDS_INFO,
            context.getString(R.string.notification_channel_device_info),
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
    fun updateEarbudsNotification(
        context: Context,
        device: BluetoothDevice,
        battery: DeviceBattery
    ) {
        createEarbudsNotificationChannel(context)

        if (battery.case != null) {
            createEarbudsNotification(context, device, battery)
        } else {
            cancelEarbudsNotification(context, device)
        }
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS]
    )
    private fun createEarbudsNotification(
        context: Context,
        device: BluetoothDevice,
        battery: DeviceBattery
    ) = battery.run {
        val pendingIntent = Intent(ACTION_DEVICE_CONFIG).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            setPackage(context.packageName)
        }.let {
            PendingIntent.getActivity(
                context, device.address.hashCode(), it, PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = Notification.Builder(context, CHANNEL_ID_EARBUDS_INFO).apply {
            setContentTitle(device.alias)
            setContentText(readableString)
            setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            setVisibility(Notification.VISIBILITY_SECRET)
            setContentIntent(pendingIntent)
        }.build()

        context.getSystemService(NotificationManager::class.java)
            .notify(device.address.hashCode(), notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun cancelEarbudsNotification(context: Context, bluetoothDevice: BluetoothDevice) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(bluetoothDevice.address.hashCode())
    }

}
