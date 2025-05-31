package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.softwareVersion
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.vidPid
import org.lineageos.xiaomi_tws.mma.MMAManager

class DeviceInfoController(preferenceKey: String, device: BluetoothDevice) :
    BaseConfigController<Preference>(preferenceKey, device) {

    var vid: Int? = null
    var pid: Int? = null
    var softwareVersion: String? = null

    override suspend fun initData(manager: MMAManager) {
        val (vid, pid) = manager.request(device, vidPid())
        this.vid = vid
        this.pid = pid

        softwareVersion = manager.request(device, softwareVersion())
    }

    override fun postUpdateValue(preference: Preference) {
        preference.summary = "vid: $vid, pid: $pid, software: $softwareVersion"

        super.postUpdateValue(preference)
    }

}
