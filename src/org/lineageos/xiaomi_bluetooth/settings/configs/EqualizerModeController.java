package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lineageos.xiaomi_bluetooth.R;

import static org.lineageos.xiaomi_bluetooth.EarbudsConstants.*;


public class EqualizerModeController extends ListController {

    private static final String TAG = EqualizerModeController.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final Map<Integer, Set<Integer>> DEVICE_SUPPORTED_STATES = Map.ofEntries(
            Map.entry(0x2717_5035, Set.of(0, 1, 5, 6, 11, 12)),
            Map.entry(0x2717_503B, Set.of(0, 5, 6, 12)),
            Map.entry(0x2717_506A, Set.of(0, 1, 5, 6)),
            Map.entry(0x2717_506B, Set.of(0, 1, 5, 6)),
            Map.entry(0x2717_506C, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_506D, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_506F, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_5075, Set.of(0, 1, 5, 6)),
            Map.entry(0x2717_507F, Set.of(0, 1, 6)),
            Map.entry(0x2717_5080, Set.of(0, 1, 6)),
            Map.entry(0x2717_5081, Set.of(1, 6, 10, 11, 13, 14)),
            Map.entry(0x2717_5082, Set.of(1, 6, 10, 11, 13, 14)),
            Map.entry(0x2717_5088, Set.of(0, 1, 5, 6, 7)),
            Map.entry(0x2717_5089, Set.of(0, 1, 5, 6, 7)),
            Map.entry(0x2717_508A, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_508B, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_5095, Set.of(0, 1, 5, 6)),
            Map.entry(0x2717_509A, Set.of(0, 1, 5, 6, 7)),
            Map.entry(0x2717_509B, Set.of(0, 1, 5, 6, 7)),
            Map.entry(0x2717_509C, Set.of(0, 1, 5, 6, 7)),
            Map.entry(0x2717_509D, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_509E, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_509F, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_50A0, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x2717_50AF, Set.of(0, 1, 5, 6, 10)),
            Map.entry(0x5A4D_EA03, Set.of(0, 1, 5, 6)),
            Map.entry(0x5A4D_EA0D, Set.of(0, 1, 5, 6)),
            Map.entry(0x5A4D_EA0E, Set.of(0, 1, 5, 6)),
            Map.entry(0x5A4D_EA0F, Set.of(0, 1, 5, 6))
    );

    private static final List<ConfigState> CONFIG_STATES = List.of(
            new ConfigState(new byte[]{0x00}, R.string.equalizer_mode_default),
            new ConfigState(new byte[]{0x01}, R.string.equalizer_mode_vocal_enhance),
            new ConfigState(new byte[]{0x05}, R.string.equalizer_mode_bass_boost),
            new ConfigState(new byte[]{0x06}, R.string.equalizer_mode_treble_boost),
            new ConfigState(new byte[]{0x07}, R.string.equalizer_mode_volume_boost),
            new ConfigState(new byte[]{0x14}, R.string.equalizer_mode_harman)
    );

    public EqualizerModeController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getConfigId() {
        return XIAOMI_MMA_CONFIG_EQUALIZER_MODE;
    }

    @Override
    public int getExpectedConfigLength() {
        return 1;
    }

    @NonNull
    @Override
    protected ConfigState getDefaultState() {
        return CONFIG_STATES.getFirst();
    }

    @NonNull
    @Override
    protected List<ConfigState> getConfigStates() {
        Set<Integer> supportedModes = DEVICE_SUPPORTED_STATES.get((getVid() << 16) | getPid());
        if (DEBUG) {
            Log.d(TAG, String.format("Supported modes: %s",
                    supportedModes == null ? "null" : Arrays.toString(supportedModes.toArray())));
        }
        if (supportedModes == null || supportedModes.isEmpty()) {
            return List.of(getDefaultState());
        }

        return CONFIG_STATES.stream()
                .filter(state -> supportedModes.contains((int) state.configValue[0]))
                .toList();
    }

}
