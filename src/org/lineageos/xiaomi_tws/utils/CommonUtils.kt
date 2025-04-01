package org.lineageos.xiaomi_tws.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object CommonUtils {

    fun <T> executeWithTimeout(timeoutMs: Long, task: Callable<T>): T {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit(task)

        try {
            return future[timeoutMs, TimeUnit.MILLISECONDS]
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw TimeoutException("Task timed out after $timeoutMs ms")
        } catch (e: InterruptedException) {
            future.cancel(true)
            throw InterruptedException("Task was interrupted").initCause(e)
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } finally {
            executor.shutdownNow()
        }
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            setData(Uri.fromParts("package", activity.packageName, null))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(intent)
    }

}
