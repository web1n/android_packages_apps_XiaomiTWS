package org.lineageos.xiaomi_bluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.SliceAction;
import androidx.slice.builders.ListBuilder;

import com.android.settingslib.drawer.TileUtils;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.settings.EarbudsActivity;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;


public class BleSliceProvider extends SliceProvider {

    private static final String TAG = BleSliceProvider.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public Slice onBindSlice(@NonNull Uri sliceUri) {
        if (DEBUG) Log.d(TAG, "onBindSlice: " + sliceUri);
        BluetoothDevice device = getBluetoothDevice(sliceUri);
        if (getContext() == null || device == null) {
            return null;
        }

        ListBuilder listBuilder = new ListBuilder(getContext(), sliceUri, ListBuilder.INFINITY);

        ListBuilder.RowBuilder settingsActionBuilder = createSettingsActionBuilder(device);
        if (settingsActionBuilder != null) {
            listBuilder.addRow(settingsActionBuilder);
        }

        ListBuilder.RowBuilder firmwareVersionBuilder = createFirmwareVersionBuilder(device);
        if (firmwareVersionBuilder != null) {
            listBuilder.addRow(firmwareVersionBuilder);
        }

        return listBuilder.build();
    }

    @Nullable
    private ListBuilder.RowBuilder createSettingsActionBuilder(@NonNull BluetoothDevice device) {
        if (getContext() == null) {
            return null;
        }

        Intent intent = new Intent(getContext(), EarbudsActivity.class);
        intent.setAction(TileUtils.IA_SETTINGS_ACTION);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new ListBuilder.RowBuilder()
                .setTitle(getContext().getString(R.string.earbuds_settings))
                .setPrimaryAction(createSliceAction(pendingIntent, R.drawable.ic_settings_24dp));
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

    @NonNull
    private SliceAction createSliceAction(@NonNull PendingIntent pendingIntent, int iconResId) {
        return SliceAction.create(pendingIntent,
                IconCompat.createWithResource(requireContext(), iconResId),
                ListBuilder.ICON_IMAGE, requireContext().getString(R.string.app_name));
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
