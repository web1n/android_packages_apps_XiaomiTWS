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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;
import org.lineageos.xiaomi_bluetooth.utils.EarbudsUtils;
import org.lineageos.xiaomi_bluetooth.utils.PowerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EarbudsService extends Service {

    public static String TAG = EarbudsService.class.getName();
    public static boolean DEBUG = true;

    private final ExecutorService earbudsExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, Boolean> bluetoothDeviceRecords = new ConcurrentHashMap<>();
    private final AtomicBoolean isEarbudsScanning = new AtomicBoolean();

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (DEBUG) Log.d(TAG, "device state changed");

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    if (DEBUG) Log.i(TAG, "clear all device");
                    bluetoothDeviceRecords.clear();
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null
                        && !bluetoothDeviceRecords.containsKey(device.getAddress())) {
                    runCheckXiaomiMMADevice(device);
                }
            }

            startOrStopEarbudsScan();
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

        earbudsExecutor.shutdownNow();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "startBluetoothStateListening");

        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(this);
        if (adapter != null && adapter.isEnabled()) {
            adapter.getBondedDevices().forEach(device -> {
                if (!device.isConnected()) return;

                runCheckXiaomiMMADevice(device);
            });
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        if (DEBUG) Log.d(TAG, "registering bluetooth state receiver");
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void stopBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "stopBluetoothStateListening");

        unregisterReceiver(bluetoothStateReceiver);
    }

    private boolean shouldStartScan() {
        if (!PowerUtils.isInteractive(this)) {
            return false;
        }

        boolean mmaDeviceConnected = isXiaomiMMADeviceConnected();
        if (DEBUG) Log.i(TAG, "isXiaomiMMADeviceConnected " + mmaDeviceConnected);

        return mmaDeviceConnected;
    }

    private boolean isXiaomiMMADeviceConnected() {
        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(this);
        if (adapter == null) {
            return false;
        }

        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (!device.isConnected()) continue;

            if (Boolean.TRUE.equals(
                    bluetoothDeviceRecords.getOrDefault(device.getAddress(), false))) {
                return true;
            }
        }

        return false;
    }

    private void runCheckXiaomiMMADevice(BluetoothDevice device) {
        if (earbudsExecutor.isShutdown() || earbudsExecutor.isTerminated()) {
            return;
        }
        if (!device.isConnected()) {
            return;
        }
        if (DEBUG) Log.i(TAG, "runCheckXiaomiMMADevice " + device.getName());

        earbudsExecutor.execute(() -> {
            if (bluetoothDeviceRecords.containsKey(device.getAddress())) {
                return;
            }
            boolean isMMADevice = EarbudsUtils.isXiaomiMMADevice(device);
            if (DEBUG) Log.i(TAG, device.getName() + " isMMADevice " + isMMADevice);

            bluetoothDeviceRecords.put(device.getAddress(), isMMADevice);
            mainHandler.post(this::startOrStopEarbudsScan);
        });
    }

    private void startOrStopEarbudsScan() {
        if (DEBUG) Log.i(TAG, "startOrStopEarbudsScan");

        boolean shouldStartScan = shouldStartScan();
        if (DEBUG) Log.i(TAG, "shouldStartScan " + shouldStartScan);
        if (shouldStartScan) {
            startEarbudsScan();
        } else {
            stopEarbudsScan();
        }
    }

    private void startEarbudsScan() {
        if (isEarbudsScanning.compareAndSet(false, true)) {
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
        }
    }

    private void stopEarbudsScan() {
        if (isEarbudsScanning.compareAndSet(true, false)) {
            if (DEBUG) Log.d(TAG, "stop scan earbuds");

            BluetoothLeScanner scanner = BluetoothUtils.getScanner(this);
            if (scanner == null) {
                return;
            }
            scanner.stopScan(EarbudsScanCallback);
        }
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
