package org.lineageos.xiaomi_bluetooth.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.BleSliceProvider;
import org.lineageos.xiaomi_bluetooth.EarbudsIconProvider;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;

import static android.bluetooth.BluetoothDevice.*;
import static org.lineageos.xiaomi_bluetooth.EarbudsConstants.*;

import static com.android.settingslib.bluetooth.BluetoothUtils.META_INT_ERROR;

import java.util.ArrayList;
import java.util.List;


public class EarbudsUtils {

    private static final String TAG = EarbudsUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Nullable
    public static Earbuds parseXiaomiATCommand(@NonNull BluetoothDevice device, @NonNull String arg0) {
        if (arg0.length() < 16 || !arg0.startsWith("FF010201") || !arg0.endsWith("FF")) {
            return null;
        }
        byte[] scanRecordBytes = CommonUtils.hexToBytes(arg0.substring(14, arg0.length() - 2));

        ScanRecord record;
        try {
            record = (ScanRecord) ScanRecord.class
                    .getMethod("parseFromBytes", byte[].class)
                    .invoke(null, scanRecordBytes);
        } catch (Exception e) {
            Log.e(TAG, "parseXiaomiATCommand: ", e);
            return null;
        }

        Log.e("TAG", "parseXiaomiATCommand record: " + record);
        if (record == null) {
            return null;
        }

        byte[] fastConnectData = record.getServiceData(UUID_XIAOMI_FAST_CONNECT);
        if (fastConnectData == null || fastConnectData.length < XIAOMI_MMA_DATA_LENGTH) {
            return null;
        }

        return Earbuds.fromBytes(device.getAddress(),
                fastConnectData[13], fastConnectData[12], fastConnectData[14]);
    }

    @Nullable
    public static Earbuds parseScanResult(@Nullable ScanResult result) {
        if (result == null || result.getScanRecord() == null) {
            return null;
        }

        byte[] manufacturerData = result.getScanRecord()
                .getManufacturerSpecificData(MANUFACTURER_ID_XIAOMI);
        byte[] fastConnectData = result.getScanRecord()
                .getServiceData(UUID_XIAOMI_FAST_CONNECT);
        if (manufacturerData == null || manufacturerData.length < XIAOMI_MMA_DATA_LENGTH) {
            return null;
        }
        if (fastConnectData == null || fastConnectData.length < XIAOMI_MMA_DATA_LENGTH) {
            return null;
        }

        String macAddress = parseMacAddressFromManufacturerData(manufacturerData);
        if (macAddress == null) {
            return null;
        }

        return Earbuds.fromBytes(macAddress,
                fastConnectData[13], fastConnectData[12], fastConnectData[14]);
    }

    @Nullable
    private static String parseMacAddressFromManufacturerData(@NonNull byte[] obj) {
        boolean macAddressEncrypted = (obj[7] & 1) != 0;
        int offset = !macAddressEncrypted ? 11 : 18;

        byte[] macBytes = new byte[]{
                obj[1 + offset], obj[offset], obj[2 + offset],
                obj[5 + offset], obj[4 + offset], obj[3 + offset]
        };

        List<String> hexList = new ArrayList<>();
        for (byte b : macBytes) {
            hexList.add(String.format("%02X", b));
        }
        return String.join(":", hexList);
    }

    private static void setEarbudsMetadata(@NonNull BluetoothDevice device) {
        if (!device.isConnected()) {
            if (DEBUG) Log.d(TAG, "device not connected " + device);
            return;
        }

        // set device type
        BluetoothUtils.setDeviceMetadata(device,
                METADATA_DEVICE_TYPE, DEVICE_TYPE_UNTETHERED_HEADSET, false);
        BluetoothUtils.setDeviceMetadata(device,
                METADATA_IS_UNTETHERED_HEADSET, true, false);
        BluetoothUtils.setDeviceMetadata(device,
                METADATA_ENHANCED_SETTINGS_UI_URI,
                BleSliceProvider.generateSliceUri(device.getAddress()), false);
    }

    public static void setEarbudsModelData(@NonNull Context context,
                                           @NonNull BluetoothDevice device,
                                           int vendorId, int productId,
                                           @NonNull String softwareVersion) {
        if (!device.isConnected()) {
            if (DEBUG) Log.d(TAG, "device not connected " + device);
            return;
        }
        setEarbudsMetadata(device);

        // set software version
        BluetoothUtils.updateDeviceMetadata(device,
                BluetoothDevice.METADATA_SOFTWARE_VERSION, softwareVersion);

        // set icon
        // left
        String leftIcon = EarbudsIconProvider.generateIconUri(
                context, vendorId, productId, EarbudsIconProvider.TYPE_LEFT);
        BluetoothUtils.setDeviceMetadata(device,
                BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON, leftIcon, false);
        // right
        String rightIcon = EarbudsIconProvider.generateIconUri(
                context, vendorId, productId, EarbudsIconProvider.TYPE_RIGHT);
        BluetoothUtils.setDeviceMetadata(device,
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON, rightIcon, false);
        // case
        String caseIcon = EarbudsIconProvider.generateIconUri(
                context, vendorId, productId, EarbudsIconProvider.TYPE_CASE);
        BluetoothUtils.setDeviceMetadata(device,
                BluetoothDevice.METADATA_UNTETHERED_CASE_ICON, caseIcon, false);
        BluetoothUtils.setDeviceMetadata(device,
                BluetoothDevice.METADATA_MAIN_ICON, caseIcon, false);
    }

    public static void updateEarbudsStatus(@NonNull Earbuds earbuds) {
        if (!earbuds.isValid()) return;
        if (DEBUG) Log.d(TAG, "updateEarbudsStatus " + earbuds);

        BluetoothDevice device = BluetoothUtils.getBluetoothDevice(earbuds.macAddress);
        if (device == null || !device.isConnected()) {
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
