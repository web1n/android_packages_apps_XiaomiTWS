package org.lineageos.xiaomi_bluetooth.settings;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.xiaomi_bluetooth.EarbudsIconProvider;
import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EarbudsListFragment extends PreferenceFragmentCompat {

    private static final String TAG = EarbudsListFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int MMA_DEVICE_CHECK_TIMEOUT_MS = 2000;

    private record EarbudsInfo(int vendorId, int productId, Earbuds earbuds) {
        private EarbudsInfo(int vendorId, int productId, @NonNull Earbuds earbuds) {
            this.vendorId = vendorId;
            this.productId = productId;
            this.earbuds = earbuds;
        }
    }

    private ExecutorService earbudsExecutor;
    private ActivityResultLauncher<String[]> permissionRequestLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeExecutor();
        initializePermissionHandler();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.earbuds_list);
    }

    @Override
    public void onResume() {
        super.onResume();

        reloadDevices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        shutdownExecutor();
    }

    private void initializeExecutor() {
        earbudsExecutor = Executors.newSingleThreadExecutor();
    }

    private void shutdownExecutor() {
        if (earbudsExecutor != null && !earbudsExecutor.isShutdown()) {
            earbudsExecutor.shutdownNow();
        }
    }

    private void initializePermissionHandler() {
        permissionRequestLauncher = registerForActivityResult(new RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = !result.containsValue(false);

                    if (allGranted) {
                        reloadDevices();
                    } else {
                        openAppSettings();
                    }
                });
    }

    private boolean checkPermissions() {
        String[] missingPermissions = CommonUtils.getMissingRuntimePermissions(requireContext());
        if (missingPermissions.length == 0) return true;
        if (DEBUG) Log.d(TAG, "Missing permissions: " + Arrays.toString(missingPermissions));

        permissionRequestLauncher.launch(missingPermissions);
        return false;
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void reloadDevices() {
        if (!checkPermissions()) return;
        if (DEBUG) Log.d(TAG, "reloadDevices");

        @SuppressLint("MissingPermission")
        List<BluetoothDevice> devices = BluetoothUtils.getConnectedHeadsetA2DPDevices();
        devices.forEach(this::processDevices);
    }

    private void processDevices(@NonNull BluetoothDevice device) {
        addEarbudsPreference(device);

        executeBackgroundTask(() -> {
            try (MMADevice mma = new MMADevice(device)) {
                @SuppressLint("MissingPermission")
                EarbudsInfo info = CommonUtils.executeWithTimeout(() -> {
                    mma.connect();

                    Pair<Integer, Integer> vidPid = mma.getVidPid();
                    Earbuds battery = mma.getBatteryInfo();

                    if (vidPid != null && battery != null) {
                        return new EarbudsInfo(vidPid.first, vidPid.second, battery);
                    }
                    return null;
                }, MMA_DEVICE_CHECK_TIMEOUT_MS);

                updateUI(() -> updateEarbudsPreference(device, info));
            } catch (Exception e) {
                Log.e(TAG, "Error processing device: " + device.getName(), e);
                updateUI(() -> updateEarbudsPreference(device, null));
            }
        });
    }

    private void executeBackgroundTask(@NonNull Runnable task) {
        if (earbudsExecutor == null || earbudsExecutor.isShutdown()) {
            return;
        }

        earbudsExecutor.execute(task);
    }

    private void updateUI(@NonNull Runnable action) {
        requireActivity().runOnUiThread(() -> {
            if (getActivity() == null || getActivity().isFinishing()) {
                return;
            }

            action.run();
        });
    }

    @SuppressLint("MissingPermission")
    private void addEarbudsPreference(@NonNull BluetoothDevice device) {
        if (DEBUG) Log.d(TAG, "Adding preference for: " + device);

        Preference earbudsPreference = findPreference(device.getAddress());
        if (earbudsPreference == null) {
            earbudsPreference = new Preference(requireContext());
            earbudsPreference.setKey(device.getAddress());

            getPreferenceScreen().addPreference(earbudsPreference);
        }

        Intent infoIntent = new Intent(EarbudsInfoFragment.ACTION_EARBUDS_INFO);
        infoIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        infoIntent.setPackage(requireContext().getPackageName());

        earbudsPreference.setTitle(device.getName());
        earbudsPreference.setSummary(R.string.device_connecting);
        earbudsPreference.setIntent(infoIntent);
        earbudsPreference.setSelectable(false);
        earbudsPreference.setIconSpaceReserved(true);
    }

    private void updateEarbudsPreference(@NonNull BluetoothDevice device,
                                         @Nullable EarbudsInfo info) {
        if (DEBUG) Log.d(TAG, "Updating preference for device: " + device);

        Preference earbudsPreference = findPreference(device.getAddress());
        if (earbudsPreference == null) return;

        if (info != null) {
            earbudsPreference.setSelectable(true);
            earbudsPreference.setSummary(info.earbuds.toString());
            earbudsPreference.setIcon(getIconForModel(info.vendorId, info.productId));
        } else {
            earbudsPreference.setSummary(R.string.not_xiaomi_earbuds);
        }
    }

    @Nullable
    private Drawable getIconForModel(int vendorId, int productId) {
        ImageDecoder.Source source = ImageDecoder.createSource(
                requireContext().getContentResolver(),
                Uri.parse(EarbudsIconProvider.generateIconUri(
                        requireContext(), vendorId, productId, EarbudsIconProvider.TYPE_CASE))
        );

        try {
            return ImageDecoder.decodeDrawable(source);
        } catch (Exception e) {
            Log.e(TAG, "getIconForModel: ", e);
        }
        return null;
    }

}
