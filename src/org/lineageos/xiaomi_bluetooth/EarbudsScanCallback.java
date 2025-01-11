package org.lineageos.xiaomi_bluetooth;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;

import org.lineageos.xiaomi_bluetooth.earbuds.Earbuds;
import org.lineageos.xiaomi_bluetooth.utils.EarbudsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class EarbudsScanCallback extends ScanCallback {

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        List<Earbuds> earbudsStatusList = new ArrayList<>();

        for (ScanResult result : results) {
            Earbuds earbuds = EarbudsUtils.parseScanResult(result);

            if (earbuds != null) earbudsStatusList.add(earbuds);
        }

        if (!earbudsStatusList.isEmpty()) {
            onEarbudsScanResult(earbudsStatusList.get(earbudsStatusList.size() - 1));
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        onBatchScanResults(Collections.singletonList(result));
    }

    public abstract void onEarbudsScanResult(@NonNull Earbuds earbuds);

}
