package org.lineageos.xiaomi_bluetooth.settings.configs;

import static org.lineageos.xiaomi_bluetooth.settings.EarbudsInfoFragment.ACTION_RELOAD_CONFIG;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settingslib.widget.BannerMessagePreference;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.R;


public class ReloadConfigController extends ConfigController {

    private static final String TAG = ReloadConfigController.class.getSimpleName();
    private static final boolean DEBUG = true;

    public ReloadConfigController(@NonNull Context context, @NonNull String preferenceKey) {
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
    public Available isAvailable() {
        return switch (super.isAvailable()) {
            case AVAILABLE -> Available.UNAVAILABLE;
            case UNAVAILABLE, UNKNOWN -> Available.AVAILABLE;
        };
    }

    @Override
    public void displayPreference(@NonNull Preference preference) {
        super.displayPreference(preference);

        ((BannerMessagePreference) preference)
                .setPositiveButtonText(R.string.reconnect_device)
                .setPositiveButtonOnClickListener(v ->
                        preference.getContext().sendBroadcast(new Intent(ACTION_RELOAD_CONFIG)));
    }

}
