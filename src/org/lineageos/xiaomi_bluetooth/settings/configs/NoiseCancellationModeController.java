package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;

import androidx.annotation.NonNull;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.EarbudsConstants;

import java.util.List;


public class NoiseCancellationModeController extends ListController {

    private static final List<ConfigState> CONFIG_STATES = List.of(
            new ConfigState(new byte[]{0x00, 0x00}, R.string.noise_cancellation_mode_off),
            new ConfigState(new byte[]{0x01, 0x00}, R.string.noise_cancellation_mode_on),
            new ConfigState(new byte[]{0x02, 0x00}, R.string.noise_cancellation_mode_transparency)
    );

    public NoiseCancellationModeController(@NonNull Context context,
                                           @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getConfigId() {
        return EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE;
    }

    @Override
    public int getExpectedConfigLength() {
        return 2;
    }

    @NonNull
    @Override
    protected List<ConfigState> getConfigStates() {
        return CONFIG_STATES;
    }

    @NonNull
    @Override
    protected ConfigState getDefaultState() {
        return CONFIG_STATES.getFirst();
    }

}
