package org.lineageos.xiaomi_bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    public static String TAG = BootCompletedReceiver.class.getName();
    public static boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (DEBUG) Log.d(TAG, "boot completed");

        context.startServiceAsUser(
                new Intent(context, EarbudsService.class), UserHandle.CURRENT);
    }

}
