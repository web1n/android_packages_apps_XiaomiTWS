package org.lineageos.xiaomi_tws.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.media.session.PlaybackState.STATE_PLAYING
import android.util.Log
import com.android.settingslib.media.BluetoothMediaDevice
import com.android.settingslib.media.LocalMediaManager
import com.android.settingslib.media.LocalMediaManager.MediaDeviceState.STATE_SELECTED
import com.android.settingslib.media.MediaDevice
import com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE
import java.util.concurrent.ConcurrentHashMap

class MediaManager(context: Context) : LocalMediaManager.DeviceCallback, MediaController.Callback(),
    MediaSessionManager.OnActiveSessionsChangedListener {

    interface MediaPlayingListener {
        fun onAnyMediaPlaying()
    }

    private val localMediaManager = LocalMediaManager(context, context.packageName)
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)

    private val mediaDevices = ConcurrentHashMap.newKeySet<MediaDevice>()
    private val mediaPlayingListeners = ConcurrentHashMap.newKeySet<MediaPlayingListener>()
    private val registeredMediaControllers = ConcurrentHashMap.newKeySet<MediaController>()

    override fun onDeviceListUpdate(devices: List<MediaDevice>) {
        mediaDevices.clear()
        mediaDevices.addAll(devices)
    }

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        unregisterAllMediaControllers()

        controllers?.forEach {
            it.registerCallback(this@MediaManager)
            registeredMediaControllers.add(it)
        }
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        val anyPlaying = mediaSessionManager.getActiveSessions(null).any { it.isPlaying() }
        if (anyPlaying) notifyPlayingListeners()
    }

    fun startScan() {
        if (DEBUG) Log.d(TAG, "Starting media scan")

        registerMediaSessionManager()
        registerLocalMediaManager()
    }

    fun stopScan() {
        if (DEBUG) Log.d(TAG, "Stopping media scan")

        unregisterMediaSessionManager()
        unregisterLocalMediaManager()
        mediaDevices.clear()
    }

    private fun registerMediaSessionManager() {
        mediaSessionManager.runCatching {
            addOnActiveSessionsChangedListener(this@MediaManager, null)

            onActiveSessionsChanged(getActiveSessions(null))
        }.onFailure {
            Log.e(TAG, "Failed to init media session manager", it)
        }
    }

    private fun registerLocalMediaManager() {
        localMediaManager.runCatching {
            registerCallback(this@MediaManager)
            startScan()
        }.onFailure {
            Log.e(TAG, "Failed to init local media manager", it)
        }
    }

    private fun unregisterMediaSessionManager() {
        unregisterAllMediaControllers()

        mediaSessionManager.runCatching {
            removeOnActiveSessionsChangedListener(this@MediaManager)
        }.onFailure {
            Log.e(TAG, "Failed to cleanup media session manager", it)
        }
    }

    private fun unregisterLocalMediaManager() {
        localMediaManager.runCatching {
            unregisterCallback(this@MediaManager)
            stopScan()
        }.onFailure {
            Log.e(TAG, "Failed to cleanup local media manager", it)
        }
    }

    private fun unregisterAllMediaControllers() {
        registeredMediaControllers.forEach { controller ->
            controller.runCatching {
                unregisterCallback(this@MediaManager)
            }.onFailure {
                Log.w(TAG, "Failed to unregister controller: $controller", it)
            }
        }
        registeredMediaControllers.clear()
    }

    fun addPlayingListener(listener: MediaPlayingListener) {
        mediaPlayingListeners.add(listener)
    }

    fun removeListener(listener: MediaPlayingListener) {
        mediaPlayingListeners.remove(listener)
    }

    private fun notifyPlayingListeners() {
        mediaPlayingListeners.forEach { listener ->
            listener.runCatching {
                onAnyMediaPlaying()
            }.onFailure {
                Log.e(TAG, "Error notifying listener: $listener", it)
            }
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
            localMediaManager.connectDevice(mediaDevice)
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
        fun MediaDevice.isSelected() = state == STATE_SELECTED
    }

}
