package org.lineageos.xiaomi_bluetooth.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class BluetoothUtils {

    private static final String TAG = BluetoothUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Nullable
    public static BluetoothAdapter getBluetoothAdapter(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        return context.getSystemService(BluetoothManager.class).getAdapter();
    }

    @Nullable
    public static BluetoothLeScanner getScanner(@Nullable Context context) {
        BluetoothAdapter adapter = getBluetoothAdapter(context);
        if (adapter == null) {
            return null;
        }

        return adapter.getBluetoothLeScanner();
    }

    @NonNull
    public static List<BluetoothDevice> getConnectedHeadsetA2DPDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return Collections.emptyList();
        }

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices == null) {
            return Collections.emptyList();
        }

        return bondedDevices.stream().filter(BluetoothUtils::isConnectedHeadsetA2DPDevice).toList();
    }

    public static boolean isConnectedHeadsetA2DPDevice(@Nullable BluetoothDevice device) {
        if (device == null || !device.isConnected()) {
            return false;
        }

        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            return false;
        }

        return bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_A2DP) &&
                bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET);
    }

    @Nullable
    public static BluetoothDevice getBluetoothDevice(String mac) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (mac == null || adapter == null || !adapter.isEnabled()) {
            return null;
        }

        return adapter.getRemoteDevice(mac);
    }

    public static void setDeviceMetadata(
            BluetoothDevice device, int key, Object value, boolean update) {
        if (device == null || value == null) {
            return;
        }
        if (value.toString().length() > BluetoothDevice.METADATA_MAX_LENGTH) {
            return;
        }
        if (!update && device.getMetadata(key) != null) {
            return;
        }

        device.setMetadata(key, value.toString().getBytes());
    }

    public static void updateDeviceMetadata(
            BluetoothDevice device, int key, Object value) {
        setDeviceMetadata(device, key, value, true);
    }

}
