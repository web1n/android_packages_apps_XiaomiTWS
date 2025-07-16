package org.lineageos.xiaomi_tws.nearby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanRecord
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner.Companion.UUID_FAST_CONNECT
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner.Companion.XIAOMI_MANUFACTURER_ID
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import kotlin.experimental.and

data class NearbyDevice(val address: String, val vid: Int, val pid: Int) {

    val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
    val name = getDeviceName(vid, pid)

    companion object {
        private const val EXPECTED_DATA_LENGTH = 24

        private val DEVICE_NAMES = buildMap<Pair<Int, Int>, String> {
            put(0x2717 to 0x5025, "Xiaomi Buds 3 Pro")
            put(0x2717 to 0x5026, "Xiaomi Buds 3")
            put(0x2717 to 0x5027, "Redmi Buds 3")
            put(0x2717 to 0x502A, "Redmi Buds 3")
            put(0x2717 to 0x502B, "Xiaomi Buds 3")
            put(0x2717 to 0x502D, "Xiaomi Buds 3T Pro")
            put(0x2717 to 0x5034, "Redmi Buds 4")
            put(0x2717 to 0x5035, "Xiaomi Buds 4 Pro")
            put(0x2717 to 0x5037, "Redmi Buds 4")
            put(0x2717 to 0x503B, "Xiaomi Buds 4 Pro")
            put(0x2717 to 0x5044, "Xiaomi Buds 4")
            put(0x2717 to 0x505A, "Xiaomi Buds 3 Star Wars")
            put(0x2717 to 0x505D, "Redmi Buds 4 Harry Potter")
            put(0x2717 to 0x505E, "Xiaomi Buds 3 Star Wars")
            put(0x2717 to 0x505F, "Redmi Buds 4 Active")
            put(0x2717 to 0x5066, "Xiaomi Buds 3 Disney")
            put(0x2717 to 0x5069, "Redmi Buds 4 Active")
            put(0x2717 to 0x506A, "Redmi Buds 5")
            put(0x2717 to 0x506B, "Redmi Buds 5 AAPE")
            put(0x2717 to 0x506C, "Redmi Buds 5 Pro")
            put(0x2717 to 0x506D, "Redmi Buds 5 Pro")
            put(0x2717 to 0x506F, "Redmi Buds 5 Pro Gaming")
            put(0x2717 to 0x5075, "Redmi Buds 5")
            put(0x2717 to 0x507F, "Xiaomi OpenWear Stereo")
            put(0x2717 to 0x5080, "Xiaomi OpenWear Stereo")
            put(0x2717 to 0x5081, "Xiaomi Buds 5")
            put(0x2717 to 0x5082, "Xiaomi Buds 5")
            put(0x2717 to 0x5088, "Redmi Buds 6 Active")
            put(0x2717 to 0x5089, "Redmi Buds 6 Active")
            put(0x2717 to 0x508A, "Redmi Buds 6 Lite")
            put(0x2717 to 0x508B, "Redmi Buds 6 Lite")
            put(0x2717 to 0x5095, "Redmi Buds 6S")
            put(0x2717 to 0x509A, "REDMI Buds SE")
            put(0x2717 to 0x509B, "Redmi Buds 6 Play")
            put(0x2717 to 0x509C, "Xiaomi Air4 SE")
            put(0x2717 to 0x509D, "REDMI Buds 6 Pro")
            put(0x2717 to 0x509E, "Redmi Buds 6 Pro")
            put(0x2717 to 0x509F, "Redmi Buds 6")
            put(0x2717 to 0x50A0, "Redmi Buds 6")
            put(0x2717 to 0x50AB, "Xiaomi Buds 5 Pro Wi-Fi")
            put(0x2717 to 0x50AC, "Xiaomi Buds 5 Pro Wi-Fi")
            put(0x2717 to 0x50AD, "Xiaomi Buds 5 Pro")
            put(0x2717 to 0x50AF, "REDMI Buds 6 Pro Gaming")
            put(0x2717 to 0x50B4, "Xiaomi Buds 5 Pro")
            put(0x2717 to 0x50B9, "REDMI Buds 7S")
            put(0x5A4D to 0xEA03, "Redmi AirDots 3 Pro")
            put(0x5A4D to 0xEA0D, "Redmi AirDots 3 Pro Genshin Impact")
            put(0x5A4D to 0xEA0E, "Redmi Buds 4 Pro")
            put(0x5A4D to 0xEA0F, "Redmi Buds 4 Pro")
        }

        fun getDeviceName(vid: Int, pid: Int): String {
            return DEVICE_NAMES[vid to pid] ?: "Unknown Device ($vid,$pid)"
        }

        fun fromScanRecord(scanRecord: ScanRecord): NearbyDevice {
            val manufacturerData = scanRecord.getManufacturerSpecificData(XIAOMI_MANUFACTURER_ID)
            val fastConnectData = scanRecord.serviceData?.get(UUID_FAST_CONNECT)
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
