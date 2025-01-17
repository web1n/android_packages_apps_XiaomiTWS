package org.lineageos.xiaomi_bluetooth.utils;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.List;

public class CommonUtils {

    @NonNull
    public static byte[] byteListToArray(@NonNull List<Byte> byteList) {
        ByteBuffer buffer = ByteBuffer.allocate(byteList.size());
        for (Byte b : byteList) {
            buffer.put(b);
        }

        return buffer.array();
    }

}
