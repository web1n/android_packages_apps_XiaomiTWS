package org.lineageos.xiaomi_bluetooth.settings;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;


public class EarbudsActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = EarbudsActivity.class.getName();
    private static final boolean DEBUG = true;

    private static final String TAG_EARBUDS = "earbuds";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothDevice device = getIntent()
                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        if (DEBUG) Log.d(TAG, "onCreate: " + device);
        if (device == null) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            EarbudsFragment fragment = new EarbudsFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
            fragment.setArguments(bundle);

            getFragmentManager().beginTransaction().replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    fragment, TAG_EARBUDS).commit();
        }
    }

}
