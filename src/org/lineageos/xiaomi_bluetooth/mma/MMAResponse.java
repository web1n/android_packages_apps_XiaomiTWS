package org.lineageos.xiaomi_bluetooth.mma;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;


public class MMAResponse {

    public static String TAG = MMAResponse.class.getName();
    public static boolean DEBUG = true;

    public final byte opCode;
    public final byte opCodeSN;
    public final byte[] data;

    protected MMAResponse(byte opCode, byte opCodeSN, byte[] data) {
        this.opCode = opCode;
        this.opCodeSN = opCodeSN;
        this.data = data;
    }

    @NonNull
    @Override
    public String toString() {
        return "MMAResponse{" +
                "opCode=" + opCode +
                ", opCodeSN=" + opCodeSN +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    @Nullable
    public static MMAResponse fromPacket(byte[] packet) {
        if (packet == null || packet.length < 6) {
            if (DEBUG) Log.d(TAG, "fromPacket: packet length < 6");
            return null;
        }

        int type = (packet[0] & 0x80) == 0 ? 0 : 1;
        int responseFlag = (packet[0] & 0x40) == 0 ? 0 : 1;
        byte opCode = packet[1];
        int parameterLength = ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        byte status = packet[4];
        byte opCodeSN = packet[5];

        if (type != 0 || responseFlag != 0) {
            if (DEBUG) Log.d(TAG, "fromPacket: type or responseFlag not 0");
//            return null;
        }
        if (parameterLength < 2 || packet.length - 4 != parameterLength) {
            if (DEBUG) Log.d(TAG, "fromPacket: parameterLength not equal");
            return null;
        }
        if (status != 0) {
            if (DEBUG) Log.d(TAG, "fromPacket: status not success");
        }

        byte[] data = new byte[parameterLength - 2];
        System.arraycopy(packet, 6, data, 0, parameterLength - 2);

        if (DEBUG) Log.d(TAG, "fromPacket: opCode " + opCode + " opCodeSN " + opCodeSN);
        return new MMAResponse(opCode, opCodeSN, data);
    }

}
