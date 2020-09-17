/*
 *   Copyright (c) 2020 Project CHIP Authors
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.google.chip.chiptool.commissioner.thread.internal;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.WorkInfo;
import com.google.chip.chiptool.R;
import com.google.chip.chiptool.commissioner.CommissionerActivity;
import com.google.chip.chiptool.commissioner.thread.CommissionerUtils;
import com.google.chip.chiptool.commissioner.thread.ThreadNetworkCredential;
import com.google.gson.Gson;
import io.openthread.ip6oble.Ip6oBleService;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommissioningFragment extends Fragment implements Observer<WorkInfo> {

  private static final String TAG = CommissioningFragment.class.getSimpleName();

  private static final int REQUEST_CODE_START_IP6OBLE = 0xB004;

  private static final int TEST_REMOTE_DEVICE_PORT = 11095;
  private static final int TEST_COMMISSIONING_PORT = 11096;

  // How long we show the "IP link established..." message.
  private static final int IP_LINK_ESTABLISHED_DISPLAY_TIME = 2000; // In Milliseconds.

  // How long we show the "Installing credentail..." message.
  private static final int INSTALLING_CREDENTIAL_DISPLAY_TIME = 2000;

  private String joinerBleDeviceAddr;
  private ThreadNetworkCredential networkCredential;

  CommissionerActivity commissionerActivity;

  ExecutorService executor = Executors.newSingleThreadExecutor();

  //WorkRequest commssionerWorkRequest;

  TextView statusText;
  ProgressBar progressBar;
  Button cancelButton;
  Button doneButton;
  ImageView doneImage;
  ImageView errorImage;

  private String joinerIp6Addr;

  private BroadcastReceiver ip6oBleServiceReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent == null) {
        return;
      }

      if (intent.getAction().equals(Ip6oBleService.EVENT_6OBLE_CONNECTED)) {
        onConnected(intent.getStringExtra(Ip6oBleService.KEY_LOCAL_IP6_ADDR), intent.getStringExtra(
            Ip6oBleService.KEY_PEER_IP6_ADDR));
      } else if (intent.getAction().equals(Ip6oBleService.EVENT_6OBLE_DISCONNECTED)) {
        onDisconnected();
      } else {
        Log.w(TAG, "unexpected broadcast event: " + intent.getAction());
      }
    }
  };

  public CommissioningFragment(@NonNull ThreadNetworkCredential networkCredential) {
    this.networkCredential = networkCredential;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.commissioner_commissioning_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    commissionerActivity = (CommissionerActivity) getActivity();
    joinerBleDeviceAddr = commissionerActivity.getJoinerBleDeviceAddr();

    cancelButton = view.findViewById(R.id.cancel_button);
    doneButton = view.findViewById(R.id.done_button);
    doneImage = view.findViewById(R.id.done_image);
    errorImage = view.findViewById(R.id.error_image);
    statusText = view.findViewById(R.id.status_text);
    progressBar = view.findViewById(R.id.commissioning_progress);
    progressBar.setMin(0);
    progressBar.setMax(100);

    Data arguments =
        new Data.Builder()
            .putString(Constants.KEY_JOINER_BLE_DEVICE_ADDR, new Gson().toJson(joinerBleDeviceAddr))
            .putString(Constants.KEY_NETWORK_CREDENTIAL, new Gson().toJson(networkCredential))
            .build();

    /*
    commssionerWorkRequest = new OneTimeWorkRequest.Builder(CommissioningWorker.class).setInputData(arguments).build();

    WorkManager.getInstance(getActivity()).enqueue(commssionerWorkRequest);

    WorkManager.getInstance(getActivity())
        .getWorkInfoByIdLiveData(commssionerWorkRequest.getId())
        .observe(getViewLifecycleOwner(), this);

    view.findViewById(R.id.cancel_button)
        .setOnClickListener(
            v -> {
              WorkManager.getInstance(getActivity())
                  .cancelWorkById(commssionerWorkRequest.getId());

              CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
              commissionerActivity.finishCommissioning(Activity.RESULT_CANCELED);
            });

    view.findViewById(R.id.done_button)
        .setOnClickListener(
            v -> {
              CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
              commissionerActivity.finishCommissioning(Activity.RESULT_OK);
            });

     */

    view.findViewById(R.id.cancel_button).setOnClickListener(
        v -> {
          stopCommissioning();
          commissionerActivity.finishCommissioning(Activity.RESULT_CANCELED);
        });

    view.findViewById(R.id.done_button).setOnClickListener(
        v -> {
          commissionerActivity.finishCommissioning(Activity.RESULT_OK);
        });

    startCommissioning();
  }

  @Override
  public void onChanged(@Nullable WorkInfo workInfo) {
    if (workInfo != null) {
      if (workInfo.getState().isFinished()) {

        showCommissionDone(
            workInfo.getOutputData().getBoolean(Constants.KEY_SUCCESS, false),
            workInfo.getOutputData().getString(Constants.KEY_COMMISSIONING_STATUS));
      } else {
        showInProgress(workInfo.getProgress().getString(Constants.KEY_COMMISSIONING_STATUS));
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CODE_START_IP6OBLE) {
      if (resultCode == Activity.RESULT_OK) {
        Intent intent = new Intent(getContext(), Ip6oBleService.class);
        intent.setAction(Ip6oBleService.ACTION_START);
        intent.putExtra(Ip6oBleService.KEY_PEER_BLE_ADDR, joinerBleDeviceAddr);
        getActivity().startService(intent);
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    stopCommissioning();
  }

  @Override
  public void onResume() {
    super.onResume();
    LocalBroadcastManager.getInstance(getContext()).registerReceiver(ip6oBleServiceReceiver, new IntentFilter(
        Ip6oBleService.EVENT_6OBLE_CONNECTED));
  }

  @Override
  public void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(ip6oBleServiceReceiver);
  }

  private void startCommissioning() {
    startIp6oBleService();
  }

  private void stopCommissioning() {
    stopIp6oBleService();
  }

  private void startIp6oBleService() {
    // Start IP6oBLE service.
    Intent intent = VpnService.prepare(getContext());
    if (intent != null) {
      startActivityForResult(intent, REQUEST_CODE_START_IP6OBLE);
    } else {
      onActivityResult(REQUEST_CODE_START_IP6OBLE, Activity.RESULT_OK, null);
    }
  }

  private void stopIp6oBleService() {
    Intent intent = new Intent(getContext(), Ip6oBleService.class);
    intent.setAction(Ip6oBleService.ACTION_STOP);
    getActivity().startService(intent);
  }

  private void onConnected(@Nullable String localIp6Addr, @Nullable String peerIp6Addr) {
    Log.d(TAG, "::onConnected");

    if (localIp6Addr != null && peerIp6Addr != null) {
      joinerIp6Addr = peerIp6Addr;

      // TODO: send notification
      showInProgress(String.format("IP link established!\njoiner's IP: %s", joinerIp6Addr));

      installCredential();
    } else {
      showCommissionDone(false, "failed to establish IP link!");
    }
  }

  private void onDisconnected() {
    Log.d(TAG, "IP link disconnected!");
    joinerIp6Addr = null;
    showCommissionDone(false, "IP link is down!");
  }

  private void installCredential() {
    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
      DatagramSocket socket = null;
      try {

        Thread.sleep(2000);

        new Handler(Looper.getMainLooper()).post(() -> {
          showInProgress("installing network credential...");
        });

        socket = new DatagramSocket();
        //socket.connect( InetAddress.getByName(ThreadVpnService.LOCAL_ADDR), 6666);
        // We cannot use the address (2001:1983::de8) assigned to the TUN interface.
        socket.connect(InetAddress.getByName(joinerIp6Addr + "%tun0"), TEST_COMMISSIONING_PORT);
        DatagramPacket packet = new DatagramPacket(networkCredential.getActiveOperationalDataset(), networkCredential.getActiveOperationalDataset().length);

        Log.d(TAG, "installing network credential: " + CommissionerUtils.getHexString(networkCredential.getActiveOperationalDataset()));
        socket.send(packet);

        Thread.sleep(5000);

        return null;
      } catch (Exception e) {
        return e.toString();
      } finally {
        if (socket != null) {
          socket.close();
        }
      }
    }).thenAccept(failure -> {
      new Handler(Looper.getMainLooper()).post(() -> {
        showCommissionDone(failure == null, failure == null ? "success!" : "failed to install network credential!");
      });
    });
  }

  private void showInProgress(String status) {
    if (status != null) {
      statusText.setText(status);
    }

    progressBar.setVisibility(View.VISIBLE);

    cancelButton.setVisibility(View.VISIBLE);
    doneImage.setVisibility(View.GONE);
    errorImage.setVisibility(View.GONE);
    doneButton.setVisibility(View.GONE);
  }

  private void showCommissionDone(Boolean success, String status) {
    if (status != null) {
      statusText.setText(status);
    }

    progressBar.setVisibility(View.GONE);
    cancelButton.setVisibility(View.GONE);
    doneButton.setVisibility(View.VISIBLE);

    if (success) {
      doneImage.setVisibility(View.VISIBLE);
      errorImage.setVisibility(View.GONE);
    } else {
      doneImage.setVisibility(View.GONE);
      errorImage.setVisibility(View.VISIBLE);
    }
  }
}

