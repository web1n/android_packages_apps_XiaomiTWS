package org.lineageos.xiaomi_bluetooth.utils;

import android.content.Context;
import android.os.PowerManager;


public class PowerUtils {

    public static String TAG = PowerUtils.class.getName();
    public static boolean DEBUG = true;


    public static boolean isInteractive(Context context) {
        if (context == null) {
            return false;
        }

        return context.getSystemService(PowerManager.class).isInteractive();
    }

}
