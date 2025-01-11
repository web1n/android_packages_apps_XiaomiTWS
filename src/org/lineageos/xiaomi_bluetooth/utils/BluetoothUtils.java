package org.lineageos.xiaomi_bluetooth.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;

import androidx.annotation.Nullable;


public class BluetoothUtils {

    public static String TAG = BluetoothUtils.class.getName();
    public static boolean DEBUG = true;

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

    @Nullable
    public static BluetoothDevice getBluetoothDevice(String mac) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (mac == null || adapter == null || !adapter.isEnabled()) {
            return null;
        }

        return adapter.getRemoteDevice(mac);
    }

    public static boolean isAnyDeviceConnected(@Nullable Context context) {
        BluetoothAdapter adapter = getBluetoothAdapter(context);
        if (adapter == null) {
            return false;
        }

        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (device.isConnected()) return true;
        }

        return false;
    }

    public static void setDeviceMetadata(
        BluetoothDevice device, int key, Object value, boolean update) {
        if (device == null || value == null) {
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
