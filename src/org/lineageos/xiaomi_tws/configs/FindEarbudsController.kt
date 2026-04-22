package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import androidx.preference.TwoStatePreference
import org.lineageos.xiaomi_tws.features.DeviceBattery
import org.lineageos.xiaomi_tws.mma.ConfigData
import org.lineageos.xiaomi_tws.mma.ConfigData.FindEarbuds.Position
import org.lineageos.xiaomi_tws.mma.DeviceEvent
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.batteryInfo
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.FindEarbuds

class FindEarbudsController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<TwoStatePreference, Boolean, ConfigData.FindEarbuds>(preferenceKey, device) {

    override val config = FindEarbuds

    private lateinit var status: DeviceBattery

    override suspend fun initData(manager: MMAManager) {
        super.initData(manager)

        status = manager.request(device, batteryInfo())
    }

    override fun postUpdateValue(preference: TwoStatePreference) {
        value?.let {
            preference.isChecked = it.enabled
        }
    }

    override fun onDeviceEvent(event: DeviceEvent) {
        super.onDeviceEvent(event)

        if (event is DeviceEvent.BatteryChanged) {
            status = event.battery
        }
    }

    override fun preferenceValueToValue(value: Boolean): ConfigData.FindEarbuds {
        val positions = if (value) {
            ArrayList<Position>().apply {
                if (status.left != null) add(Position.Left)
                if (status.right != null) add(Position.Right)
            }
        } else {
            listOf(Position.Left, Position.Right)
        }

        return ConfigData.FindEarbuds(value, positions)
    }

}
