package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public abstract class ListController extends ConfigController {

    private static final String TAG = ListController.class.getSimpleName();
    private static final boolean DEBUG = true;

    private boolean isModesUpdated = false;

    public ListController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @NonNull
    protected abstract List<ConfigState> getConfigStates();

    @NonNull
    protected abstract ConfigState getDefaultState();

    @Override
    public void displayPreference(@NonNull Preference preference) {
        preference.setDefaultValue(CommonUtils.bytesToHex(getDefaultState().configValue));
        super.displayPreference(preference);
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        if (!(preference instanceof ListPreference listPreference)) {
            Log.w(TAG, "updateState: Incorrect preference type for " + preferenceKey);
            return;
        }

        updateSupportedModes(listPreference);
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
        if (getConfigValue() == null) return;
        if (preference instanceof ListPreference listPreference) {
            listPreference.setValue(CommonUtils.bytesToHex(getConfigValue()));
        }
    }

    private void updateSupportedModes(@NonNull ListPreference preference) {
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

        int resId = getConfigStates().stream()
                .filter(state -> Arrays.equals(state.configValue, getConfigValue()))
                .findFirst().orElse(getDefaultState())
                .summaryResId;
        return context.getString(resId);
    }

    @Override
    public boolean saveConfig(@NonNull MMADevice device, @NonNull Object value) throws IOException {
        if (value instanceof String strValue) {
            value = CommonUtils.hexToBytes(strValue);
        }

        return super.saveConfig(device, value);
    }

}
