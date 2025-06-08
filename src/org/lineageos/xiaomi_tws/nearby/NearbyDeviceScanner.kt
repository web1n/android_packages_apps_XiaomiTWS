package org.lineageos.xiaomi_tws.nearby

import android.Manifest
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class NearbyDeviceScanner private constructor(context: Context) {

    private val bluetoothAdapter = context.getBluetoothAdapter()
    private val nearbyListeners = ConcurrentHashMap.newKeySet<NearbyDeviceListener>()

    private val isScanning = AtomicBoolean(false)

    val devices = ConcurrentHashMap.newKeySet<NearbyDevice>()

    private val bluetoothScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            handleScanResults(results)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) onBatchScanResults(listOf(result))
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Bluetooth scan failed with error code: $errorCode")

            isScanning.set(false)
            notifyNewDevices(emptySet())
            notifyError(errorCode)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan(): Boolean {
        if (DEBUG) Log.d(TAG, "Start scanning")
        if (isScanning.get()) {
            Log.w(TAG, "Scan is already in progress")
            return true
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (!bluetoothAdapter.isLeEnabled || scanner == null) {
            Log.w(TAG, "Bluetooth is not enabled")
            return false
        }

        return scanner.runCatching {
            startScan(FAST_CONNECT_FILTERS, FAST_CONNECT_SETTINGS, bluetoothScanCallback)
        }.onSuccess {
            isScanning.set(true)
        }.onFailure {
            isScanning.set(false)
            Log.e(TAG, "Failed to start scan", it)
        }.isSuccess
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (DEBUG) Log.d(TAG, "Stop scanning")
        if (!isScanning.get()) {
            if (DEBUG) Log.d(TAG, "Scan is not running")
            return
        }

        bluetoothAdapter.bluetoothLeScanner?.runCatching {
            stopScan(bluetoothScanCallback)
        }?.onFailure {
            Log.e(TAG, "Failed to stop scan", it)
        }

        isScanning.set(false)
        notifyNewDevices(emptySet())
    }

    fun isScanning() = isScanning.get()

    fun registerNearbyListener(listener: NearbyDeviceListener) {
        nearbyListeners.add(listener)
    }

    fun unregisterNearbyListener(listener: NearbyDeviceListener) {
        nearbyListeners.remove(listener)
    }

    private fun handleScanResults(results: List<ScanResult>) {
        val newDevices = results.mapNotNull {
            NearbyDevice.runCatching { fromScanResult(it) }
                .onFailure { Log.e(TAG, "Failed to parse scan result:", it) }
                .getOrNull()
        }.toSet()

        notifyNewDevices(newDevices)
    }

    private fun notifyNewDevices(newDevices: Set<NearbyDevice>) {
        if (devices.toSet() == newDevices) return

        devices.apply {
            clear()
            addAll(newDevices)
        }

        nearbyListeners.forEach { listener ->
            listener.runCatching { onDevicesChanged(devices.toSet()) }
                .onFailure { Log.e(TAG, "Error notifying scan listener", it) }
        }
    }

    private fun notifyError(errorCode: Int) {
        nearbyListeners.forEach { listener ->
            listener.runCatching { onScanError(errorCode) }
                .onFailure { Log.e(TAG, "Error notifying scan listener of error", it) }
        }
    }

    companion object {
        private val TAG = NearbyDeviceScanner::class.simpleName
        private const val DEBUG = true

        const val XIAOMI_MANUFACTURER_ID = 0x038F
        private const val SCAN_REPORT_DELAY: Long = 5000

        val UUID_FAST_CONNECT = ParcelUuid.fromString("0000FD2D-0000-1000-8000-00805f9b34fb")

        private val FAST_CONNECT_FILTERS = listOf(
            ScanFilter.Builder()
                .setManufacturerData(XIAOMI_MANUFACTURER_ID, byteArrayOf(0x16, 0x01))
                .setServiceData(UUID_FAST_CONNECT, byteArrayOf())
                .build()
        )
        private val FAST_CONNECT_SETTINGS = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(SCAN_REPORT_DELAY)
            .build()

        @Volatile
        private var instance: NearbyDeviceScanner? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: NearbyDeviceScanner(context.applicationContext).also {
                instance = it
            }
        }
    }
}
