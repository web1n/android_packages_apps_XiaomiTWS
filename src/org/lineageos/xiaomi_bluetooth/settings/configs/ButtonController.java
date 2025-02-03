package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ButtonController extends ListController {

    private static final String TAG = ButtonController.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final byte VALUE_NOT_MODIFIED = -1;

    private enum Type {
        SINGLE_CLICK(0x04),
        DOUBLE_CLICK(0x01),
        TREBLE_CLICK(0x02),
        LONG_PRESS(0x03);

        private final byte value;

        Type(int value) {
            this.value = (byte) value;
        }

        @Nullable
        private static Type fromByte(byte value) {
            for (Type type : values()) {
                if (type.value == value) return type;
            }

            if (DEBUG) Log.w(TAG, "Unknown Type value: 0x" + String.format("%02x", value));
            return null;
        }
    }

    private enum Position {
        LEFT, RIGHT
    }

    private static final class ButtonConfig {
        private final Type type;
        private final Position position;
        private byte value;

        private ButtonConfig(Type type, Position position, byte value) {
            this.type = type;
            this.position = position;
            this.value = value;
        }

        @NonNull
        private byte[] toBytes() {
            return new byte[]{
                    type.value,
                    (position == Position.LEFT) ? value : VALUE_NOT_MODIFIED,
                    (position == Position.RIGHT) ? value : VALUE_NOT_MODIFIED
            };
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%02x", value);
        }

        @NonNull
        static List<ButtonConfig> parseFromBytes(@Nullable byte[] value) {
            if (value == null || value.length % 3 != 0) {
                if (DEBUG) {
                    Log.w(TAG, "Length must be multiple of 3. Actual: "
                            + (value != null ? value.length : "null"));
                }
                return Collections.emptyList();
            }

            List<ButtonConfig> configs = new ArrayList<>();
            ByteBuffer buffer = ByteBuffer.wrap(value);

            while (buffer.remaining() >= 3) {
                byte typeByte = buffer.get();
                byte leftValue = buffer.get();
                byte rightValue = buffer.get();

                Type type = Type.fromByte(typeByte);
                if (type != null) {
                    configs.add(new ButtonConfig(type, Position.LEFT, leftValue));
                    configs.add(new ButtonConfig(type, Position.RIGHT, rightValue));
                }
            }

            return configs;
        }
    }

    private static final List<ConfigState> CONFIG_STATES = Arrays.asList(
            new ConfigState(new byte[]{0x08}, R.string.function_disabled),
            new ConfigState(new byte[]{0x00}, R.string.function_voice_assistant),
            new ConfigState(new byte[]{0x01}, R.string.function_play_pause),
            new ConfigState(new byte[]{0x02}, R.string.function_previous_track),
            new ConfigState(new byte[]{0x03}, R.string.function_next_track),
            new ConfigState(new byte[]{0x04}, R.string.function_volume_up),
            new ConfigState(new byte[]{0x05}, R.string.function_volume_down),
            new ConfigState(new byte[]{0x06}, R.string.function_noise_control),
            new ConfigState(new byte[]{0x09}, R.string.function_screenshot)
    );

    private final Type type;
    private final Position position;

    public ButtonController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);

        int lastUnderlineIndex = preferenceKey.lastIndexOf('_');
        if (lastUnderlineIndex == -1) {
            throw new IllegalArgumentException("Invalid preference key format: " + preferenceKey);
        }

        try {
            type = Type.valueOf(preferenceKey.substring(0, lastUnderlineIndex).toUpperCase());
            position = Position.valueOf(preferenceKey.substring(lastUnderlineIndex + 1).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid type or position in preference key: " + preferenceKey, e);
        }
    }

    @Override
    public int getConfigId() {
        return EarbudsConstants.XIAOMI_MMA_CONFIG_BUTTON_MODE;
    }

    @Override
    public int getExpectedConfigLength() {
        return 1;
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

    @Override
    public boolean isValidValue(@Nullable byte[] value) {
        return !ButtonConfig.parseFromBytes(value).isEmpty();
    }

    @Override
    protected void updateValue(@NonNull Preference preference) {
        ButtonConfig buttonConfig = getButtonConfig();
        if (buttonConfig == null) {
            if (DEBUG) Log.w(TAG, "No button config found for update");
            return;
        }

        if (preference instanceof ListPreference listPreference) {
            listPreference.setValue(buttonConfig.toString());
        }
    }

    @Override
    public String getSummary() {
        ButtonConfig buttonConfig = getButtonConfig();
        if (buttonConfig == null) return null;

        int resId = getConfigStates().stream()
                .filter(state -> state.configValue[0] == buttonConfig.value)
                .findFirst().orElse(getDefaultState())
                .summaryResId;
        return context.getString(resId);
    }

    @Override
    public boolean saveConfig(MMADevice device, Object value) throws IOException {
        if (!(value instanceof String strValue)) {
            Log.w(TAG, "Unknown config value type");
            return false;
        }

        ButtonConfig buttonConfig = getButtonConfig();
        if (buttonConfig == null) {
            Log.w(TAG, "No existing button config to update");
            return false;
        }

        buttonConfig.value = CommonUtils.hexToBytes(strValue)[0];
        byte[] config = buttonConfig.toBytes();

        return super.saveConfig(device, config);
    }

    @Nullable
    private ButtonConfig getButtonConfig() {
        return ButtonConfig.parseFromBytes(getConfigValue()).stream()
                .filter(config -> config.type == type && config.position == position)
                .findFirst().orElse(null);
    }

}
