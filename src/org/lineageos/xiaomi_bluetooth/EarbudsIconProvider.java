package org.lineageos.xiaomi_bluetooth;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


public class EarbudsIconProvider extends ContentProvider {

    public static final String TAG = EarbudsIconProvider.class.getName();
    public static final boolean DEBUG = true;

    private static final String AUTHORITY_ICONS = "org.lineageos.xiaomi_bluetooth_icons";

    private static final String METHOD_GRANT_URI_PERMISSION = "grant_uri_permission";

    private static final String PACKAGE_NAME_ANDROID_SETTINGS = "com.android.settings";
    private static final String PACKAGE_NAME_XIAOMI_BLUETOOTH = "org.lineageos.xiaomi_bluetooth";

    public static final String TYPE_CASE = "case";
    public static final String TYPE_LEFT = "left";
    public static final String TYPE_RIGHT = "right";

    private static final int VENDOR_ID_FALLBACK = 0x2717;
    private static final int PRODUCT_ID_FALLBACK = 0x509F;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (DEBUG) Log.d(TAG, "call: " + method + " " + arg);
        if (!PACKAGE_NAME_XIAOMI_BLUETOOTH.equals(getCallingPackage())) {
            if (DEBUG) Log.d(TAG, "calling package not xiaomi-bluetooth");
            return null;
        } else if (!METHOD_GRANT_URI_PERMISSION.equals(method)) {
            if (DEBUG) Log.d(TAG, "method not " + METHOD_GRANT_URI_PERMISSION);
            return null;
        }

        Uri uri = Uri.parse(arg);
        if (!AUTHORITY_ICONS.equals(uri.getAuthority())) {
            if (DEBUG) Log.d(TAG, "authority not equal " + uri.getAuthority());
            return null;
        }

        // grant permission
        requireContext().grantUriPermission(PACKAGE_NAME_ANDROID_SETTINGS, uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return null;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        if (DEBUG) Log.d(TAG, "openAssetFile: " + uri + " " + mode);
        String type = uri.getLastPathSegment();
        if (!(TYPE_CASE.equals(type) || TYPE_LEFT.equals(type) || TYPE_RIGHT.equals(type))) {
            throw new FileNotFoundException("type not valid");
        }

        List<String> segments = uri.getPathSegments();
        if (segments.size() < 3) {
            throw new FileNotFoundException("not a valid uri");
        }

        int vid, pid;
        try {
            vid = Integer.parseInt(segments.get(segments.size() - 3));
            pid = Integer.parseInt(segments.get(segments.size() - 2));
        } catch (NumberFormatException e) {
            throw new FileNotFoundException("unable to parse vid or pid");
        }

        AssetManager assetManager = Objects.requireNonNull(getContext()).getAssets();
        try {
            return openFD(assetManager, type, vid, pid);
        } catch (IOException e) {
            Log.w(TAG, type + " icon not exist, use fallback one");

            try {
                return openFD(assetManager, type, VENDOR_ID_FALLBACK, PRODUCT_ID_FALLBACK);
            } catch (IOException e1) {
                throw new FileNotFoundException("icon not found");
            }
        }
    }

    private static AssetFileDescriptor openFD(AssetManager am, String type,
                                              int vendorId, int productId) throws IOException {
        String fileName = String.format("icons/%d_%d_%s.png", vendorId, productId, type);
        return am.openFd(fileName);
    }

    public static String generateIconUri(Context context, int vendorId, int productId, String type) {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY_ICONS)
                .appendPath(String.valueOf(vendorId))
                .appendPath(String.valueOf(productId))
                .appendPath(type)
                .build();

        // grant permission
        context.getContentResolver().call(uri, METHOD_GRANT_URI_PERMISSION, uri.toString(), null);

        return uri.toString();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

}
