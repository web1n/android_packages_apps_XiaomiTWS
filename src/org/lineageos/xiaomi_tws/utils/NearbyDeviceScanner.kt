package org.lineageos.xiaomi_tws.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.and

class NearbyDeviceScanner(context: Context) {

    interface NearbyDeviceListener {
        fun onDevicesChanged(devices: Set<NearbyDevice>)
        fun onScanError(errorCode: Int) {}
    }

    data class NearbyDevice(private val address: String, val discoverable: Boolean) {
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)

        companion object {
            fun fromScanResult(scanResult: ScanResult): NearbyDevice? {
                val manufacturerData = scanResult.scanRecord
                    ?.manufacturerSpecificData
                    ?.get(XIAOMI_MANUFACTURER_ID)
                if (manufacturerData?.size != EXPECTED_DATA_LENGTH) {
                    return null
                }

                val isEncrypted = (manufacturerData[7] and 0x01) != 0.toByte()
                val macAddress = extractMacAddress(manufacturerData, isEncrypted)
                val discoverable = (manufacturerData[6] and 0x40) != 0.toByte()

                return NearbyDevice(macAddress, discoverable)
            }

            private fun extractMacAddress(data: ByteArray, isEncrypted: Boolean): String {
                val offset = if (isEncrypted) 18 else 11
                val addressBytes = byteArrayOf(
                    data[1 + offset], data[offset], data[2 + offset],
                    data[5 + offset], data[4 + offset], data[3 + offset]
                )
                return addressBytes.toHexString(":")
            }
        }
    }

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
        val newDevices = results
            .mapNotNull { NearbyDevice.fromScanResult(it) }
            .toSet()

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

        private const val XIAOMI_MANUFACTURER_ID = 0x038F
        private const val EXPECTED_DATA_LENGTH = 24
        private const val SCAN_REPORT_DELAY: Long = 5000

        private val FAST_CONNECT_FILTERS = listOf(
            ScanFilter.Builder()
                .setManufacturerData(XIAOMI_MANUFACTURER_ID, byteArrayOf(0x16, 0x01))
                .build()
        )
        private val FAST_CONNECT_SETTINGS = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(SCAN_REPORT_DELAY)
            .build()
    }
}
