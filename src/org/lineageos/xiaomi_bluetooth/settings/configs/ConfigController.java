package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.IOException;
import java.util.Arrays;


public abstract class ConfigController {

    private static final String TAG = ConfigController.class.getName();
    private static final boolean DEBUG = true;

    public static final int CONFIG_ID_INVALID = 0x01;
    protected static final byte VALUE_FEATURE_NOT_SUPPORTED = -1;


    protected static class ConfigState {
        @NonNull
        final byte[] configValue;
        @StringRes
        final int summaryResId;

        ConfigState(@NonNull byte[] configValue, @StringRes int summaryResId) {
            this.configValue = Arrays.copyOf(configValue, configValue.length);
            this.summaryResId = summaryResId;
        }
    }

    @NonNull
    public final String preferenceKey;

    protected final Context context;
    private Integer vid, pid;
    private byte[] configValue;

    public ConfigController(@NonNull Context context, @NonNull String preferenceKey) {
        this.context = context;
        this.preferenceKey = preferenceKey;
    }

    public abstract int getConfigId();

    public abstract int getExpectedConfigLength();

    public final void setVendorData(int vid, int pid) {
        this.vid = vid;
        this.pid = pid;
    }

    public Integer getVid() {
        return vid;
    }

    public Integer getPid() {
        return pid;
    }

    public final void setConfigValue(@NonNull byte[] configValue) {
        if (!isValidValue(configValue)) {
            configValue = new byte[]{VALUE_FEATURE_NOT_SUPPORTED};
        }

        this.configValue = Arrays.copyOf(configValue, configValue.length);
    }

    @Nullable
    public final byte[] getConfigValue() {
        return configValue;
    }

    public boolean isValidValue(@Nullable byte[] value) {
        return value != null && value.length == getExpectedConfigLength();
    }

    public boolean isAvailable() {
        return isValidValue(configValue) && !isNotSupported();
    }

    protected final boolean isNotSupported() {
        return configValue != null
                && configValue.length == 1
                && configValue[0] == VALUE_FEATURE_NOT_SUPPORTED;
    }

    public void displayPreference(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }

        preference.setPersistent(false);
        updateState(preference);
    }

    public void updateState(@NonNull Preference preference) {
        if (preference == null) {
            return;
        }

        updateValue(preference);
        updateSummary(preference);
        updateVisible(preference);
    }

    @Nullable
    public String getSummary() {
        return null;
    }

    protected void updateValue(@NonNull Preference preference) {
    }

    private void updateSummary(@NonNull Preference preference) {
        String summary = isAvailable()
                ? getSummary()
                : context.getString(R.string.feature_not_supported);

        if (summary != null) {
            preference.setSummary(summary);
        }
    }

    private void updateVisible(@NonNull Preference preference) {
        preference.setSelectable(isAvailable());
    }

    public boolean saveConfig(@NonNull MMADevice device, @NonNull Object value) throws IOException {
        if (!(value instanceof byte[] byteValue)) {
            Log.w(TAG, "Invalid config value type: " + value.getClass().getSimpleName());
            return false;
        }

        if (Arrays.equals(byteValue, configValue)) {
            if (DEBUG) Log.d(TAG, "saveConfig: config not change");
            return true;
        }

        boolean result = device.setDeviceConfig(getConfigId(), byteValue);
        if (DEBUG) {
            Log.d(TAG, String.format("Config save %s for %s: %s",
                    result ? "successful" : "failed",
                    getClass().getSimpleName(),
                    CommonUtils.bytesToHex(byteValue)));
        }

        if (result) {
            setConfigValue(byteValue);
        }
        return result;
    }

}
