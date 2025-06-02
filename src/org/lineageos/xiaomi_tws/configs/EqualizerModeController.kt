package org.lineageos.xiaomi_tws.configs

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.preference.ListPreference
import org.lineageos.xiaomi_tws.R
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.vidPid
import org.lineageos.xiaomi_tws.mma.MMAManager
import org.lineageos.xiaomi_tws.mma.configs.EqualizerMode
import org.lineageos.xiaomi_tws.mma.configs.EqualizerMode.Mode

class EqualizerModeController(preferenceKey: String, device: BluetoothDevice) :
    ConfigController<ListPreference, Mode>(preferenceKey, device) {

    data class EqualizerDevice(val vendorId: Int, val productId: Int, val supportedModes: Set<Int>)

    override val config = EqualizerMode()

    private var vid: Int = 0
    private var pid: Int = 0

    override suspend fun initData(manager: MMAManager) {
        super.initData(manager)

        val (vid, pid) = manager.request(device, vidPid())
        this.vid = vid
        this.pid = pid
    }

    override fun preInitView(preference: ListPreference) {
        preference.isPersistent = false
        preference.isSelectable = false

        super.preInitView(preference)
    }

    override fun postInitView(preference: ListPreference) {
        preference.isSelectable = true

        val supportedModes = getSupportedModes()
        preference.entries = supportedModes
            .map { modeToString(preference.context, it) }
            .toTypedArray()
        preference.entryValues = supportedModes
            .map { it.name }
            .toTypedArray()
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        super.postInitView(preference)
    }

    override fun postUpdateValue(preference: ListPreference) {
        if (value == null) return

        preference.value = value!!.name

        super.postUpdateValue(preference)
    }

    override suspend fun onPreferenceChange(
        manager: MMAManager,
        preference: ListPreference,
        newValue: Any
    ): Boolean {
        val newConfigValue = Mode.valueOf(newValue as String)

        return super.onPreferenceChange(manager, preference, newConfigValue)
    }

    private fun getSupportedModes(): Set<Mode> {
        return DEVICE_SUPPORTED_MODES
            .find { it.vendorId == vid && it.productId == pid }
            ?.supportedModes
            ?.mapNotNull { Mode.fromValue(it.toByte()) }
            ?.toSet()
            ?: return setOf(Mode.Default)
    }

    private fun modeToString(context: Context, mode: Mode): String {
        val stringRes = when (mode) {
            Mode.Default -> R.string.equalizer_mode_default
            Mode.VocalEnhance -> R.string.equalizer_mode_vocal_enhance
            Mode.BassBoost -> R.string.equalizer_mode_bass_boost
            Mode.TrebleBoost -> R.string.equalizer_mode_treble_boost
            Mode.VolumeBoost -> R.string.equalizer_mode_volume_boost
            Mode.Harman -> R.string.equalizer_mode_harman
            Mode.HarmanMaster -> R.string.equalizer_mode_harman_master
        }

        return context.getString(stringRes)
    }

    companion object {
        private val DEVICE_SUPPORTED_MODES = arrayOf(
            EqualizerDevice(0x2717, 0x5035, setOf(0, 1, 5, 6, 11, 12)),
            EqualizerDevice(0x2717, 0x503B, setOf(0, 5, 6, 12)),
            EqualizerDevice(0x2717, 0x506A, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x506B, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x506C, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x506D, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x506F, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x5075, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x507F, setOf(0, 1, 6)),
            EqualizerDevice(0x2717, 0x5080, setOf(0, 1, 6)),
            EqualizerDevice(0x2717, 0x5081, setOf(1, 6, 10, 11, 13, 14)),
            EqualizerDevice(0x2717, 0x5082, setOf(1, 6, 10, 11, 13, 14)),
            EqualizerDevice(0x2717, 0x5088, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x5089, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x508A, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x508B, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x5095, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x2717, 0x509A, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x509B, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x509C, setOf(0, 1, 5, 6, 7)),
            EqualizerDevice(0x2717, 0x509D, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x509E, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x509F, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x50A0, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x50AB, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50AC, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50AD, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50AF, setOf(0, 1, 5, 6, 10)),
            EqualizerDevice(0x2717, 0x50B4, setOf(1, 6, 10, 12, 13, 14, 15)),
            EqualizerDevice(0x2717, 0x50B9, setOf(0, 1, 5, 6, 7, 10)),
            EqualizerDevice(0x5A4D, 0xEA03, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x5A4D, 0xEA0D, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x5A4D, 0xEA0E, setOf(0, 1, 5, 6)),
            EqualizerDevice(0x5A4D, 0xEA0F, setOf(0, 1, 5, 6))
        )

    }
}
