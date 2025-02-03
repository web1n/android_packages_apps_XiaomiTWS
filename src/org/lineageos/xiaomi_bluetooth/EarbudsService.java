package org.lineageos.xiaomi_bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.mma.MMADevice;
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;
import org.lineageos.xiaomi_bluetooth.utils.EarbudsUtils;
import org.lineageos.xiaomi_bluetooth.utils.PowerUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EarbudsService extends Service {

    private static final String TAG = EarbudsService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int MMA_DEVICE_CHECK_TIMEOUT_MS = 1000;

    private final ExecutorService earbudsExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Boolean> bluetoothDeviceRecords = new ConcurrentHashMap<>();
    private final AtomicBoolean isEarbudsScanning = new AtomicBoolean();

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (DEBUG) Log.d(TAG, "device state changed " + intent.getAction());
            BluetoothDevice device = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    if (DEBUG) Log.d(TAG, "clear all device");
                    bluetoothDeviceRecords.clear();
                }
            } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                if (device != null
                        && state == BluetoothHeadset.STATE_CONNECTED
                        && !bluetoothDeviceRecords.containsKey(device.getAddress())) {
                    runCheckEarbudsStatus(device, true);
                }
            } else if (BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED.equals(intent.getAction())) {
                if (device != null && Boolean.TRUE
                        .equals(bluetoothDeviceRecords.getOrDefault(device.getAddress(), false))) {
                    runCheckEarbudsStatus(device, false);
                }
            } else if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT.equals(intent.getAction())) {
                if (device != null) {
                    runCheckATCommand(device, intent);
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())
                    || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())
                    || Intent.ACTION_SCREEN_ON.equals(intent.getAction())
                    || Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                startOrStopEarbudsScan();
            }
        }
    };

    private final EarbudsScanCallback earbudsScanCallback = new EarbudsScanCallback() {
        @Override
        public void onEarbudsScanResult(@NonNull Earbuds earbuds) {
            EarbudsUtils.updateEarbudsStatus(earbuds);
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

    private void runCheckATCommand(@NonNull BluetoothDevice device, @NonNull Intent intent) {
        if (DEBUG) Log.d(TAG, "runCheckATCommand");
        String cmd = intent.getStringExtra(
                BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
        int type = intent.getIntExtra(
                BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,
                BluetoothHeadset.AT_CMD_TYPE_READ);
        Object[] args = intent.getSerializableExtra(
                BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, Object[].class);

        if (!EarbudsConstants.VENDOR_SPECIFIC_HEADSET_EVENT_XIAOMI.equals(cmd)) {
            if (DEBUG) Log.d(TAG, "runCheckATCommand: cmd not xiaomi: " + cmd);
            return;
        } else if (type != BluetoothHeadset.AT_CMD_TYPE_SET) {
            if (DEBUG) Log.d(TAG, "runCheckATCommand: type not AT_CMD_TYPE_SET " + type);
            return;
        } else if (args == null || args.length != 1 || !(args[0] instanceof String)) {
            if (DEBUG) Log.d(TAG, "runCheckATCommand: args not valid");
            return;
        }

        Earbuds earbuds = EarbudsUtils.parseXiaomiATCommand(device, (String) args[0]);
        if (DEBUG) Log.d(TAG, "runCheckATCommand: " + args[0] + " " + earbuds);

        if (earbuds != null) {
            EarbudsUtils.updateEarbudsStatus(earbuds);
        }
    }

    private void startBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "startBluetoothStateListening");

        BluetoothUtils.getConnectedHeadsetA2DPDevices().forEach(
                device -> runCheckEarbudsStatus(device, true));

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        // Xiaomi AT event
        filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        filter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
                + "." + EarbudsConstants.MANUFACTURER_ID_XIAOMI);

        if (DEBUG) Log.d(TAG, "registering bluetooth state receiver");
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void stopBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "stopBluetoothStateListening");

        unregisterReceiver(bluetoothStateReceiver);
    }

    private void runCheckEarbudsStatus(@NonNull BluetoothDevice device, boolean firstCheck) {
        if (earbudsExecutor.isShutdown() || earbudsExecutor.isTerminated()) {
            return;
        }
        if (!BluetoothUtils.isConnectedHeadsetA2DPDevice(device)) {
            if (DEBUG) Log.d(TAG, "runCheckEarbudsStatus: " + device + " not headset a2dp device");
            return;
        }
        if (DEBUG) Log.d(TAG, "runCheckEarbudsStatus " + device.getName());

        earbudsExecutor.execute(() -> {
            boolean status = false;
            try {
                status = CommonUtils.executeWithTimeout(
                        () -> checkEarbudsStatus(device, firstCheck), MMA_DEVICE_CHECK_TIMEOUT_MS);
            } catch (RuntimeException | TimeoutException e) {
                Log.e(TAG, "runCheckEarbudsStatus: ", e);
            }
            if (DEBUG) Log.d(TAG, "runCheckEarbudsStatus: " + status);

            if (firstCheck) {
                bluetoothDeviceRecords.put(device.getAddress(), status);
                startOrStopEarbudsScan();
            }
        });
    }

    private boolean checkEarbudsStatus(@NonNull BluetoothDevice device,
                                       boolean firstCheck) throws IOException {
        if (DEBUG) Log.d(TAG, "checkEarbudsStatus " + firstCheck);
        if (!BluetoothUtils.isConnectedHeadsetA2DPDevice(device)) {
            return false;
        }

        try (MMADevice mma = new MMADevice(device)) {
            mma.connect();

            if (firstCheck) {
                Pair<Integer, Integer> vidPid = mma.getVidPid();
                String softwareVersion = mma.getSoftwareVersion();

                if (vidPid != null && softwareVersion != null) {
                    // set metadata
                    EarbudsUtils.setEarbudsModelData(getApplication(),
                            device, vidPid.first, vidPid.second, softwareVersion);
                    return true;
                }
            } else {
                Earbuds earbuds = mma.getBatteryInfo();

                if (earbuds != null) {
                    // update earbuds battery
                    EarbudsUtils.updateEarbudsStatus(earbuds);
                    return true;
                }
            }

            return false;
        }
    }

    private void startOrStopEarbudsScan() {
        if (DEBUG) Log.d(TAG, "startOrStopEarbudsScan");

        boolean shouldStartScan = PowerUtils.isInteractive(this)
                && bluetoothDeviceRecords.containsValue(true);
        if (DEBUG) Log.d(TAG, "shouldStartScan " + shouldStartScan);
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
            scanner.startScan(filters, settings, earbudsScanCallback);
        }
    }

    private void stopEarbudsScan() {
        if (isEarbudsScanning.compareAndSet(true, false)) {
            if (DEBUG) Log.d(TAG, "stop scan earbuds");

            BluetoothLeScanner scanner = BluetoothUtils.getScanner(this);
            if (scanner == null) {
                return;
            }
            scanner.stopScan(earbudsScanCallback);
        }
    }

}
