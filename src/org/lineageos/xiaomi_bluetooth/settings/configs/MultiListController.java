package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class MultiListController extends ConfigController {

    private static final String TAG = MultiListController.class.getSimpleName();
    private static final boolean DEBUG = true;

    private boolean isModesUpdated = false;

    public MultiListController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @NonNull
    protected abstract List<ConfigState> getConfigStates();

    @NonNull
    protected abstract List<ConfigState> getCheckedStates();

    @Override
    public void updateState(@NonNull Preference preference) {
        updateSupportedModes((MultiSelectListPreference) preference);

        super.updateState(preference);
    }

    @Override
    public Available isAvailable() {
        return switch (super.isAvailable()) {
            case AVAILABLE -> !getConfigStates().isEmpty()
                    ? Available.AVAILABLE
                    : Available.UNAVAILABLE;
            case UNAVAILABLE -> Available.UNAVAILABLE;
            case UNKNOWN -> Available.UNKNOWN;
        };
    }

    @Override
    protected void updateValue(@NonNull Preference preference) {
        if (DEBUG) Log.d(TAG, "updateValue: " + preferenceKey);

        Set<String> values = getCheckedStates().stream()
                .map(state -> CommonUtils.bytesToHex(state.configValue))
                .collect(Collectors.toSet());
        ((MultiSelectListPreference) preference).setValues(values);
    }

    private void updateSupportedModes(@NonNull MultiSelectListPreference preference) {
        if (isAvailable() != Available.AVAILABLE || isModesUpdated) {
            return;
        }
        isModesUpdated = true;

        List<ConfigState> configStates = getConfigStates();
        String[] entryValues = configStates.stream()
                .map(state -> CommonUtils.bytesToHex(state.configValue))
                .toArray(String[]::new);
        String[] entries = configStates.stream()
                .map(state -> context.getString(state.summaryResId))
                .toArray(String[]::new);

        if (DEBUG) {
            Log.d(TAG, String.format("updateSupportedModes: entryValues=%s, entries=%s",
                    Arrays.toString(entryValues), Arrays.toString(entries)));
        }

        preference.setEntryValues(entryValues);
        preference.setEntries(entries);
    }

    @Override
    public String getSummary() {
        if (getConfigValue() == null) {
            return null;
        }

        return getCheckedStates().stream()
                .map(state -> context.getString(state.summaryResId))
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean saveConfig(@NonNull MMADevice device, @NonNull Object value) throws IOException {
        if (value instanceof String strValue) {
            value = CommonUtils.hexToBytes(strValue);
        }

        return super.saveConfig(device, value);
    }

}
