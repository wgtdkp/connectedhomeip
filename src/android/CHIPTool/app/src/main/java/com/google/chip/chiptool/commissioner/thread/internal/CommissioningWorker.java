package com.google.chip.chiptool.commissioner.thread.internal;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.openthread.commissioner.Error;
import io.openthread.commissioner.ErrorCode;

public class CommissioningWorker extends Worker {

  public CommissioningWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);

    // TODO(wgtdkp):
  }

  @NonNull
  @Override
  public Result doWork() {
    // TODO(wgtdkp):
    return errorToResult(new Error(ErrorCode.kNone, ""));
  }

  private Result errorToResult(Error error) {
    Data.Builder dataBuilder = new Data.Builder();

    if (error.getCode() == ErrorCode.kNone) {
      dataBuilder.putString(Constants.KEY_COMMISSIONING_STATUS, "commission device success!");
      dataBuilder.putBoolean(Constants.KEY_SUCCESS, true);
      return Result.success(dataBuilder.build());
    } else {
      dataBuilder.putString(Constants.KEY_COMMISSIONING_STATUS, error.toString());
      dataBuilder.putBoolean(Constants.KEY_SUCCESS, false);
      return Result.failure(StateToData(error.toString()));
    }
  }

  private Data StateToData(String state) {
    return new Data.Builder().putString(Constants.KEY_COMMISSIONING_STATUS, state).build();
  }
}
