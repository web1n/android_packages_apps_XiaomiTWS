package org.lineageos.xiaomi_tws.nearby

interface NearbyDeviceListener {
    fun onDevicesChanged(devices: Set<NearbyDevice>)
    fun onScanError(errorCode: Int) {}
}
