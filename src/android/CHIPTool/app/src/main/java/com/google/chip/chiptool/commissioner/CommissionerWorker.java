package com.google.chip.chiptool.commissioner;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo;

public class CommissionerWorker extends Worker {

    private static final String TAG = CommissionerWorker.class.getSimpleName();

    private CHIPDeviceInfo deviceInfo;
    private NetworkInfo networkInfo;

    public CommissionerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        setProgressAsync(new Data.Builder().putString(Constants.KEY_COMMISSIONING_STATUS, "commissioning...").build());

        try {
            Thread.sleep(1000 * 5);
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted");
        }

        Data result = new Data.Builder().putString(Constants.KEY_COMMISSIONING_STATUS, "commission device success!").build();
        return Result.success(result);
    }
}
