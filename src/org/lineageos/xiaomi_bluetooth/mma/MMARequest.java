package org.lineageos.xiaomi_bluetooth.mma;

import androidx.annotation.NonNull;

import java.util.Arrays;


public class MMARequest {

//    public static String TAG = MMARequest.class.getName();
//    public static boolean DEBUG = true;

    public final byte opCode;
    public final byte opCodeSN;
    public final byte[] data;
    public final boolean needReceive;

    public MMARequest(byte opCode, byte opCodeSN, byte[] data, boolean needReceive) {
        this.opCode = opCode;
        this.opCodeSN = opCodeSN;
        this.data = data;
        this.needReceive = needReceive;
    }

    @NonNull
    @Override
    public String toString() {
        return "MMARequest{" +
                "opCode=" + opCode +
                ", opCodeSN=" + opCodeSN +
                ", data=" + Arrays.toString(data) +
                ", needReceive=" + needReceive +
                '}';
    }

    @NonNull
    public byte[] toBytes() {
        byte[] dataToSend = new byte[5 + data.length + 4];

        // header
        dataToSend[0] = (byte) 0xFE;
        dataToSend[1] = (byte) 0xDC;
        dataToSend[2] = (byte) 0xBA;

        dataToSend[3] = (byte) (needReceive ? 0b1_1_000_000 : 0b1_0_000_000);
        dataToSend[4] = opCode;

        int length = data.length + 1; // add opCodeSN
        dataToSend[5] = (byte) ((length >> 8) & 0xFF);
        dataToSend[6] = (byte) (length & 0xFF);

        dataToSend[7] = opCodeSN;
        System.arraycopy(data, 0, dataToSend, 8, data.length);

        // footer
        dataToSend[dataToSend.length - 1] = (byte) 0xEF;
        return dataToSend;
    }

}
