package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import org.lineageos.xiaomi_bluetooth.mma.MMADevice;

import java.io.IOException;
import java.util.Arrays;


public abstract class SwitchController extends ConfigController {

    private static final String TAG = SwitchController.class.getSimpleName();
    private static final boolean DEBUG = true;

    public SwitchController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @NonNull
    protected abstract ConfigState getEnabledState();

    @NonNull
    protected abstract ConfigState getDisabledState();

    protected boolean isEnabled() {
        return Arrays.equals(getEnabledState().configValue, getConfigValue());
    }

    @Override
    public void displayPreference(@NonNull Preference preference) {
        super.displayPreference(preference);

        if (!(preference instanceof TwoStatePreference switchPreference)) {
            Log.w(TAG, "displayPreference: Incorrect preference type for " + preferenceKey);
            return;
        }

        switchPreference.setSummaryOn(getEnabledState().summaryResId);
        switchPreference.setSummaryOff(getDisabledState().summaryResId);
    }

    @Override
    public String getSummary() {
        if (getConfigValue() == null) {
            return null;
        }

        int resId = isEnabled()
                ? getEnabledState().summaryResId
                : getDisabledState().summaryResId;
        if (DEBUG) Log.d(TAG, "getSummary: resId: " + resId);
        return context.getString(resId);
    }

    @Override
    public boolean saveConfig(@NonNull MMADevice device, @NonNull Object value) throws IOException {
        if (value instanceof Boolean enable) {
            value = enable
                    ? getEnabledState().configValue
                    : getDisabledState().configValue;
        }

        return super.saveConfig(device, value);
    }

    @Override
    protected void updateValue(@NonNull Preference preference) {
        if (preference instanceof TwoStatePreference switchPreference) {
            switchPreference.setChecked(isEnabled());
        }
    }

}
