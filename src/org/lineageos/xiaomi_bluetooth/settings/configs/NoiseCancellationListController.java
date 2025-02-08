package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;

import java.io.IOException;
import java.util.List;
import java.util.Set;


public class NoiseCancellationListController extends MultiListController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = ListController.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final byte VALUE_NOT_MODIFIED = -1;

    private static final List<ConfigState> CONFIG_STATES = List.of(
            new ConfigState(new byte[]{0x00}, R.string.noise_cancellation_mode_off),
            new ConfigState(new byte[]{0x01}, R.string.noise_cancellation_mode_on),
            new ConfigState(new byte[]{0x02}, R.string.noise_cancellation_mode_transparency)
    );

    private enum Position {
        LEFT, RIGHT
    }

    @NonNull
    private final Position position;

    public NoiseCancellationListController(@NonNull Context context,
                                           @NonNull String preferenceKey) {
        super(context, preferenceKey);

        position = Position.valueOf(preferenceKey.split("_")[0].toUpperCase());
    }

    @Override
    public int getConfigId() {
        return EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_LIST;
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
    protected List<ConfigState> getCheckedStates() {
        if (DEBUG) Log.d(TAG, "getCheckedStates");

        if (getConfigValue() == null || !isValidValue(getConfigValue())) {
            return List.of();
        }

        byte configByte = getConfigValue()[position == Position.LEFT ? 0 : 1];

        return CONFIG_STATES.stream()
                .filter(state -> (configByte & (1 << state.configValue[0])) > 0)
                .toList();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (DEBUG) Log.d(TAG, "onPreferenceChange: " + o);

        return ((Set<?>) o).size() >= 2;
    }

    @Override
    public boolean saveConfig(@NonNull MMADevice device, @NonNull Object value) throws IOException {
        if (DEBUG) Log.d(TAG, "saveConfig: " + value);

        byte valueByte = ((Set<?>) value).stream()
                .map(s -> 1 << Integer.parseInt((String) s))
                .reduce(0, Integer::sum).byteValue();
        value = position == Position.LEFT
                ? new byte[]{valueByte, VALUE_NOT_MODIFIED}
                : new byte[]{VALUE_NOT_MODIFIED, valueByte};

        return super.saveConfig(device, value);
    }

}
