package org.lineageos.xiaomi_bluetooth.settings;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import java.util.Map;
import java.util.Objects;


public class EarbudsActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = EarbudsActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final Map<String, Class<? extends Fragment>> FRAGMENTS = Map.of(
            "org.lineageos.xiaomi_bluetooth.settings.EarbudsInfoActivity", EarbudsInfoFragment.class
    );

    private static final String TAG_EARBUDS = "earbuds";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    createFragment(), TAG_EARBUDS).commit();
        }
    }

    @NonNull
    private Fragment createFragment() {
        ResolveInfo resolveInfo = getPackageManager()
                .resolveActivity(getIntent(), PackageManager.MATCH_DEFAULT_ONLY);
        String activityName = resolveInfo != null ? resolveInfo.activityInfo.name : null;

        Class<?> fragmentClass = Objects.requireNonNull(
                FRAGMENTS.getOrDefault(activityName, EarbudsListFragment.class));
        if (DEBUG) Log.d(TAG, "createFragment: class: " + fragmentClass);

        try {
            return (Fragment) fragmentClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create fragment: " + fragmentClass, e);
        }
    }

}
