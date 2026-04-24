package org.lineageos.xiaomi_tws.nearby

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.content.Context
import org.lineageos.xiaomi_tws.features.DeviceBattery
import org.lineageos.xiaomi_tws.features.DeviceModel
import org.lineageos.xiaomi_tws.features.DeviceModel.ProductID
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner.Companion.UUID_FAST_CONNECT
import org.lineageos.xiaomi_tws.nearby.NearbyDeviceScanner.Companion.XIAOMI_MANUFACTURER_ID
import org.lineageos.xiaomi_tws.utils.BluetoothUtils.getBluetoothAdapter
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.isBitSet
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import org.lineageos.xiaomi_tws.utils.SettingsUtils

data class NearbyDevice(
    val address: String?,
    val accountKey: String,
    val productID: ProductID,
    val battery: DeviceBattery?
) {

    val device = address?.let { getBluetoothAdapter().getRemoteDevice(address) }

    val model = DeviceModel.from(productID)
    val name = model?.marketName ?: "Unknown Device ($productID)"

    fun isValidAccountKey(): Boolean {
        return accountKey != "0000000000"
    }

    fun getDevice(context: Context): BluetoothDevice? {
        if (device != null) {
            return device
        }

        return SettingsUtils.getInstance(context).getDeviceForAccountKey(accountKey)
    }

    companion object {
        private const val EXPECTED_DATA_LENGTH = 24

        fun fromScanRecord(scanRecord: ScanRecord): NearbyDevice {
            val manufacturerData = scanRecord.getManufacturerSpecificData(XIAOMI_MANUFACTURER_ID)
            val fastConnectData = scanRecord.serviceData?.get(UUID_FAST_CONNECT)
            require(manufacturerData == null || manufacturerData.size == EXPECTED_DATA_LENGTH)
            require(fastConnectData != null && fastConnectData.size == EXPECTED_DATA_LENGTH)

            val macAddress = manufacturerData?.let { extractMacAddress(manufacturerData) }
            val accountKey = extractAccountKey(fastConnectData)
            val productID = extractProductId(fastConnectData)
            val battery = extractBattery(fastConnectData)

            return NearbyDevice(macAddress, accountKey, productID, battery)
        }

        private fun extractMacAddress(manufacturerData: ByteArray): String {
            val isEncrypted = manufacturerData[7].isBitSet(0)
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

        private fun extractProductId(serviceData: ByteArray): ProductID {
            val vid = bytesToInt(serviceData[18], serviceData[19], false)
            val pid = bytesToInt(serviceData[16], serviceData[17], false)

            return ProductID(vid, pid)
        }

        private fun extractBattery(serviceData: ByteArray): DeviceBattery? {
            return DeviceBattery.fromBytes(serviceData[13], serviceData[12], serviceData[14])
        }

        private fun extractAccountKey(serviceData: ByteArray): String {
            return serviceData.copyOfRange(6, 11).reversedArray().toHexString()
        }
    }
}
