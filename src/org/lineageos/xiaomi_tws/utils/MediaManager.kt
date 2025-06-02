package org.lineageos.xiaomi_tws.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState.STATE_PLAYING
import android.util.Log
import com.android.settingslib.media.BluetoothMediaDevice
import com.android.settingslib.media.LocalMediaManager
import com.android.settingslib.media.MediaDevice
import com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE

class MediaManager(context: Context) : LocalMediaManager.DeviceCallback {

    private val localMediaManager = LocalMediaManager(context, context.packageName)
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)

    private val mediaDevices = ArrayList<MediaDevice>()

    override fun onDeviceListUpdate(devices: List<MediaDevice>) {
        synchronized(mediaDevices) {
            mediaDevices.clear()
            mediaDevices.addAll(devices)
        }
    }

    fun startScan() {
        try {
            localMediaManager.registerCallback(this)
            localMediaManager.startScan()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start media scan", e)
        }
    }

    fun stopScan() {
        try {
            localMediaManager.unregisterCallback(this)
            localMediaManager.stopScan()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop media scan", e)
        }
    }

    fun getBluetoothMediaDevice(device: BluetoothDevice): MediaDevice? {
        return mediaDevices.find { it is BluetoothMediaDevice && it.id == device.address }
    }

    fun getBuiltinMediaDevice(): MediaDevice? {
        return mediaDevices.find { it.deviceType == TYPE_PHONE_DEVICE }
    }

    fun connectDevice(mediaDevice: MediaDevice): Boolean {
        if (DEBUG) Log.d(TAG, "connectDevice: $mediaDevice")
        if (localMediaManager.currentConnectedDevice == mediaDevice) {
            return true
        }

        return try {
            return localMediaManager.connectDevice(mediaDevice)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transfer media to device $mediaDevice", e)
            false
        }
    }

    fun playMedia() {
        if (DEBUG) Log.d(TAG, "playMedia")

        val activeSessions = mediaSessionManager.getActiveSessions(null)
        activeSessions.find { it.isPlaying() }?.let {
            if (DEBUG) Log.d(TAG, "Media is already playing on session: $it")
            return
        }

        val latestSession = activeSessions.find { !it.isPlaying() } ?: return
        latestSession.runCatching {
            transportControls.play()
        }.onFailure {
            Log.e(TAG, "Failed to play media on session: $it")
        }
    }

    fun pauseMedia() {
        if (DEBUG) Log.d(TAG, "pauseMedia")

        mediaSessionManager.getActiveSessions(null)
            .filter { it.isPlaying() }
            .forEach { session ->
                session.runCatching {
                    transportControls.pause()
                }.onFailure {
                    Log.e(TAG, "Failed to pause media on session: $it")
                }
            }
    }

    companion object {
        private val TAG = MediaManager::class.simpleName
        private const val DEBUG = true

        private fun MediaController.isPlaying() = playbackState?.state == STATE_PLAYING
    }

}
