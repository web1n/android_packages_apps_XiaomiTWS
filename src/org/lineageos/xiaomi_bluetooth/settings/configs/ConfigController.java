package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.IOException;
import java.util.Arrays;


public abstract class ConfigController {

    private static final String TAG = ConfigController.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final int CONFIG_ID_INVALID = 0x01;
    protected static final byte VALUE_FEATURE_NOT_SUPPORTED = -1;

    public enum Available {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }

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
    protected final Context context;
    @NonNull
    public final String preferenceKey;
    @Nullable
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

    @Nullable
    public Integer getVid() {
        return vid;
    }

    @Nullable
    public Integer getPid() {
        return pid;
    }

    public final void setConfigValue(@Nullable byte[] configValue) {
        if (configValue == null || !isValidValue(configValue)) {
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

    public Available isAvailable() {
        if (configValue == null) {
            return Available.UNKNOWN;
        }

        return isValidValue(configValue) && !isNotSupported()
                ? Available.AVAILABLE
                : Available.UNAVAILABLE;
    }

    protected final boolean isNotSupported() {
        return configValue != null
                && configValue.length == 1
                && configValue[0] == VALUE_FEATURE_NOT_SUPPORTED;
    }

    public void displayPreference(@NonNull Preference preference) {
        preference.setPersistent(false);
        updateState(preference);
    }

    public void updateState(@NonNull Preference preference) {
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
        String summary = isAvailable() == Available.AVAILABLE
                ? getSummary()
                : null;

        if (summary != null) {
            preference.setSummary(summary);
        }
    }

    private void updateVisible(@NonNull Preference preference) {
        Available available = isAvailable();

        preference.setVisible(available != Available.UNAVAILABLE);
        preference.setSelectable(available == Available.AVAILABLE);

        updateParentVisibility(preference);
    }

    private static void updateParentVisibility(@NonNull Preference preference) {
        PreferenceGroup parent = preference.getParent();
        if (parent == null) {
            return;
        }

        if (preference.isVisible()) {
            parent.setVisible(true);
        } else {
            boolean hasVisibleChildren = false;
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference p = parent.getPreference(i);
                if (p.isVisible()) {
                    hasVisibleChildren = true;
                    break;
                }
            }
            parent.setVisible(hasVisibleChildren);
        }
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
