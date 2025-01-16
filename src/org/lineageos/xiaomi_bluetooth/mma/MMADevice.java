package org.lineageos.xiaomi_bluetooth.mma;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;
import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MMADevice implements AutoCloseable {

    public static String TAG = MMADevice.class.getName();
    public static boolean DEBUG = true;

    private final BluetoothDevice device;

    private BluetoothSocket socket;
    private byte opCodeSN = 0;

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
        byte opCodeSN = this.opCodeSN;
        this.opCodeSN += 1;
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

        String version = Integer.toHexString(((data[2] & 0xFF) << 8) | (data[3] & 0xFF));

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

        String primaryVersion = Integer.toHexString(((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
        String secondaryVersion = Integer.toHexString(((data[4] & 0xFF) << 8) | (data[5] & 0xFF));

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
