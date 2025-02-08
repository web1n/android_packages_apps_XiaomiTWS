package org.lineageos.xiaomi_bluetooth.settings;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.settings.configs.ConfigController;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;
import org.lineageos.xiaomi_bluetooth.utils.PreferenceUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;


public class EarbudsFragment extends PreferenceFragmentCompat {

    private static final String TAG = EarbudsFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String ACTION_RELOAD_CONFIG =
            "org.lineageos.xiaomi_bluetooth.action.RELOAD_CONFIG";

    private static final int PREFERENCE_XML_RES_ID = R.xml.earbuds_settings;
    private static final int MMA_DEVICE_CHECK_TIMEOUT_MS = 2000;

    private final BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(ACTION_RELOAD_CONFIG)) {
                reloadConfig();
            } else {
                if (DEBUG) Log.w(TAG, "unknown action " + intent.getAction());
            }
        }
    };

    private final Set<ConfigController> configControllers = new HashSet<>();
    private ExecutorService earbudsExecutor;
    private BluetoothDevice device;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeArguments();
        initializeExecutor();
        registerActionReceiver();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(PREFERENCE_XML_RES_ID);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindPreferenceControllers();
        reloadConfig();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterActionReceiver();
        shutdownExecutor();
        unbindPreferenceControllers();
    }

    private void initializeArguments() {
        Bundle args = getArguments();
        if (args != null) {
            device = args.getParcelable(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        }
        if (DEBUG) Log.d(TAG, "Initialized with device: " + device);
    }

    private void initializeExecutor() {
        earbudsExecutor = Executors.newSingleThreadExecutor();
    }

    private void shutdownExecutor() {
        if (earbudsExecutor != null && !earbudsExecutor.isShutdown()) {
            earbudsExecutor.shutdownNow();
        }
    }

    private void registerActionReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RELOAD_CONFIG);

        requireContext().registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void unregisterActionReceiver() {
        requireContext().unregisterReceiver(actionReceiver);
    }

    private void reloadConfig() {
        if (DEBUG) Log.d(TAG, "reloadConfig");
        if (!checkConnected()) return;

        final int[] configIds = configControllers.stream()
                .mapToInt(ConfigController::getConfigId)
                .filter(id -> id != ConfigController.CONFIG_ID_INVALID)
                .distinct().toArray();
        if (DEBUG) Log.d(TAG, "reloadConfig: ids: " + Arrays.toString(configIds));

        executeBackgroundTask(() -> {
            try (MMADevice mma = new MMADevice(device)) {
                boolean success = CommonUtils.executeWithTimeout(() -> {
                    mma.connect();

                    Pair<Integer, Integer> vidPid = mma.getVidPid();
                    if (vidPid == null) {
                        return false;
                    }

                    for (int configId : configIds) {
                        byte[] value = mma.getDeviceConfig(configId, null);
                        processConfigValue(configId, value, vidPid.first, vidPid.second);
                    }

                    return true;
                }, MMA_DEVICE_CHECK_TIMEOUT_MS);

                if (success) {
                    updateUI(this::refreshPreferences);
                }
            } catch (IOException | TimeoutException e) {
                handleError("Config reload failed", e);
            }
        });
    }

    private void processConfigValue(int configId, @Nullable byte[] value, int vid, int pid) {
        if (DEBUG) Log.d(TAG, "processConfigValue: " + configId + " " + Arrays.toString(value));

        configControllers.stream()
                .filter(controller -> controller.getConfigId() == configId)
                .forEach(controller -> {
                    controller.setVendorData(vid, pid);
                    controller.setConfigValue(value);
                });
    }

    private void bindPreferenceControllers() {
        if (DEBUG) Log.d(TAG, "bindPreferenceControllers");

        Set<ConfigController> controllers =
                PreferenceUtils.createAllControllers(requireContext(), PREFERENCE_XML_RES_ID);
        controllers.forEach(this::bindControllerToPreference);
        configControllers.addAll(controllers);
    }

    private void bindControllerToPreference(@NonNull ConfigController controller) {
        Preference preference = findPreference(controller.preferenceKey);
        if (preference == null) {
            return;
        }

        controller.displayPreference(preference);
        setupPreferenceListener(preference);
    }

    private void unbindPreferenceControllers() {
        if (DEBUG) Log.d(TAG, "unbindPreferenceControllers");

        configControllers.forEach(controller -> {
            Preference preference = findPreference(controller.preferenceKey);
            if (preference == null) {
                return;
            }

            preference.setOnPreferenceChangeListener(null);
            preference.setOnPreferenceClickListener(null);
        });
        configControllers.clear();
    }

    private void setupPreferenceListener(@NonNull Preference preference) {
        preference.setOnPreferenceChangeListener((p, newValue) -> {
            ConfigController controller = findController(preference);
            if (controller == null) {
                return false;
            }

            if (controller instanceof Preference.OnPreferenceChangeListener listener) {
                boolean update = listener.onPreferenceChange(p, newValue);
                if (!update) return false;
            }

            if (checkConnected()) {
                executeBackgroundTask(() -> handlePreferenceChange(controller, p, newValue));
            }
            return false;
        });

        preference.setOnPreferenceClickListener(p -> {
            ConfigController controller = findController(p);
            if (controller == null) {
                return false;
            }

            if (controller instanceof Preference.OnPreferenceClickListener listener) {
                return listener.onPreferenceClick(p);
            }
            return false;
        });
    }

    private void handlePreferenceChange(@NonNull ConfigController controller,
                                        @NonNull Preference preference,
                                        @NonNull Object newValue) {
        try (MMADevice mma = new MMADevice(device)) {
            boolean success = CommonUtils.executeWithTimeout(() -> {
                mma.connect();
                return controller.saveConfig(mma, newValue);
            }, MMA_DEVICE_CHECK_TIMEOUT_MS);

            if (success) {
                updateUI(() -> controller.updateState(preference));
            }
        } catch (RuntimeException | IOException | TimeoutException e) {
            handleError("Save config failed", e);
        }
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

    private void refreshPreferences() {
        configControllers.forEach(controller -> {
            Preference preference = findPreference(controller.preferenceKey);
            if (preference != null) controller.updateState(preference);
        });
    }

    @Nullable
    private ConfigController findController(@NonNull Preference preference) {
        ConfigController controller = configControllers.stream()
                .filter(c -> c.preferenceKey.equals(preference.getKey()))
                .findFirst().orElse(null);

        if (controller == null) {
            Log.w(TAG, "Unknown controller for: " + preference.getKey());
        }
        return controller;
    }

    private boolean checkConnected() {
        if (!Objects.requireNonNull(device).isConnected()) {
            showToast(getString(R.string.device_not_connected));
            return false;
        }
        return true;
    }

    private void showToast(@NonNull String content) {
        if (DEBUG) Log.d(TAG, "showToast: " + content);

        updateUI(() -> Toast.makeText(getActivity(), content, Toast.LENGTH_SHORT).show());
    }

    private void handleError(@NonNull String message, @NonNull Exception e) {
        Log.e(TAG, message, e);
        showToast(message + ": " + e);
    }

}
