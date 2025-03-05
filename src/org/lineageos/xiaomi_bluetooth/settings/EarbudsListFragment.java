package org.lineageos.xiaomi_bluetooth.settings;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;
import org.lineageos.xiaomi_bluetooth.utils.PermissionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EarbudsListFragment extends PreferenceFragmentCompat {

    private static final String TAG = EarbudsListFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int MMA_DEVICE_CHECK_TIMEOUT_MS = 2000;

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
        String[] missingPermissions = PermissionUtils.getMissingRuntimePermissions(requireContext());
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
                Earbuds info = CommonUtils.executeWithTimeout(() -> {
                    mma.connect();

                    return mma.getBatteryInfo();
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
        earbudsPreference.setIconSpaceReserved(false);
    }

    private void updateEarbudsPreference(@NonNull BluetoothDevice device,
                                         @Nullable Earbuds earbuds) {
        if (DEBUG) Log.d(TAG, "Updating preference for device: " + device);

        Preference earbudsPreference = findPreference(device.getAddress());
        if (earbudsPreference == null) return;

        if (earbuds != null) {
            earbudsPreference.setSelectable(true);
            earbudsPreference.setSummary(earbuds.toString());
        } else {
            earbudsPreference.setSummary(R.string.not_xiaomi_earbuds);
        }
    }

}
