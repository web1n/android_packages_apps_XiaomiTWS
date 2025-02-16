package org.lineageos.xiaomi_bluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import org.lineageos.xiaomi_bluetooth.settings.EarbudsInfoFragment;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;

import java.util.Objects;


public class BleSliceProvider extends SliceProvider {

    private static final String TAG = BleSliceProvider.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String AUTHORITY_BLE_SLICE = "org.lineageos.xiaomi_bluetooth.ble-slice";
    private static final String KEY_MAC_ADDRESS = "mac";

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public Slice onBindSlice(@NonNull Uri sliceUri) {
        if (DEBUG) Log.d(TAG, "onBindSlice: " + sliceUri);
        BluetoothDevice device = getBluetoothDevice(sliceUri);

        return new ListBuilder(requireContext(), sliceUri, ListBuilder.INFINITY)
                .addRow(createSettingsActionBuilder(device))
                .build();
    }

    @NonNull
    private ListBuilder.RowBuilder createSettingsActionBuilder(@NonNull BluetoothDevice device) {
        Intent intent = new Intent(EarbudsInfoFragment.ACTION_EARBUDS_INFO);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.setPackage(requireContext().getPackageName());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
        SliceAction sliceAction = SliceAction.create(pendingIntent,
                IconCompat.createWithResource(requireContext(), R.drawable.ic_settings_24dp),
                ListBuilder.ICON_IMAGE, requireContext().getString(R.string.app_name));

        return new ListBuilder.RowBuilder()
                .setTitle(requireContext().getString(R.string.earbuds_settings))
                .setPrimaryAction(sliceAction);
    }

    @NonNull
    private static BluetoothDevice getBluetoothDevice(@NonNull Uri uri) {
        String macAddress = Objects.requireNonNull(uri.getQueryParameter(KEY_MAC_ADDRESS));

        return BluetoothUtils.getBluetoothDevice(macAddress);
    }

    @NonNull
    public static String generateSliceUri(@NonNull String macAddress) {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY_BLE_SLICE)
                .appendQueryParameter(KEY_MAC_ADDRESS, macAddress)
                .build();
        return uri.toString();
    }

}
