package org.lineageos.xiaomi_bluetooth.mma;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MMADevice implements AutoCloseable {

    public static final String TAG = MMADevice.class.getName();
    public static final boolean DEBUG = true;

    private final BluetoothDevice device;

    private BluetoothSocket socket;
    private Byte opCodeSN;

    public MMADevice(@NonNull BluetoothDevice device) {
        this.device = device;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public void checkConnected() throws IOException {
        if (!isConnected()) {
            throw new IOException("not connected");
        }
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return device;
    }

    public byte getNewOpCodeSN() {
        if (opCodeSN == null) {
            opCodeSN = (byte) (new Random().nextInt(256) - 128);
        }

        opCodeSN = (byte) (opCodeSN + 1);
        return opCodeSN;
    }

    public void sendRequest(@NonNull MMARequest request) throws IOException {
        checkConnected();

        byte[] data = request.toBytes();
        if (DEBUG) Log.d(TAG, "sendRequest: " + Arrays.toString(data));

        try {
            OutputStream outputStream = this.socket.getOutputStream();

            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "sendReceivePacket: ", e);
            throw e;
        }
    }

    @Nullable
    public MMAResponse readResponse() throws IOException {
        checkConnected();

        try {
            byte[] responsePacket = getResponsePacket(this.socket.getInputStream());
            return MMAResponse.fromPacket(responsePacket);
        } catch (IOException e) {
            Log.e(TAG, "sendReceivePacket: ", e);
            throw e;
        }
    }

    @Nullable
    public synchronized MMAResponse sendReceive(@NonNull MMARequest request) throws IOException {
        checkConnected();

        sendRequest(request);

        for (int i = 0; i < 5; i++) {
            MMAResponse response = readResponse();
            if (response == null) continue;
            if (DEBUG) Log.d(TAG, "sendReceive: " + i + " " + response);

            if (response.opCode != request.opCode || response.opCodeSN != request.opCodeSN) {
                continue;
            }
            return response;
        }

        return null;
    }

    @Nullable
    private byte[] getDeviceInfo(int mask, @Nullable Integer expectedLength) throws IOException {
        MMARequest request = new MMARequest(
                EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_INFO, getNewOpCodeSN(),
                new byte[]{0x00, 0x00, 0x00, (byte) (1 << mask)}, true);

        MMAResponse response = sendReceive(request);
        if (response == null
                || (expectedLength != null && response.data.length != expectedLength)
                || response.data[0] != response.data.length - 1
                || response.data[1] != mask) {
            return null;
        }

        return response.data;
    }

    @Nullable
    public String getUBootVersion() throws IOException {
        if (DEBUG) Log.d(TAG, "getUBootVersion");
        byte[] data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_UBOOT_VERSION, 4);
        if (data == null) {
            return null;
        }

        String version = CommonUtils.intToVersion(((data[2] & 0xFF) << 8) | (data[3] & 0xFF));

        if (DEBUG) Log.d(TAG, "getUBootVersion: " + version);
        return version;
    }

    @Nullable
    public String getSoftwareVersion() throws IOException {
        if (DEBUG) Log.d(TAG, "getSoftwareVersion");
        byte[] data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_VERSION, 6);
        if (data == null) {
            return null;
        }

        String primaryVersion = CommonUtils.intToVersion(((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
        String secondaryVersion = CommonUtils.intToVersion(((data[4] & 0xFF) << 8) | (data[5] & 0xFF));

        if (DEBUG) Log.d(TAG, "getSoftwareVersion: primary: " + primaryVersion);
        if (DEBUG) Log.d(TAG, "getSoftwareVersion: secondary: " + secondaryVersion);
        return primaryVersion;
    }

    @Nullable
    public Earbuds getBatteryInfo() throws IOException {
        if (DEBUG) Log.d(TAG, "getBatteryInfo");
        byte[] data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_BATTERY, 5);
        if (data == null) {
            return null;
        }

        Earbuds earbuds = Earbuds.fromBytes(device.getAddress(), data[2], data[3], data[4]);

        if (DEBUG) Log.d(TAG, "getBatteryInfo " + earbuds);
        return earbuds;
    }

    @Nullable
    public Pair<Integer, Integer> getVidPid() throws IOException {
        if (DEBUG) Log.d(TAG, "getVidPid");
        byte[] data = getDeviceInfo(EarbudsConstants.XIAOMI_MMA_MASK_GET_VID_PID, 6);
        if (data == null) {
            return null;
        }

        int vendorId = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int productId = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);

        if (DEBUG) Log.d(TAG, "getVidPid: vid: " + vendorId + " pid: " + productId);
        return new Pair<>(vendorId, productId);
    }

    @NonNull
    public Map<Integer, byte[]> getDeviceConfig(@NonNull int[] configs) throws IOException {
        if (DEBUG) Log.d(TAG, "getDeviceConfig");

        byte[] requestData = new byte[configs.length * 2];
        for (int i = 0; i < configs.length; i++) {
            requestData[i * 2] = (byte) ((configs[i] >> 8) & 0xFF);
            requestData[(i * 2) + 1] = (byte) (configs[i] & 0xFF);
        }
        if (DEBUG) Log.d(TAG, "getDeviceConfig: " + Arrays.toString(requestData));

        MMARequest request = new MMARequest(
                EarbudsConstants.XIAOMI_MMA_OPCODE_GET_DEVICE_CONFIG,
                getNewOpCodeSN(), requestData, true);
        MMAResponse response = sendReceive(request);

        Map<Integer, byte[]> configValues = new HashMap<>();
        if (response == null) {
            return configValues;
        }

        ByteBuffer buffer = ByteBuffer.wrap(response.data);
        while (buffer.remaining() >= 4) {
            int length = buffer.get();
            if (length < 2 || length > buffer.remaining()) {
                break;
            }
            int key = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
            byte[] value = new byte[length - 2];
            buffer.get(value, 0, value.length);

            if (Arrays.binarySearch(configs, key) == -1) {
                continue;
            }

            configValues.put(key, value);
        }

        return configValues;
    }

    @Nullable
    public byte[] getDeviceConfig(int config, int expectedLength) throws IOException {
        if (DEBUG) Log.d(TAG, "getDeviceConfig");

        Map<Integer, byte[]> configs = getDeviceConfig(new int[]{config});
        byte[] configBytes = configs.getOrDefault(config, null);

        if (configBytes == null || configBytes.length != expectedLength) {
            return null;
        }
        return configBytes;
    }

    public boolean setDeviceConfig(@NonNull Map<Integer, byte[]> configs) throws IOException {
        if (DEBUG) Log.d(TAG, "setDeviceConfig");

        List<Byte> requestDataList = new ArrayList<>();
        for (Map.Entry<Integer, byte[]> entry : configs.entrySet()) {
            requestDataList.add((byte) (entry.getValue().length + 2));

            requestDataList.add((byte) ((entry.getKey() >> 8) & 0xFF));
            requestDataList.add((byte) (entry.getKey() & 0xFF));

            for (byte b : entry.getValue()) requestDataList.add(b);
        }
        byte[] requestData = CommonUtils.byteListToArray(requestDataList);

        MMARequest request = new MMARequest(
                EarbudsConstants.XIAOMI_MMA_OPCODE_SET_DEVICE_CONFIG,
                getNewOpCodeSN(), requestData, true);
        MMAResponse response = sendReceive(request);

        if (response == null) {
            return false;
        }
        return response.status == EarbudsConstants.XIAOMI_MMA_RESPONSE_STATUS_OK;
    }

    public boolean setDeviceConfig(int config, byte[] value) throws IOException {
        if (DEBUG) Log.d(TAG, "setDeviceConfig");

        Map<Integer, byte[]> configs = new HashMap<>();
        configs.put(config, value);

        return setDeviceConfig(configs);
    }

    public byte getEqualizerMode() throws IOException {
        if (DEBUG) Log.d(TAG, "getEqualizerMode");

        byte[] eqModeBytes = getDeviceConfig(EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE, 1);
        if (eqModeBytes != null) {
            byte eqMode = eqModeBytes[0];
            if (DEBUG) Log.d(TAG, "getEqualizerMode " + eqMode);

            if (eqMode <= EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_HARMAN) {
                return eqMode;
            }
        }

        return EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE_DEFAULT;
    }

    public boolean setEqualizerMode(byte eqMode) throws IOException {
        if (DEBUG) Log.d(TAG, "setEqualizerMode");

        return setDeviceConfig(EarbudsConstants.XIAOMI_MMA_CONFIG_EQUALIZER_MODE, new byte[]{eqMode});
    }

    @Nullable
    public String getDeviceSN() throws IOException {
        if (DEBUG) Log.d(TAG, "getDeviceSN");

        byte[] snBytes = getDeviceConfig(EarbudsConstants.XIAOMI_MMA_CONFIG_SN, 20);
        if (snBytes == null) {
            return null;
        }

        String snString;
        try {
            snString = new String(snBytes, StandardCharsets.US_ASCII);
        } catch (Exception ignored) {
            snString = null;
        }
        if (DEBUG) Log.d(TAG, "getDeviceSN: " + snString);
        return snString;
    }

    public byte getNoiseCancellationMode() throws IOException {
        if (DEBUG) Log.d(TAG, "getNoiseCancellationMode");

        byte[] noiseCancellationModeBytes = getDeviceConfig(
                EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE, 2);
        if (noiseCancellationModeBytes != null) {
            byte noiseCancellationMode = noiseCancellationModeBytes[0];
            if (DEBUG) Log.d(TAG, "getNoiseCancellationMode " + noiseCancellationMode);

            if (noiseCancellationMode <= EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_TRANSPARENCY) {
                return noiseCancellationMode;
            }
        }

        return EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE_OFF;
    }

    public boolean setNoiseCancellationMode(byte mode) throws IOException {
        if (DEBUG) Log.d(TAG, "setNoiseCancellationMode");

        return setDeviceConfig(
                EarbudsConstants.XIAOMI_MMA_CONFIG_NOISE_CANCELLATION_MODE, new byte[]{mode, 0x00});
    }

    @NonNull
    private static byte[] getResponsePacket(@NonNull InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean startFound = false;

        while (true) {
            byte current = inputStream.readNBytes(1)[0];

            // Check for end sequence EF
            if (startFound && current == (byte) 0xEF) {
                return buffer.toByteArray();
            }

            buffer.write(current);

            // Check for start sequence FE DC BA
            if (!startFound && buffer.size() >= 3) {
                byte[] data = buffer.toByteArray();
                if (data[data.length - 3] == (byte) 0xFE
                        && data[data.length - 2] == (byte) 0xDC
                        && data[data.length - 1] == (byte) 0xBA) {
                    startFound = true;
                    buffer.reset();
                }
            }
        }
    }

    @SuppressWarnings("all")
    public void connect() throws IOException {
        if (DEBUG) Log.d(TAG, "connect");
        if (isConnected()) return;

        if (!device.isConnected()) {
            throw new IOException("device not connected");
        }

        // cancel discovery before connect
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        BluetoothSocket socket;
        try {
            this.socket = socket = device.createInsecureRfcommSocketToServiceRecord(
                    EarbudsConstants.UUID_XIAOMI_FAST_CONNECT.getUuid());
            socket.connect();
        } catch (IOException e) {
            Log.e(TAG, "connect: ", e);
            throw e;
        }
    }

    public void close() throws IOException {
        if (!isConnected()) return;
        if (DEBUG) Log.d(TAG, "close");

        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "close: ", e);
            throw e;
        } finally {
            socket = null;
        }
    }

}
