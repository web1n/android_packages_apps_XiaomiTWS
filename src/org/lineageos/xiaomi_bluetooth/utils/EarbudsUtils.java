package org.lineageos.xiaomi_bluetooth.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbud;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;

import static android.bluetooth.BluetoothDevice.*;
import static org.lineageos.xiaomi_bluetooth.EarbudsConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class EarbudsUtils {

    public static String TAG = EarbudsUtils.class.getName();
    public static boolean DEBUG = true;

    @Nullable
    public static Earbuds parseScanResult(ScanResult result) {
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
        return Earbuds.fromBytes(macAddress,
                fastConnectData[13], fastConnectData[12], fastConnectData[14]);
    }

    @Nullable
    private static String parseMacAddressFromManufacturerData(byte[] obj) {
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

    public static void setBluetoothDeviceType(BluetoothDevice device) {
        BluetoothUtils.setDeviceMetadata(device,
                METADATA_DEVICE_TYPE,
                DEVICE_TYPE_UNTETHERED_HEADSET, false);
        BluetoothUtils.setDeviceMetadata(device,
                METADATA_IS_UNTETHERED_HEADSET, true, false);
    }

    public static void updateEarbudsStatus(BluetoothDevice device, Earbuds earbuds) {
        if (earbuds.left != null) {
            BluetoothUtils.updateDeviceMetadata(device,
                    METADATA_UNTETHERED_LEFT_CHARGING, earbuds.left.charging);
            BluetoothUtils.updateDeviceMetadata(device,
                    METADATA_UNTETHERED_LEFT_BATTERY, earbuds.left.battery);
        }
        if (earbuds.right != null) {
            BluetoothUtils.updateDeviceMetadata(device,
                    METADATA_UNTETHERED_RIGHT_CHARGING, earbuds.right.charging);
            BluetoothUtils.updateDeviceMetadata(device,
                    METADATA_UNTETHERED_RIGHT_BATTERY, earbuds.right.battery);
        }
        if (earbuds.chargingCase != null) {
            BluetoothUtils.updateDeviceMetadata(device,
                    METADATA_UNTETHERED_CASE_CHARGING, earbuds.chargingCase.charging);
            BluetoothUtils.updateDeviceMetadata(device,
                    METADATA_UNTETHERED_CASE_BATTERY, earbuds.chargingCase.battery);
        }
    }

    public static boolean isXiaomiMMADevice(BluetoothDevice device) {
        if (device == null || !device.isConnected()) {
            return false;
        }

        BluetoothSocket socket = null;
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(
                    EarbudsConstants.UUID_XIAOMI_FAST_CONNECT.getUuid());
            socket.connect();

            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
