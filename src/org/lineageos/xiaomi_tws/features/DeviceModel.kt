package org.lineageos.xiaomi_tws.features

import org.lineageos.xiaomi_tws.mma.ConfigData.EqualizerMode
import org.lineageos.xiaomi_tws.mma.ConfigData.EqualizerMode.Mode.*

data class DeviceModel(
    val id: ProductID,
    val marketName: String,
    val equalizerModes: Set<EqualizerMode.Mode>? = null,
) {
    data class ProductID(val vendorId: Int, val productId: Int)

    companion object {
        val DEFAULT_EQUALIZER_MODES = setOf(Default)

        // --- Redmi Series ---
        val REDMI_AIRDOTS_3_PRO = DeviceModel(
            ProductID(0x5A4D, 0xEA03), "Redmi AirDots 3 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_AIRDOTS_3_PRO_GENSHIN = DeviceModel(
            ProductID(0x5A4D, 0xEA0D), "Redmi AirDots 3 Pro 原神版",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_3 = DeviceModel(
            ProductID(0x2717, 0x5027), "Redmi Buds 3",
        )
        val REDMI_BUDS_3_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x502A), "Redmi Buds 3",
        )
        val REDMI_BUDS_4 = DeviceModel(
            ProductID(0x2717, 0x5034), "Redmi Buds 4",
        )
        val REDMI_BUDS_4_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x5037), "Redmi Buds 4"
        )
        val REDMI_BUDS_4_ACTIVE = DeviceModel(
            ProductID(0x2717, 0x5069), "Redmi Buds 4 活力版"
        )
        val REDMI_BUDS_4_ACTIVE_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x505F), "Redmi Buds 4 Active"
        )
        val REDMI_BUDS_4_HARRY_POTTER = DeviceModel(
            ProductID(0x2717, 0x505D), "Redmi Buds 4 哈利·波特版"
        )
        val REDMI_BUDS_4_PRO = DeviceModel(
            ProductID(0x5A4D, 0xEA0E), "Redmi Buds 4 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_4_PRO_GLOBAL = DeviceModel(
            ProductID(0x5A4D, 0xEA0F), "Redmi Buds 4 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_5 = DeviceModel(
            ProductID(0x2717, 0x506A), "Redmi Buds 5",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_5_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x5075), "Redmi Buds 5",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_5_AAPE = DeviceModel(
            ProductID(0x2717, 0x506B), "Redmi Buds 5 AAPE",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_5_PRO = DeviceModel(
            ProductID(0x2717, 0x506C), "Redmi Buds 5 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_5_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x506D), "Redmi Buds 5 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_5_PRO_GAMING = DeviceModel(
            ProductID(0x2717, 0x506F), "Redmi Buds 5 Pro Gaming",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_6 = DeviceModel(
            ProductID(0x2717, 0x509F), "Redmi Buds 6",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_6_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50A0), "Redmi Buds 6",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_6_ACTIVE = DeviceModel(
            ProductID(0x2717, 0x5088), "Redmi Buds 6 活力版",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, VolumeBoost)
        )
        val REDMI_BUDS_6_ACTIVE_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x5089), "Redmi Buds 6 Active",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, VolumeBoost)
        )
        val REDMI_BUDS_6_LITE = DeviceModel(
            ProductID(0x2717, 0x508A), "Redmi Buds 6 青春版",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_6_LITE_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x508B), "Redmi Buds 6 Lite",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_6_PLAY_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x509B), "Redmi Buds 6 Play",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, VolumeBoost)
        )
        val REDMI_BUDS_6S = DeviceModel(
            ProductID(0x2717, 0x5095), "Redmi Buds 6S",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost)
        )
        val REDMI_BUDS_6_PRO = DeviceModel(
            ProductID(0x2717, 0x509D), "REDMI Buds 6 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_6_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x509E), "Redmi Buds 6 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_6_PRO_GAMING = DeviceModel(
            ProductID(0x2717, 0x50AF), "REDMI Buds 6 Pro 电竞版",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_7S = DeviceModel(
            ProductID(0x2717, 0x50B9), "REDMI Buds 7S",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_8_ACTIVE = DeviceModel(
            ProductID(0x2717, 0x50E1), "REDMI Buds 8 活力版",
            equalizerModes = setOf(
                VocalEnhance, BassBoost, TrebleBoost, VolumeBoost, Custom, BalancedListening
            )
        )
        val REDMI_BUDS_8_ACTIVE_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50E2), "REDMI Buds 8 Active",
            equalizerModes = setOf(
                VocalEnhance, BassBoost, TrebleBoost, VolumeBoost, Custom, BalancedListening
            )
        )
        val REDMI_BUDS_8_LITE = DeviceModel(
            ProductID(0x2717, 0x50EE), "REDMI Buds 8 青春版",
            equalizerModes = setOf(
                VocalEnhance, BassBoost, TrebleBoost, VolumeBoost, Custom, BalancedListening
            )
        )
        val REDMI_BUDS_8_LITE_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50EF), "REDMI Buds 8 Lite",
            equalizerModes = setOf(
                VocalEnhance, BassBoost, TrebleBoost, VolumeBoost, Custom, BalancedListening
            )
        )
        val REDMI_BUDS_8 = DeviceModel(
            ProductID(0x2717, 0x50F2), "REDMI Buds 8",
            equalizerModes = setOf(
                VocalEnhance, BassBoost, TrebleBoost, VolumeBoost, Custom, BalancedListening
            )
        )
        val REDMI_BUDS_8_PRO = DeviceModel(
            ProductID(0x2717, 0x50E3), "REDMI Buds 8 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_8_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50E5), "REDMI Buds 8 Pro",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, Custom)
        )
        val REDMI_BUDS_SE = DeviceModel(
            ProductID(0x2717, 0x509A), "REDMI Buds SE",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, VolumeBoost)
        )

        // --- Xiaomi Series ---
        val XIAOMI_AIR4_SE = DeviceModel(
            ProductID(0x2717, 0x509C), "Xiaomi Air4 SE",
            equalizerModes = setOf(Default, VocalEnhance, BassBoost, TrebleBoost, VolumeBoost)
        )
        val XIAOMI_BUDS_3 = DeviceModel(
            ProductID(0x2717, 0x5026), "Xiaomi Buds 3"
        )
        val XIAOMI_BUDS_3_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x502B), "Xiaomi Buds 3"
        )
        val XIAOMI_BUDS_3T_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x502D), "Xiaomi Buds 3T Pro"
        )
        val XIAOMI_BUDS_3_DISNEY = DeviceModel(
            ProductID(0x2717, 0x5066), "Xiaomi 真无线降噪耳机 3 迪士尼100周年限定版"
        )
        val XIAOMI_BUDS_3_PRO = DeviceModel(
            ProductID(0x2717, 0x5025), "Xiaomi Buds 3 Pro"
        )
        val XIAOMI_BUDS_3_STAR_WARS = DeviceModel(
            ProductID(0x2717, 0x505E), "Xiaomi Buds 3 Star Wars"
        )
        val XIAOMI_BUDS_3_STAR_WARS_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x505A), "Xiaomi Buds 3 Star Wars"
        )
        val XIAOMI_BUDS_4 = DeviceModel(
            ProductID(0x2717, 0x5044), "Xiaomi Buds 4"
        )
        val XIAOMI_BUDS_4_PRO = DeviceModel(
            ProductID(0x2717, 0x5035), "Xiaomi Buds 4 Pro",
            equalizerModes = setOf(
                Default, VocalEnhance, BassBoost, TrebleBoost, Classic, Legendary
            )
        )
        val XIAOMI_BUDS_4_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x503B), "Xiaomi Buds 4 Pro"
        )
        val XIAOMI_BUDS_5 = DeviceModel(
            ProductID(0x2717, 0x5081), "Xiaomi Buds 5",
            equalizerModes = setOf(
                Default, VocalEnhance, TrebleBoost, Custom, Classic, SoothingBoost
            )
        )
        val XIAOMI_BUDS_5_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x5082), "Xiaomi Buds 5",
            equalizerModes = setOf(
                Default, VocalEnhance, TrebleBoost, Custom, Classic, SoothingBoost
            )
        )
        val XIAOMI_BUDS_5_PRO = DeviceModel(
            ProductID(0x2717, 0x50AD), "Xiaomi Buds 5 Pro",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Custom, Legendary, SoothingBoost, Harman, HarmanMaster
            )
        )
        val XIAOMI_BUDS_5_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50B4), "Xiaomi Buds 5 Pro",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Custom, Legendary, SoothingBoost, Harman, HarmanMaster
            )
        )
        val XIAOMI_BUDS_5_PRO_WIFI = DeviceModel(
            ProductID(0x2717, 0x50AB), "Xiaomi Buds 5 Pro Wi-Fi",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Custom, Legendary, SoothingBoost, Harman, HarmanMaster
            )
        )
        val XIAOMI_BUDS_5_PRO_WIFI_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50AC), "Xiaomi Buds 5 Pro Wi-Fi",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Custom, Legendary, SoothingBoost, Harman, HarmanMaster
            )
        )
        val XIAOMI_BUDS_6 = DeviceModel(
            ProductID(0x2717, 0x50EA), "Xiaomi Buds 6",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Custom, SoothingBoost, Harman, HarmanMaster,
                BalancedListening
            )
        )
        val XIAOMI_OPEN_WEAR = DeviceModel(
            ProductID(0x2717, 0x507F), "Xiaomi 开放式耳机",
            equalizerModes = setOf(Default, VocalEnhance, TrebleBoost)
        )
        val XIAOMI_OPEN_WEAR_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x5080), "Xiaomi OpenWear Stereo",
            equalizerModes = setOf(Default, VocalEnhance, TrebleBoost)
        )
        val XIAOMI_OPEN_WEAR_PRO = DeviceModel(
            ProductID(0x2717, 0x50B8), "Xiaomi 开放式耳机 Pro",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Harman, HarmanMaster, BalancedListening
            )
        )
        val XIAOMI_OPEN_WEAR_PRO_GLOBAL = DeviceModel(
            ProductID(0x2717, 0x50D7), "Xiaomi OpenWear Stereo Pro",
            equalizerModes = setOf(
                VocalEnhance, TrebleBoost, Harman, HarmanMaster, BalancedListening
            )
        )
        val XIAOMI_BONE_CONDUCTION_2 = DeviceModel(
            ProductID(0x2717, 0x50B5), "Xiaomi 骨传导耳机2",
            equalizerModes = setOf(Standard, Outdoor, UnderWater)
        )

        // --- Series Grouping ---
        val REDMI_AIRDOTS_SERIES = setOf(
            REDMI_AIRDOTS_3_PRO, REDMI_AIRDOTS_3_PRO_GENSHIN
        )
        val REDMI_BUDS_3_SERIES = setOf(
            REDMI_BUDS_3, REDMI_BUDS_3_GLOBAL
        )
        val REDMI_BUDS_4_SERIES = setOf(
            REDMI_BUDS_4, REDMI_BUDS_4_GLOBAL,
            REDMI_BUDS_4_PRO, REDMI_BUDS_4_PRO_GLOBAL,
            REDMI_BUDS_4_HARRY_POTTER,
            REDMI_BUDS_4_ACTIVE, REDMI_BUDS_4_ACTIVE_GLOBAL
        )
        val REDMI_BUDS_5_SERIES = setOf(
            REDMI_BUDS_5, REDMI_BUDS_5_GLOBAL,
            REDMI_BUDS_5_AAPE,
            REDMI_BUDS_5_PRO, REDMI_BUDS_5_PRO_GLOBAL, REDMI_BUDS_5_PRO_GAMING
        )
        val REDMI_BUDS_6_SERIES = setOf(
            REDMI_BUDS_6, REDMI_BUDS_6_GLOBAL,
            REDMI_BUDS_6_ACTIVE, REDMI_BUDS_6_ACTIVE_GLOBAL,
            REDMI_BUDS_6_LITE, REDMI_BUDS_6_LITE_GLOBAL,
            REDMI_BUDS_6S,
            REDMI_BUDS_6_PLAY_GLOBAL,
            REDMI_BUDS_6_PRO, REDMI_BUDS_6_PRO_GLOBAL, REDMI_BUDS_6_PRO_GAMING
        )
        val REDMI_BUDS_7_SERIES = setOf(
            REDMI_BUDS_7S
        )
        val REDMI_BUDS_8_SERIES = setOf(
            REDMI_BUDS_8_ACTIVE, REDMI_BUDS_8_ACTIVE_GLOBAL,
            REDMI_BUDS_8_LITE, REDMI_BUDS_8_LITE_GLOBAL,
            REDMI_BUDS_8,
            REDMI_BUDS_8_PRO, REDMI_BUDS_8_PRO_GLOBAL
        )
        val REDMI_BUDS_SE_SERIES = setOf(
            REDMI_BUDS_SE
        )

        val XIAOMI_AIR_SERIES = setOf(
            XIAOMI_AIR4_SE
        )
        val XIAOMI_BUDS_3_SERIES = setOf(
            XIAOMI_BUDS_3, XIAOMI_BUDS_3_GLOBAL,
            XIAOMI_BUDS_3T_PRO_GLOBAL,
            XIAOMI_BUDS_3_PRO,
            XIAOMI_BUDS_3_STAR_WARS, XIAOMI_BUDS_3_STAR_WARS_GLOBAL,
            XIAOMI_BUDS_3_DISNEY
        )
        val XIAOMI_BUDS_4_SERIES = setOf(
            XIAOMI_BUDS_4, XIAOMI_BUDS_4_PRO, XIAOMI_BUDS_4_PRO_GLOBAL
        )
        val XIAOMI_BUDS_5_SERIES = setOf(
            XIAOMI_BUDS_5, XIAOMI_BUDS_5_GLOBAL,
            XIAOMI_BUDS_5_PRO, XIAOMI_BUDS_5_PRO_GLOBAL,
            XIAOMI_BUDS_5_PRO_WIFI, XIAOMI_BUDS_5_PRO_WIFI_GLOBAL
        )
        val XIAOMI_BUDS_6_SERIES = setOf(
            XIAOMI_BUDS_6
        )
        val XIAOMI_SPECIALTY_SERIES = setOf(
            XIAOMI_OPEN_WEAR, XIAOMI_OPEN_WEAR_GLOBAL,
            XIAOMI_OPEN_WEAR_PRO, XIAOMI_OPEN_WEAR_PRO_GLOBAL,
            XIAOMI_BONE_CONDUCTION_2
        )

        val REDMI_SERIES = REDMI_AIRDOTS_SERIES +
                REDMI_BUDS_3_SERIES +
                REDMI_BUDS_4_SERIES +
                REDMI_BUDS_5_SERIES +
                REDMI_BUDS_6_SERIES +
                REDMI_BUDS_7_SERIES +
                REDMI_BUDS_8_SERIES +
                REDMI_BUDS_SE_SERIES

        val XIAOMI_SERIES = XIAOMI_AIR_SERIES +
                XIAOMI_BUDS_3_SERIES +
                XIAOMI_BUDS_4_SERIES +
                XIAOMI_BUDS_5_SERIES +
                XIAOMI_BUDS_6_SERIES +
                XIAOMI_SPECIALTY_SERIES

        val MODELS = REDMI_SERIES + XIAOMI_SERIES
        private val ID_TO_MODEL_MAP = MODELS.associateBy { it.id }

        fun from(id: ProductID) = ID_TO_MODEL_MAP[id]
    }
}
