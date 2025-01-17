package org.lineageos.xiaomi_bluetooth.settings;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;


public class EarbudsFragment extends PreferenceFragment {

    private static final String TAG = EarbudsFragment.class.getName();
    private static final boolean DEBUG = true;

    private static final String KEY_EQUALIZER_MODE = "equalizer_mode";
    private static final String KEY_SERIAL_NUMBER = "serial_number";

    private static final Map<Integer, String> CONFIG_KEY_MAP = new HashMap<>();
    private final Map<Integer, String> CONFIG_VALUE_MAP = new HashMap<>();

    private ExecutorService earbudsExecutor;
    private BluetoothDevice device;

    static {
        CONFIG_KEY_MAP.put(EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE, KEY_EQUALIZER_MODE);
        CONFIG_KEY_MAP.put(EarbudsConstants.XIAOMI_MMA_CONFIG_SN, KEY_SERIAL_NUMBER);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            device = args.getParcelable(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        }
        if (DEBUG) Log.d(TAG, "onCreate: device: " + device);

        earbudsExecutor = Executors.newSingleThreadExecutor();
        reloadConfig();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.earbuds_settings);

        ListPreference equalizerPreference = (ListPreference) findPreference(KEY_EQUALIZER_MODE);
        bindLayout(equalizerPreference);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (earbudsExecutor != null) {
            earbudsExecutor.shutdownNow();
        }
    }

    private void reloadConfig() {
        if (DEBUG) Log.d(TAG, "reloadConfig");
        if (!checkConnected()) return;

        earbudsExecutor.execute(() -> {
            Map<Integer, byte[]> configs = null;
            try (MMADevice mma = new MMADevice(device)) {
                configs = CommonUtils.executeWithTimeout(() -> {
                    mma.connect();
                    return mma.getDeviceConfig(new int[]{
                            EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE,
                            EarbudsConstants.XIAOMI_MMA_CONFIG_SN
                    });
                }, 300);
            } catch (RuntimeException | TimeoutException | IOException e) {
                showToast(e.toString());
                Log.e(TAG, "reloadConfig: ", e);
            }

            if (configs != null) {
                configs.forEach((key, value) ->
                        CONFIG_VALUE_MAP.put(key, CommonUtils.bytesToHex(value)));
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updatePreferenceValue);
            }
        });
    }

    private void updatePreferenceValue() {
        if (DEBUG) Log.d(TAG, "updatePreferenceValue");
        if (getContext() == null || !isResumed()) {
            return;
        }

        CONFIG_VALUE_MAP.forEach((key, value) -> {
            String preferenceKey = CONFIG_KEY_MAP.getOrDefault(key, null);
            if (DEBUG) Log.d(TAG, "updatePreferenceValue: " + preferenceKey + " " + value);
            if (preferenceKey == null) {
                return;
            }
            Preference preference = findPreference(preferenceKey);
            if (preference == null) {
                return;
            }

            // FF is not supported
            preference.setEnabled(!"FF".equals(value));

            if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue(value);
            } else {
                preference.setSummary(valueToSummary(preferenceKey, value));
            }
        });
    }

    private void bindLayout(@NonNull ListPreference preference) {
        if (DEBUG) Log.d(TAG, "bindLayout: " + preference.getKey());

        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            final Integer configKey = preferenceKeyToConfigKey(preference1.getKey());
            if (DEBUG) Log.d(TAG, "onPreferenceChanged: " + preference1.getKey() + " " + configKey);
            if (configKey == null || !checkConnected()) {
                return false;
            }

            earbudsExecutor.execute(() -> {
                boolean saved = false;
                try (MMADevice mma = new MMADevice(device)) {
                    byte[] value = CommonUtils.hexToBytes((String) newValue);

                    saved = CommonUtils.executeWithTimeout(() -> {
                        mma.connect();
                        return mma.setDeviceConfig(configKey, value);
                    }, 300);
                } catch (RuntimeException | TimeoutException | IOException e) {
                    showToast(e.toString());
                    Log.e(TAG, "onPreferenceChanged: ", e);
                }

                if (saved) {
                    CONFIG_VALUE_MAP.put(configKey, (String) newValue);
                }
                getActivity().runOnUiThread(this::updatePreferenceValue);
            });

            return false;
        });
    }

    @NonNull
    private String valueToSummary(@NonNull String preferenceKey, @NonNull String value) {
        String summary = value;

        if (KEY_SERIAL_NUMBER.equals(preferenceKey)) {
            try {
                summary = new String(CommonUtils.hexToBytes(value));
            } catch (Exception ignored) {
            }
        }

        return summary;
    }

    private boolean checkConnected() {
        if (device == null || !device.isConnected()) {
            showToast(getString(R.string.device_not_connected));
            return false;
        }
        return true;
    }

    private void showToast(@Nullable String content) {
        if (DEBUG) Log.d(TAG, "showToast: " + content);
        if (content == null || getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (getActivity() == null || !isResumed()) {
                return;
            }

            Toast.makeText(getActivity(), content, Toast.LENGTH_SHORT).show();
        });
    }

    @Nullable
    private Integer preferenceKeyToConfigKey(@Nullable String key) {
        if (key == null) {
            return null;
        }

        for (Map.Entry<Integer, String> kv : CONFIG_KEY_MAP.entrySet()) {
            if (key.equals(kv.getValue())) {
                return kv.getKey();
            }
        }

        return null;
    }

}
