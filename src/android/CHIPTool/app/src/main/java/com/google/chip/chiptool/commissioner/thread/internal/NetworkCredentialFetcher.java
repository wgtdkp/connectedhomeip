package com.google.chip.chiptool.commissioner.thread.internal;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.chip.chiptool.commissioner.thread.BorderAgentInfo;
import com.google.chip.chiptool.commissioner.thread.CommissionerUtils;
import com.google.chip.chiptool.commissioner.thread.ThreadCommissionerException;
import com.google.chip.chiptool.commissioner.thread.ThreadNetworkCredential;
import io.openthread.commissioner.ActiveOperationalDataset;
import io.openthread.commissioner.ByteArray;
import io.openthread.commissioner.ChannelMask;
import io.openthread.commissioner.Commissioner;
import io.openthread.commissioner.CommissionerHandler;
import io.openthread.commissioner.Config;
import io.openthread.commissioner.Error;
import io.openthread.commissioner.ErrorCode;
import io.openthread.commissioner.LogLevel;
import io.openthread.commissioner.Logger;
import java.net.InetAddress;

class NetworkCredentialFetcher {

  private static final String TAG = NetworkCredentialFetcher.class.getSimpleName();

  NativeCommissionerHandler nativeCommissionerHandler = new NativeCommissionerHandler();
  Commissioner nativeCommissioner;

  public ThreadNetworkCredential fetchNetworkCredential(@NonNull BorderAgentInfo borderAgentInfo, @NonNull byte[] pskc) throws ThreadCommissionerException {
    ActiveOperationalDataset activeOperationalDataset = fetchNetworkCredential(borderAgentInfo.host, borderAgentInfo.port, pskc);
    return ThreadNetworkCredential.fromActiveOperationalDataset(activeOperationalDataset);
  }

  public void cancel() {
    if (nativeCommissioner != null) {
      Log.d(TAG, "cancel requesting credential");
      nativeCommissioner.cancelRequests();
    }
  }

  private ActiveOperationalDataset fetchNetworkCredential(@NonNull InetAddress address, int port, @NonNull byte[] pskc) throws ThreadCommissionerException {
    nativeCommissioner = Commissioner.create(nativeCommissionerHandler);

    Config config = new Config();
    config.setId("TestComm");
    config.setDomainName("TestDomain");
    config.setEnableCcm(false);
    config.setEnableDtlsDebugLogging(true);
    config.setPSKc(CommissionerUtils.getByteArray(pskc));
    config.setLogger(new NativeCommissionerLogger());

    try {
      // Initialize the native commissioner
      throwIfFail(nativeCommissioner.init(config));

      // Petition to be the active commissioner in the Thread Network.
      String[] existingCommissionerId = new String[1];
      throwIfFail(
          nativeCommissioner.petition(existingCommissionerId, address.getHostAddress(), port));

      // Fetch Active Operational Dataset.
      ActiveOperationalDataset activeOperationalDataset = new ActiveOperationalDataset();
      throwIfFail(nativeCommissioner.getActiveDataset(activeOperationalDataset, 0xFFFF));
      nativeCommissioner.resign();
      nativeCommissioner = null;
      return activeOperationalDataset;
    } catch (ThreadCommissionerException e) {
      nativeCommissioner.resign();
      nativeCommissioner = null;
      throw e;
    }
  }

  private void throwIfFail(Error error) throws ThreadCommissionerException {
    if (error.getCode() != ErrorCode.kNone) {
      throw new ThreadCommissionerException(error.getCode().swigValue(), error.getMessage());
    }
  }
}

class NativeCommissionerLogger extends Logger {
  private static final String TAG = "NativeCommissioner";

  @Override
  public void log(LogLevel level, String region, String msg) {
    Log.d(TAG, String.format("[ %s ]: %s", region, msg));
  }
}

class NativeCommissionerHandler extends CommissionerHandler {
  private static final String TAG = NativeCommissionerHandler.class.getSimpleName();

  @Override
  public String onJoinerRequest(ByteArray joinerId) {
    Log.d(TAG, "A joiner is requesting commissioning");
    return "";
  }

  @Override
  public void onJoinerConnected(ByteArray joinerId, Error error) {
    Log.d(TAG, "A joiner is connected");
  }

  @Override
  public boolean onJoinerFinalize(
      ByteArray joinerId,
      String vendorName,
      String vendorModel,
      String vendorSwVersion,
      ByteArray vendorStackVersion,
      String provisioningUrl,
      ByteArray vendorData) {
    Log.d(TAG, "A joiner is finalizing");
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
