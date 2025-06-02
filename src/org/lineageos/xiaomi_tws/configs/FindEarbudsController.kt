package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.SwitchPreference
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.batteryInfo
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.FindEarbuds
import org.lineageos.xiaomi_tws.mma.configs.FindEarbuds.Position

class FindEarbudsController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<SwitchPreference, Pair<Boolean, List<Position>>>(preferenceKey, device) {

    override val config = FindEarbuds()

    private lateinit var earbudsStatus: Earbuds

    override suspend fun initData(manager: MMAManager) {
        super.initData(manager)

        earbudsStatus = manager.request(device, batteryInfo())
    }

    override fun postUpdateValue(preference: SwitchPreference) {
        if (value == null) return

        preference.isChecked = value!!.first == true
    }

    override fun onDeviceEvent(event: DeviceEvent) {
        super.onDeviceEvent(event)

        if (event is DeviceEvent.BatteryChanged) {
            earbudsStatus = event.battery
        }
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: SwitchPreference,
        newValue: Any
    ): Boolean {
        val newConfigValue = if (newValue == true) {
            val positions = ArrayList<Position>().apply {
                if (earbudsStatus.left.valid) add(Position.Left)
                if (earbudsStatus.right.valid) add(Position.Right)
            }

            true to positions
        } else {
            false to listOf(Position.Left, Position.Right)
        }

        return super.onPreferenceChange(manager, preference, newConfigValue)
    }

}
