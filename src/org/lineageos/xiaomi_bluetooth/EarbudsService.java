package org.lineageos.xiaomi_bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.utils.ATUtils;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;
import org.lineageos.xiaomi_bluetooth.utils.EarbudsUtils;

import java.util.Objects;


public class EarbudsService extends Service {

    private static final String TAG = EarbudsService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private BluetoothHeadset bluetoothHeadset;

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile != BluetoothProfile.HEADSET) {
                return;
            }

            if (DEBUG) Log.d(TAG, "Bluetooth headset connected: " + proxy);
            bluetoothHeadset = (BluetoothHeadset) proxy;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile != BluetoothProfile.HEADSET) {
                return;
            }

            if (DEBUG) Log.d(TAG, "Bluetooth headset disconnected");
            bluetoothHeadset = null;
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (DEBUG) Log.d(TAG, "onReceive: " + intent.getAction());
            BluetoothDevice device = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            if (device == null) {
                Log.w(TAG, "onReceive: Received intent with null device");
                return;
            }
            if (!device.isConnected()) {
                Log.w(TAG, "onReceive: Device is not connected");
                return;
            }
            if (bluetoothHeadset == null) {
                Log.w(TAG, "onReceive: bluetoothHeadset is null");
                return;
            }

            switch (intent.getAction()) {
                case BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT ->
                        handleATCommand(device, intent);
                default -> Log.w(TAG, "unknown action " + intent.getAction());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");

        initializeProfileProxy();
        startBluetoothStateListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy");

        closeProfileProxy();
        stopBluetoothStateListening();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleATCommand(@NonNull BluetoothDevice device, @NonNull Intent intent) {
        Earbuds earbuds = ATUtils.parseATCommandIntent(intent);

        if (earbuds != null) {
            EarbudsUtils.updateEarbudsStatus(earbuds);
        } else {
            ATUtils.sendUpdateATCommand(bluetoothHeadset, device);
        }
    }

    private void initializeProfileProxy() {
        Objects.requireNonNull(BluetoothUtils.getBluetoothAdapter(this))
                .getProfileProxy(this, profileListener, BluetoothProfile.HEADSET);
    }

    private void closeProfileProxy() {
        if (bluetoothHeadset != null) {
            Objects.requireNonNull(BluetoothUtils.getBluetoothAdapter(this))
                    .closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
            bluetoothHeadset = null;
        }
    }

    private void startBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "startBluetoothStateListening");

        IntentFilter filter = new IntentFilter();

        // Xiaomi AT event
        filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        filter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
                + "." + ATUtils.MANUFACTURER_ID_XIAOMI);

        if (DEBUG) Log.d(TAG, "registering bluetooth state receiver");
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void stopBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "stopBluetoothStateListening");

        unregisterReceiver(bluetoothStateReceiver);
    }

}
