package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.softwareVersion
import org.lineageos.xiaomi_tws.mma.MMAManager

class DeviceInfoController(preferenceKey: String, device: BluetoothDevice) :
    BaseConfigController<Preference>(preferenceKey, device) {

    private var softwareVersion: String? = null

    override suspend fun initData(manager: MMAManager) {
        softwareVersion = manager.request(device, softwareVersion())
    }

    override fun postUpdateValue(preference: Preference) {
        val content = "%s\nvid: 0x%04X, pid: 0x%04X, software: %s".format(
            model?.marketName ?: "Unknown Device",
            productID.vendorId, productID.productId,
            softwareVersion
        )
        preference.summary = content
    }

}
