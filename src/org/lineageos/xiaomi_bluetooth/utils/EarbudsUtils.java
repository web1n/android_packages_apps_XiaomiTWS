package org.lineageos.xiaomi_bluetooth.utils;

import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET;
import static android.bluetooth.BluetoothDevice.METADATA_DEVICE_TYPE;
import static android.bluetooth.BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI;
import static android.bluetooth.BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET;
import static android.bluetooth.BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY;
import static android.bluetooth.BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING;
import static android.bluetooth.BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY;
import static android.bluetooth.BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING;
import static android.bluetooth.BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY;
import static android.bluetooth.BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING;
import static com.android.settingslib.bluetooth.BluetoothUtils.META_INT_ERROR;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.util.Log;

import androidx.annotation.NonNull;

import org.lineageos.xiaomi_bluetooth.BleSliceProvider;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;


public class EarbudsUtils {

    private static final String TAG = EarbudsUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    @SuppressWarnings({"all"})
    public static ScanRecord parseFromBytes(byte[] bytes) {
        try {
            return (ScanRecord) ScanRecord.class
                    .getMethod("parseFromBytes", byte[].class)
                    .invoke(null, bytes);
        } catch (Exception e) {
            Log.e(TAG, "parseXiaomiATCommand: ", e);
            return null;
        }
    }

    private static void setEarbudsMetadata(@NonNull BluetoothDevice device) {
        if (!device.isConnected()) {
            if (DEBUG) Log.d(TAG, "device not connected " + device);
            return;
        }

        // set device type
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_DEVICE_TYPE, DEVICE_TYPE_UNTETHERED_HEADSET);
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_IS_UNTETHERED_HEADSET, true);
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_ENHANCED_SETTINGS_UI_URI,
                BleSliceProvider.generateSliceUri(device.getAddress()));
    }

    public static void updateEarbudsStatus(@NonNull Earbuds earbuds) {
        if (!earbuds.isValid()) return;
        if (DEBUG) Log.d(TAG, "updateEarbudsStatus " + earbuds);

        BluetoothDevice device = BluetoothUtils.getBluetoothDevice(earbuds.macAddress);
        if (!device.isConnected()) {
            if (DEBUG) Log.d(TAG, "device is null or not connected " + device);
            return;
        }
        setEarbudsMetadata(device);

        // left
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_UNTETHERED_LEFT_CHARGING,
                earbuds.left != null && earbuds.left.charging);
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_UNTETHERED_LEFT_BATTERY,
                earbuds.left != null ? earbuds.left.battery : META_INT_ERROR);
        // right
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_UNTETHERED_RIGHT_CHARGING,
                earbuds.right != null && earbuds.right.charging);
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_UNTETHERED_RIGHT_BATTERY,
                earbuds.right != null ? earbuds.right.battery : META_INT_ERROR);
        // case
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_UNTETHERED_CASE_CHARGING,
                earbuds.chargingCase != null && earbuds.chargingCase.charging);
        BluetoothUtils.updateDeviceMetadata(device,
                METADATA_UNTETHERED_CASE_BATTERY,
                earbuds.chargingCase != null ? earbuds.chargingCase.battery : META_INT_ERROR);
    }

}
