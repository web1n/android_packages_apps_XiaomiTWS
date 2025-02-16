package org.lineageos.xiaomi_bluetooth.utils;

import static org.lineageos.xiaomi_bluetooth.EarbudsConstants.UUID_XIAOMI_FAST_CONNECT;
import static org.lineageos.xiaomi_bluetooth.EarbudsConstants.XIAOMI_MMA_DATA_LENGTH;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.le.ScanRecord;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;


public class ATUtils {

    private static final String TAG = ATUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final int MANUFACTURER_ID_XIAOMI = 0x038F;
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI = "+XIAOMI";

    private static final String COMMAND_START = "FF010201";
    private static final String COMMAND_END = "FF";

    public static void sendATCommand(@NonNull BluetoothHeadset bluetoothHeadset,
                                     @NonNull BluetoothDevice device,
                                     int type, @NonNull String value) {
        if (!device.isConnected()) {
            return;
        }

        String data = String.format("%s%02x%s%s", COMMAND_START, type, value, COMMAND_END);

        boolean result = bluetoothHeadset.sendVendorSpecificResultCode(
                device, VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI, data);
        if (DEBUG) {
            Log.d(TAG, "sendATCommand: type=" + type + ", value=" + value + ", result=" + result);
        }
    }

    public static void sendUpdateATCommand(@NonNull BluetoothHeadset bluetoothHeadset,
                                           @NonNull BluetoothDevice device) {
        sendATCommand(bluetoothHeadset, device, 0x02, "0101");
    }

    private static boolean isValidATFastConnectCommand(@NonNull String arg0) {
        return arg0.length() >= 16 && arg0.startsWith(COMMAND_START) && arg0.endsWith(COMMAND_END);
    }

    @Nullable
    public static Earbuds parseATFastConnectCommand(@NonNull BluetoothDevice device,
                                                    @NonNull String arg0) {
        if (!isValidATFastConnectCommand(arg0)) {
            Log.w(TAG, "Invalid AT Fast Connect command: " + arg0);
            return null;
        }
        byte[] scanRecordBytes = CommonUtils.hexToBytes(arg0.substring(14, arg0.length() - 2));

        ScanRecord record = EarbudsUtils.parseFromBytes(scanRecordBytes);
        if (record == null) {
            Log.w(TAG, "Failed to parse ScanRecord from AT command");
            return null;
        }

        byte[] fastConnectData = record.getServiceData(UUID_XIAOMI_FAST_CONNECT);
        if (fastConnectData == null || fastConnectData.length < XIAOMI_MMA_DATA_LENGTH) {
            Log.w(TAG, "Invalid fast connect data");
            return null;
        }

        return Earbuds.fromBytes(device.getAddress(),
                fastConnectData[13], fastConnectData[12], fastConnectData[14]);
    }

}
