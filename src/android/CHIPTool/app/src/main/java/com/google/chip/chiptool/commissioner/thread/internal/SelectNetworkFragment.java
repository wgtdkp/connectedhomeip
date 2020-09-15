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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.google.chip.chiptool.R;
import com.google.chip.chiptool.commissioner.CommissionerActivity;
import com.google.chip.chiptool.commissioner.thread.BorderAgentInfo;
import com.google.chip.chiptool.commissioner.thread.CommissionerUtils;
import com.google.chip.chiptool.commissioner.thread.ThreadCommissionerService;
import com.google.chip.chiptool.commissioner.thread.ThreadNetworkCredential;
import com.google.chip.chiptool.commissioner.thread.ThreadNetworkInfo;
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo;
import io.openthread.commissioner.ByteArray;
import io.openthread.commissioner.Commissioner;
import io.openthread.commissioner.Error;
import io.openthread.commissioner.ErrorCode;
import java.util.concurrent.ExecutionException;

public class SelectNetworkFragment extends Fragment implements InputNetworkPasswordDialogFragment.PasswordDialogListener, FetchCredentialDialogFragment.CredentialListener, View.OnClickListener {

  private static final String TAG = CommissioningFragment.class.getSimpleName();

  private CHIPDeviceInfo deviceInfo;

  private NetworkAdapter networksAdapter;

  private ThreadNetworkInfoHolder selectedNetwork;
  private byte[] userInputPskc;
  private Button addDeviceButton;

  private String joinerBleDeviceAddr;

  private BorderAgentDiscoverer borderAgentDiscoverer;

  public SelectNetworkFragment() {

  }

  public SelectNetworkFragment(CHIPDeviceInfo deviceInfo) {
    this.deviceInfo = deviceInfo;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    networksAdapter = new NetworkAdapter(getContext());
    borderAgentDiscoverer = new BorderAgentDiscoverer(getContext(), networksAdapter);
    borderAgentDiscoverer.start();

    CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
    joinerBleDeviceAddr = commissionerActivity.getJoinerBleDeviceAddr();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    borderAgentDiscoverer.stop();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.commissioner_select_network_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Hide the button
    addDeviceButton = view.findViewById(R.id.add_device_button);
    addDeviceButton.setVisibility(View.GONE);

    String deviceInfoString =
        String.format(
            "version: %d\nvendorId: %d\nproductId: %d\nsetupPinCode: %d",
            deviceInfo.getVersion(),
            deviceInfo.getVendorId(),
            deviceInfo.getProductId(),
            deviceInfo.getSetupPinCode());
    TextView deviceInfoView = view.findViewById(R.id.device_info);
    deviceInfoView.setText(deviceInfoString);

    final ListView networkListView = view.findViewById(R.id.networks);
    networkListView.setAdapter(networksAdapter);

    networkListView.setOnItemClickListener(
      (AdapterView<?> adapterView, View v, int position, long id) -> {
        selectedNetwork = (ThreadNetworkInfoHolder) adapterView.getItemAtPosition(position);
        addDeviceButton.setVisibility(View.VISIBLE);
      });

    view.findViewById(R.id.add_device_button).setOnClickListener(this);
  }

  private void gotoCommissioning(@NonNull String peerBleAddr, @NonNull ThreadNetworkCredential credential) {
    CommissioningFragment fragment = new CommissioningFragment(peerBleAddr, credential);
    getParentFragmentManager()
        .beginTransaction()
        .replace(R.id.commissioner_service_activity, fragment, fragment.getClass().getSimpleName())
        .addToBackStack(null)
        .commit();
  }

  // Click listeners for network password dialog.

  @Override
  public void onPositiveClick(InputNetworkPasswordDialogFragment fragment, String password) {
    BorderAgentInfo selectedBorderAgent = selectedNetwork.borderAgents.get(0);
    userInputPskc = computePskc(selectedNetwork.networkInfo, password);
    gotoFetchingCredential(selectedBorderAgent, userInputPskc);
  }

  @Override
  public void onNegativeClick(DialogFragment dialog) {
    CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
    commissionerActivity.finishCommissioning(Activity.RESULT_CANCELED);
  }

  private byte[] computePskc(ThreadNetworkInfo threadNetworkInfo, String password) {
    short[] extendedPanId = new short[threadNetworkInfo.extendedPanId.length];
    for (int i = 0; i < extendedPanId.length; ++i) {
      extendedPanId[i] = (short)(((short) threadNetworkInfo.extendedPanId[i]) & 0xff);
    }

    ByteArray pskc = new ByteArray();
    Error error = Commissioner.generatePSKc(pskc, password, threadNetworkInfo.networkName, new ByteArray(extendedPanId));
    if (error.getCode() != ErrorCode.kNone) {
      Log.e(TAG, String.format("failed to generate PSKc: %s", error.toString()));
    }

    return CommissionerUtils.getByteArray(pskc);
  }

  private void gotoFetchingCredential(BorderAgentInfo borderAgentInfo, byte[] pskc) {
    new FetchCredentialDialogFragment(borderAgentInfo, pskc, SelectNetworkFragment.this)
        .show(getParentFragmentManager(), FetchCredentialDialogFragment.class.getSimpleName());
  }

  @Override
  public void onClick(View view) {
    try {
      BorderAgentInfo selectedBorderAgent = selectedNetwork.borderAgents.get(0);
      ThreadCommissionerServiceImpl commissionerService = new ThreadCommissionerServiceImpl(getContext());
      BorderAgentRecord borderAgentRecord = commissionerService.getBorderAgentRecord(selectedBorderAgent).get();  // NetworkCredentialDatabase.getDatabase(getContext()).getNetworkCredential(selectedNetwork);

      if (borderAgentRecord != null && borderAgentRecord.getActiveOperationalDataset() != null) {
        gotoCommissioning(joinerBleDeviceAddr, new ThreadNetworkCredential(borderAgentRecord.getActiveOperationalDataset()));
      } else if (borderAgentRecord != null && borderAgentRecord.getPskc() != null) {
        gotoFetchingCredential(selectedBorderAgent, borderAgentRecord.getPskc());
      } else {
        new InputNetworkPasswordDialogFragment(SelectNetworkFragment.this).show(
            getParentFragmentManager(), InputNetworkPasswordDialogFragment.class.getSimpleName());
      }
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onCancelClick(FetchCredentialDialogFragment fragment) {

  }

  @Override
  public void onConfirmClick(FetchCredentialDialogFragment fragment,
      ThreadNetworkCredential credential) {
    if (credential != null) {
      ThreadCommissionerServiceImpl commissionerService = new ThreadCommissionerServiceImpl(getContext());
      try {
        commissionerService.addThreadNetworkCredential(selectedNetwork.borderAgents.get(0), userInputPskc, credential).get();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      gotoCommissioning(joinerBleDeviceAddr, credential);
    } else {
      Log.w(TAG, "failed to fetch credentials");
    }
  }
}
