package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;

import androidx.annotation.NonNull;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;

import java.nio.charset.StandardCharsets;


public class SerialNumberController extends ConfigController {

    public SerialNumberController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getConfigId() {
        return EarbudsConstants.XIAOMI_MMA_CONFIG_SN;
    }

    @Override
    public int getExpectedConfigLength() {
        return 20;
    }

    @Override
    public String getSummary() {
        if (getConfigValue() == null) {
            return null;
        }

        try {
            return new String(getConfigValue(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

}
