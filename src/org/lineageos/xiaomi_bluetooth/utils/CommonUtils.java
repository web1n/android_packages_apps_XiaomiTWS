package org.lineageos.xiaomi_bluetooth.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import org.lineageos.xiaomi_bluetooth.EarbudsConstants;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class CommonUtils {

    @NonNull
    public static byte[] byteListToArray(@NonNull List<Byte> byteList) {
        ByteBuffer buffer = ByteBuffer.allocate(byteList.size());
        for (Byte b : byteList) {
            buffer.put(b);
        }

        return buffer.array();
    }

    public static <T> T executeWithTimeout(@NonNull Callable<T> task,
                                           int timeout_ms) throws TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);

        try {
            return future.get(timeout_ms, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            future.cancel(true);
            throw new TimeoutException();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    @NonNull
    public static String intToVersion(int version) {
        return String.join(".", Integer.toHexString(version).split(""));
    }

    @NonNull
    public static String bytesToHex(@NonNull byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    @NonNull
    public static byte[] hexToBytes(@NonNull String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hexString.charAt(i), 16) << 4;
            int low = Character.digit(hexString.charAt(i + 1), 16);
            bytes[i / 2] = (byte) (high | low);
        }
        return bytes;
    }

    @NonNull
    public static String[] getMissingRuntimePermissions(@NonNull Context context) {
        return Arrays.stream(EarbudsConstants.REQUIRED_RUNTIME_PERMISSIONS)
                .filter(permission -> context.checkSelfPermission(permission)
                        != PackageManager.PERMISSION_GRANTED)
                .toArray(String[]::new);
    }

}
