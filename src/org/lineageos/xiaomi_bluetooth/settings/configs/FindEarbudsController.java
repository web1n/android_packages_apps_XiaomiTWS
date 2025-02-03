package org.lineageos.xiaomi_bluetooth.settings.configs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.lineageos.xiaomi_bluetooth.R;
import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;

import java.io.IOException;


public class FindEarbudsController extends SwitchController {

    private static final String TAG = FindEarbudsController.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final byte LEFT_EARBUD_FLAG = 0x01;
    private static final byte RIGHT_EARBUD_FLAG = 0x02;
    private static final byte ENABLE_FLAG = 0x01;
    private static final byte DISABLE_FLAG = 0x00;

    private static final byte[] ENABLED_CONFIG = {ENABLE_FLAG, LEFT_EARBUD_FLAG | RIGHT_EARBUD_FLAG};
    private static final byte[] DISABLED_CONFIG = {DISABLE_FLAG, LEFT_EARBUD_FLAG | RIGHT_EARBUD_FLAG};

    public FindEarbudsController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @NonNull
    @Override
    protected ConfigState getEnabledState() {
        return new ConfigState(ENABLED_CONFIG, R.string.find_earbuds_on);
    }

    @NonNull
    @Override
    protected ConfigState getDisabledState() {
        return new ConfigState(DISABLED_CONFIG, R.string.find_earbuds_off);
    }

    @Override
    public int getConfigId() {
        return EarbudsConstants.XIAOMI_MMA_CONFIG_FIND_EARBUDS;
    }

    @Override
    public int getExpectedConfigLength() {
        return 2;
    }

    @Override
    public boolean saveConfig(@NonNull MMADevice device, @NonNull Object value) throws IOException {
        if (!(value instanceof Boolean enable)) {
            Log.w(TAG, "Invalid value type: " + value.getClass().getSimpleName());
            return false;
        }
        Earbuds earbuds = validateEarbudsPresence(device);

        byte firstByte = enable ? ENABLE_FLAG : DISABLE_FLAG;
        byte secondByte = 0;
        if (earbuds.left != null) secondByte |= LEFT_EARBUD_FLAG;
        if (earbuds.right != null) secondByte |= RIGHT_EARBUD_FLAG;

        byte[] config = {firstByte, secondByte};
        return super.saveConfig(device, config);
    }

    @Override
    protected boolean isEnabled() {
        return isAvailable() && getConfigValue().length > 0 && getConfigValue()[0] == ENABLE_FLAG;
    }

    @NonNull
    private Earbuds validateEarbudsPresence(@NonNull MMADevice device) throws IOException {
        Earbuds earbuds = device.getBatteryInfo();

        if (earbuds == null || (earbuds.left == null && earbuds.right == null)) {
            throw new IllegalStateException("No earbuds information available");
        }

        return earbuds;
    }

}
