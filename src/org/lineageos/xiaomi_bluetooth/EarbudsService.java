package org.lineageos.xiaomi_bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;
import org.lineageos.xiaomi_bluetooth.utils.EarbudsUtils;
import org.lineageos.xiaomi_bluetooth.utils.PowerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class EarbudsService extends Service {

    public static String TAG = EarbudsService.class.getName();
    public static boolean DEBUG = true;

    private final AtomicBoolean scanning = new AtomicBoolean();

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "device state changed");
            boolean shouldStartScan = PowerUtils.isInteractive(context)
                    && BluetoothUtils.isAnyDeviceConnected(context);

            if (shouldStartScan) {
                startEarbudsScan();
            } else {
                stopEarbudsScan();
            }
        }
    };

    private final EarbudsScanCallback EarbudsScanCallback = new EarbudsScanCallback() {
        @Override
        public void onEarbudsScanResult(@NonNull Earbuds earbuds) {
            updateEarbudsStatus(earbuds);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");

        startBluetoothStateListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy");

        stopEarbudsScan();
        stopBluetoothStateListening();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "startBluetoothStateListening");

        if (BluetoothUtils.isAnyDeviceConnected(this)) {
            startEarbudsScan();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void stopBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "stopBluetoothStateListening");

        unregisterReceiver(bluetoothStateReceiver);
    }

    private void startEarbudsScan() {
        if (scanning.get()) return;
        if (DEBUG) Log.d(TAG, "start scan earbuds");

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(EarbudsConstants.SCAN_REPORT_DELAY)
                .build();
        List<ScanFilter> filters = new ArrayList<>();

        BluetoothLeScanner scanner = BluetoothUtils.getScanner(this);
        if (scanner == null) {
            return;
        }
        scanner.startScan(filters, settings, EarbudsScanCallback);
        scanning.set(true);
    }

    private void stopEarbudsScan() {
        if (!scanning.get()) return;
        if (DEBUG) Log.d(TAG, "stop scan earbuds");

        BluetoothLeScanner scanner = BluetoothUtils.getScanner(this);
        if (scanner == null) {
            return;
        }
        scanner.stopScan(EarbudsScanCallback);
        scanning.set(false);
    }

    private void updateEarbudsStatus(Earbuds earbuds) {
        if (!earbuds.isValid()) return;
        if (DEBUG) Log.d(TAG, "updateEarbudsStatus " + earbuds);

        BluetoothDevice device =
                BluetoothUtils.getBluetoothDevice(earbuds.macAddress);
        if (device == null || !device.isConnected()) {
            if (DEBUG) Log.d(TAG, "device is null or not connected " + earbuds.macAddress);
            return;
        }

        EarbudsUtils.setBluetoothDeviceType(device);
        EarbudsUtils.updateEarbudsStatus(device, earbuds);
    }

}
