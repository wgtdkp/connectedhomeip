package com.google.chip.chiptool.commissioner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.google.chip.chiptool.R;
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo;
import com.google.gson.Gson;

public class CommissioningFragment extends Fragment implements Observer<WorkInfo> {

  private static final String TAG = CommissioningFragment.class.getSimpleName();

  private CHIPDeviceInfo deviceInfo;
  private NetworkInfo networkInfo;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState
  ) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.commissioner_commissioning_fragment, container, false);
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ProgressBar progressBar = getActivity().findViewById(R.id.commissioning_progress);
    progressBar.setMin(0);
    progressBar.setMax(100);

    deviceInfo = getArguments().getParcelable(Constants.KEY_DEVICE_INFO);
    networkInfo = getArguments().getParcelable(Constants.KEY_NETWORK_INFO);

    Data arguments = new Data.Builder()
            .putString(Constants.KEY_DEVICE_INFO, new Gson().toJson(deviceInfo))
            .putString(Constants.KEY_DEVICE_INFO, new Gson().toJson(networkInfo))
            .build();
    WorkRequest commssionerWorkRequest = new OneTimeWorkRequest.Builder(CommissionerWorker.class).setInputData(arguments).build();

    WorkManager.getInstance(getActivity()).enqueue(commssionerWorkRequest);

    WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(commssionerWorkRequest.getId())
            .observe(getViewLifecycleOwner(), this);

    view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        CommissionerActivity commissionerActivity = (CommissionerActivity)getActivity();
        commissionerActivity.finishCommissioning(Activity.RESULT_CANCELED);
        //NavHostFragment.findNavController(SelectNetworkFragment.this)
        //    .navigate(R.id.action_select_network_to_commissioning);
      }
    });

    view.findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        CommissionerActivity serviceActivity = (CommissionerActivity)getActivity();
        serviceActivity.finishCommissioning(Activity.RESULT_OK);
      }
    });
  }

  @Override
  public void onChanged(@Nullable WorkInfo workInfo) {
    TextView statusText = getActivity().findViewById(R.id.status_text);
    ProgressBar progressBar = getActivity().findViewById(R.id.commissioning_progress);

    if (workInfo != null) {
      if (workInfo.getState().isFinished()) {
        showCommissionDone(workInfo.getOutputData().getString(Constants.KEY_COMMISSIONING_STATUS));
      } else {
        showInProgress(workInfo.getProgress().getString(Constants.KEY_COMMISSIONING_STATUS));
      }
    }
  }

  private void showInProgress(String status) {
    TextView statusText = getActivity().findViewById(R.id.status_text);
    if (status != null) {
      statusText.setText(status);
    }

    ProgressBar progressBar = getActivity().findViewById(R.id.commissioning_progress);
    progressBar.setVisibility(View.VISIBLE);

    Button cancelButton = getActivity().findViewById(R.id.cancel_button);
    cancelButton.setVisibility(View.VISIBLE);

    ImageView doneImage = getActivity().findViewById(R.id.done_image);
    doneImage.setVisibility(View.GONE);

    Button doneButton = getActivity().findViewById(R.id.done_button);
    doneButton.setVisibility(View.GONE);
  }

  private void showCommissionDone(String status) {
    TextView statusText = getActivity().findViewById(R.id.status_text);
    if (status != null) {
      statusText.setText(status);
    }

    ProgressBar progressBar = getActivity().findViewById(R.id.commissioning_progress);
    progressBar.setVisibility(View.GONE);

    Button cancelButton = getActivity().findViewById(R.id.cancel_button);
    cancelButton.setVisibility(View.GONE);

    ImageView doneImage = getActivity().findViewById(R.id.done_image);
    doneImage.setVisibility(View.VISIBLE);

    Button doneButton = getActivity().findViewById(R.id.done_button);
    doneButton.setVisibility(View.VISIBLE);
  }
}
