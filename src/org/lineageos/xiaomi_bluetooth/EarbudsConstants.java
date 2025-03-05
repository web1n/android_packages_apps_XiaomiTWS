package org.lineageos.xiaomi_bluetooth;

import android.os.ParcelUuid;


public class EarbudsConstants {

    public static final ParcelUuid UUID_XIAOMI_FAST_CONNECT
            = ParcelUuid.fromString("0000FD2D-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid UUID_XIAOMI_XIAOAI =
            ParcelUuid.fromString("00001101-0000-1000-8000-008584d01810");
    public static final ParcelUuid UUID_SPP =
            ParcelUuid.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static final ParcelUuid[] XIAOMI_UUIDS = new ParcelUuid[]{
            UUID_XIAOMI_FAST_CONNECT, UUID_XIAOMI_XIAOAI, UUID_SPP
    };

    public static final int XIAOMI_MMA_DATA_LENGTH = 22;


    public static final byte XIAOMI_MMA_RESPONSE_STATUS_OK = 0x00;

    public static final byte XIAOMI_MMA_OPCODE_GET_DEVICE_INFO = 0x02;
    public static final byte XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG = (byte) 0xF2;
    public static final byte XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG = (byte) 0xF3;

    public static final int XIAOMI_MMA_MASK_GET_VERSION = 1;
    public static final int XIAOMI_MMA_MASK_GET_VID_PID = 3;
    public static final int XIAOMI_MMA_MASK_GET_UBOOT_VERSION = 6;
    public static final int XIAOMI_MMA_MASK_GET_BATTERY = 7;

    public static final int XIAOMI_MMA_CONFIG_BUTTON_MODE = 0x0002;
    public static final int XIAOMI_MMA_CONFIG_EQUALIZER_MODE = 0x0007;
    public static final int XIAOMI_MMA_CONFIG_FIND_EARBUDS = 0x0009;
    public static final int XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_LIST = 0x000A;
    public static final int XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE = 0x000B;
    public static final int XIAOMI_MMA_CONFIG_SN = 0x0027;

    public static final byte XIAOMI_MMA_CONFIG_EQUALIZER_MODE_DEFAULT = 0x00;
    public static final byte XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOCAL_ENHANCE = 0x01;
    public static final byte XIAOMI_MMA_CONFIG_EQUALIZER_MODE_BASS_BOOST = 0x05;
    public static final byte XIAOMI_MMA_CONFIG_EQUALIZER_MODE_TREBLE_BOOST = 0x06;
    public static final byte XIAOMI_MMA_CONFIG_EQUALIZER_MODE_VOLUME_BOOST = 0x07;
    public static final byte XIAOMI_MMA_CONFIG_EQUALIZER_MODE_HARMAN = 0x14;

    public static final byte XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF = 0x00;
    public static final byte XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_ON = 0x01;
    public static final byte XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY = 0x02;

}
