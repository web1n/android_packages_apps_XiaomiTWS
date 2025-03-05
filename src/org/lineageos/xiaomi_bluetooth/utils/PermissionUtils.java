package org.lineageos.xiaomi_bluetooth.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;


public class PermissionUtils {

    private static final String TAG = PermissionUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    @NonNull
    public static String[] getMissingRuntimePermissions(@NonNull Context context) {
        String[] requestedPermissions = null;
        try {
            requestedPermissions = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getMissingRuntimePermissions: ", e);
        }
        if (requestedPermissions == null) {
            return new String[0];
        }

        String[] missingRuntimePermissions = Arrays.stream(requestedPermissions)
                .filter(permission -> isRuntimePermission(context, permission))
                .filter(permission -> checkSelfPermission(context, permission))
                .toArray(String[]::new);
        if (DEBUG) Log.d(TAG,
                "getMissingRuntimePermissions: " + Arrays.toString(missingRuntimePermissions));
        return missingRuntimePermissions;
    }

    public static boolean checkSelfPermission(@NonNull Context context,
                                              @NonNull String permissionName) {
        return context.checkSelfPermission(permissionName) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isRuntimePermission(@NonNull Context context,
                                              @NonNull String permissionName) {
        try {
            return context.getPackageManager().getPermissionInfo(permissionName, 0).getProtection()
                    == PermissionInfo.PROTECTION_DANGEROUS;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
