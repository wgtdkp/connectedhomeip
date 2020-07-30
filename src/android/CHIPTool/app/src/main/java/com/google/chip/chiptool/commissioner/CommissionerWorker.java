package com.google.chip.chiptool.commissioner;

import android.content.Context;
import android.net.Network;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo;

import com.google.gson.Gson;
import io.openthread.commissioner.Commissioner;
import io.openthread.commissioner.CommissionerDataset;
import io.openthread.commissioner.Config;
import io.openthread.commissioner.Error;
import io.openthread.commissioner.ByteArray;
import io.openthread.commissioner.ChannelMask;
import io.openthread.commissioner.CommissionerHandler;
import io.openthread.commissioner.ErrorCode;
import io.openthread.commissioner.Logger;
import io.openthread.commissioner.LogLevel;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

class NativeCommissionerLogger extends Logger {
    private static final String TAG = "NativeCommissioner";

    @Override
    public void log(LogLevel level, String region, String msg) {
        Log.d(TAG, String.format("[ %s ]: %s", region, msg));
    }
}

class NativeCommissionerHandler extends CommissionerHandler {
    private static final String TAG = "NativeCommissioner";

    private CommissionerWorker commissionerWorker;

    NativeCommissionerHandler(CommissionerWorker commissionerWorker) {
        this.commissionerWorker = commissionerWorker;
    }

    @Override
    public String onJoinerRequest(ByteArray joinerId) {
        Log.d(TAG, "A joiner is requesting commissioning");
        return commissionerWorker.getPskd();
    }

    @Override
    public void onJoinerConnected(ByteArray joinerId, Error error) {
        Log.d(TAG, "A joiner is connected");
    }

    @Override
    public boolean onJoinerFinalize(ByteArray joinerId, String vendorName, String vendorModel, String vendorSwVersion, ByteArray vendorStackVersion, String provisioningUrl, ByteArray vendorData) {
        Log.d(TAG, "A joiner is finalizing");
        commissionerWorker.onJoinerFinalize(joinerId);
        return true;
    }

    @Override
    public void onKeepAliveResponse(Error error) {
        Log.d(TAG, "received keep-alive response: " + error.toString());
    }

    @Override
    public void onPanIdConflict(String peerAddr, ChannelMask channelMask, int panId) {
        Log.d(TAG, "received PAN ID CONFLICT report");
    }

    @Override
    public void onEnergyReport(String aPeerAddr, ChannelMask aChannelMask, ByteArray aEnergyList) {
        Log.d(TAG, "received ENERGY SCAN report");
    }

    @Override
    public void onDatasetChanged() {
        Log.d(TAG, "Thread Network Dataset chanaged");
    }
}

public class CommissionerWorker extends Worker {

    private static final String TAG = CommissionerWorker.class.getSimpleName();

    private CHIPDeviceInfo deviceInfo;
    private NetworkInfo networkInfo;

    private boolean curJoinerCommissioned = false;

    public CommissionerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        deviceInfo = new Gson().fromJson(getInputData().getString(Constants.KEY_DEVICE_INFO), CHIPDeviceInfo.class);
        networkInfo = new Gson().fromJson(getInputData().getString(Constants.KEY_DEVICE_INFO), NetworkInfo.class);
    }

    String getPskd() {
        final int pskdLength = 27 / 5 + 1;
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < pskdLength - 1; ++i) {
            str.append("0123456789ABCDEFGHJKLMNPRSTUVWXY".charAt(((int)deviceInfo.getSetupPinCode() >> (i * 5)) & ((1 << 5) - 1)));
        }

        return str.toString();
    }

    private ByteArray getJoinerId() {
        int productId = deviceInfo.getProductId();
        return new ByteArray(new short[] {0x00, 0x00, 0x00, 0x00,
            (short)(productId >> 24),
            (short)(productId >> 16),
            (short)(productId >> 8),
            (short)(productId & 0xff)
            });
    }

    @NonNull
    @Override
    public Result doWork() {
        setProgressAsync(StateToData("commissioning..."));

        Commissioner nativeCommissioner = Commissioner.create(new NativeCommissionerHandler(this));
        ByteArray pskc = new ByteArray(new short[]{0x3a, 0xa5, 0x5f, 0x91, 0xca, 0x47, 0xd1, 0xe4, 0xe7, 0x1a, 0x08, 0xcb, 0x35, 0xe9, 0x15, 0x91});
        Config config = new Config();
        config.setId("TestComm");
        config.setDomainName("TestDomain");
        config.setEnableCcm(false);
        config.setPSKc(pskc);
        config.setLogger(new NativeCommissionerLogger());

        Error error = nativeCommissioner.init(config);
        if (error.getCode() != ErrorCode.kNone) {
            return errorToResult(error);
        }

        String[] existingCommissionerId = new String[1];
        error = nativeCommissioner.petition(existingCommissionerId, networkInfo.getHost().getHostAddress(), networkInfo.getPort());
        if (error.getCode() != ErrorCode.kNone) {
            return errorToResult(error);
        }

        if (!nativeCommissioner.isActive()) {
            return Result.failure(StateToData("the commissioner is not active"));
        }

        curJoinerCommissioned = false;

        ByteArray steeringData = new ByteArray();
        Commissioner.addJoiner(steeringData, getJoinerId());
        CommissionerDataset commDataset = new CommissionerDataset();
        commDataset.setSteeringData(steeringData);
        commDataset.setPresentFlags(commDataset.getPresentFlags() | CommissionerDataset.kSteeringDataBit);

        error = nativeCommissioner.setCommissionerDataset(commDataset);
        if (error.getCode() != ErrorCode.kNone) {
            return errorToResult(error);
        }

        for (int i = 0; i < 15; ++i) {
            if (curJoinerCommissioned) {
                break;
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                Log.d(TAG, "interrupted exception");
            }
        }

        if (!curJoinerCommissioned) {
            return Result.failure(StateToData("timeout"));
        }

        nativeCommissioner.resign();

        return Result.success(StateToData("commission device success!"));
    }

    private Result errorToResult(Error error) {
        if (error.getCode() == ErrorCode.kNone) {
            return Result.success(StateToData("commission device success!"));
        } else {
            return Result.failure(StateToData(error.toString()));
        }
    }

    private Data StateToData(String state) {
        return new Data.Builder().putString(Constants.KEY_COMMISSIONING_STATUS, state).build();
    }

    public void onJoinerFinalize(ByteArray joinerId) {

    }
}
