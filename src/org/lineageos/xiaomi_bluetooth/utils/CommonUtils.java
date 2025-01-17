package org.lineageos.xiaomi_bluetooth.utils;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
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

    public static <T> T executeWithTimeout(@NonNull Callable<T> task, int timeout_ms) throws TimeoutException {
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

}
