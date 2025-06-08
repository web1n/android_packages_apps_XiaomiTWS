package org.lineageos.xiaomi_tws.nearby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner.Companion.UUID_FAST_CONNECT
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner.Companion.XIAOMI_MANUFACTURER_ID
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import kotlin.experimental.and

data class NearbyDevice(val address: String, val vid: Int, val pid: Int) {

    val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)

    companion object {
        private const val EXPECTED_DATA_LENGTH = 24

        fun fromScanResult(scanResult: ScanResult): NearbyDevice {
            val manufacturerData = scanResult.scanRecord
                ?.getManufacturerSpecificData(XIAOMI_MANUFACTURER_ID)
            val fastConnectData = scanResult.scanRecord?.serviceData?.get(UUID_FAST_CONNECT)
            require(manufacturerData != null && manufacturerData.size == EXPECTED_DATA_LENGTH)
            require(fastConnectData != null && fastConnectData.size == EXPECTED_DATA_LENGTH)

            val macAddress = extractMacAddress(manufacturerData)
            val (vid, pid) = extractVidPid(fastConnectData)

            return NearbyDevice(macAddress, vid, pid)
        }

        private fun extractMacAddress(manufacturerData: ByteArray): String {
            val isEncrypted = (manufacturerData[7] and 0x01) != 0.toByte()
            val offset = if (isEncrypted) 18 else 11

            val addressBytes = byteArrayOf(
                manufacturerData[1 + offset],
                manufacturerData[offset],
                manufacturerData[2 + offset],
                manufacturerData[5 + offset],
                manufacturerData[4 + offset],
                manufacturerData[3 + offset]
            )
            return addressBytes.toHexString(":")
        }

        private fun extractVidPid(serviceData: ByteArray): Pair<Int, Int> {
            val vid = bytesToInt(serviceData[18], serviceData[19], false)
            val pid = bytesToInt(serviceData[16], serviceData[17], false)

            return Pair(vid, pid)
        }
    }
}
