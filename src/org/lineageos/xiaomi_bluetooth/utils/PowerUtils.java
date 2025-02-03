package org.lineageos.xiaomi_bluetooth.utils;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.Nullable;

import java.util.Objects;


public class PowerUtils {

    private static final String TAG = PowerUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static boolean isInteractive(@Nullable Context context) {
        if (context == null) {
            return false;
        }

        return Objects.requireNonNull(context.getSystemService(PowerManager.class)).isInteractive();
    }

}
