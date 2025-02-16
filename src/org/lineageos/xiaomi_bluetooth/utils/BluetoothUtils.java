package org.lineageos.xiaomi_bluetooth.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class BluetoothUtils {

    private static final String TAG = BluetoothUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    @NonNull
    public static BluetoothAdapter getBluetoothAdapter(@NonNull Context context) {
        return Objects.requireNonNull(context.getSystemService(BluetoothManager.class))
                .getAdapter();
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

    public static boolean isConnectedHeadsetA2DPDevice(@NonNull BluetoothDevice device) {
        if (!device.isConnected()) {
            return false;
        }

        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            return false;
        }

        return bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_A2DP) &&
                bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET);
    }

    @NonNull
    public static BluetoothDevice getBluetoothDevice(@NonNull String mac) {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
    }

    public static void updateDeviceMetadata(@NonNull BluetoothDevice device, int key,
                                            @NonNull Object value) {
        String valueStr = value.toString();
        if (valueStr.length() > BluetoothDevice.METADATA_MAX_LENGTH) {
            return;
        }

        device.setMetadata(key, valueStr.getBytes());
    }

}
