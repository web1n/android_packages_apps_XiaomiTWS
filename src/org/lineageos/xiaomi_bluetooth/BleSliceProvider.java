package org.lineageos.xiaomi_bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;


public class BleSliceProvider extends SliceProvider {

    private static final String TAG = BleSliceProvider.class.getName();
    private static final boolean DEBUG = true;

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        if (DEBUG) Log.d(TAG, "onBindSlice: " + sliceUri);
        BluetoothDevice device = getBluetoothDevice(sliceUri);
        if (getContext() == null || device == null) {
            return null;
        }

        ListBuilder listBuilder = new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY);

        ListBuilder.RowBuilder firmwareVersionBuilder = createFirmwareVersionBuilder(device);
        if (firmwareVersionBuilder != null) {
            listBuilder.addRow(firmwareVersionBuilder);
        }

        return listBuilder.build();
    }

    @Nullable
    private ListBuilder.RowBuilder createFirmwareVersionBuilder(@NonNull BluetoothDevice device) {
        if (getContext() == null) {
            return null;
        }

        byte[] firmwareVersionBytes = device.getMetadata(BluetoothDevice.METADATA_SOFTWARE_VERSION);
        if (firmwareVersionBytes == null) {
            return null;
        }
        String firmwareVersion = new String(firmwareVersionBytes);

        return new ListBuilder.RowBuilder()
                .setTitle(getContext().getString(R.string.firmware_version))
                .setSubtitle(firmwareVersion);
    }

    @Nullable
    private static BluetoothDevice getBluetoothDevice(@NonNull Uri uri) {
        try {
            return BluetoothUtils.getBluetoothDevice(uri.getQueryParameter("mac"));
        } catch (Exception e) {
            Log.e(TAG, "getBluetoothDevice: ", e);
            return null;
        }
    }

    @NonNull
    public static String generateSliceUri(@NonNull String macAddress) {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("org.lineageos.xiaomi_bluetooth.ble-slice")
                .appendQueryParameter("mac", macAddress)
                .build();
        return uri.toString();
    }

}
