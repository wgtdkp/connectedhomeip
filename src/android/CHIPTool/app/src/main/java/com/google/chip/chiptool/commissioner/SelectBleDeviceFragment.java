package com.google.chip.chiptool.commissioner;

import android.app.Activity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.chip.chiptool.R;
import com.google.chip.chiptool.setuppayloadscanner.BarcodeFragment;

/**
 * A fragment representing a list of Items.
 */
public class SelectBleDeviceFragment extends Fragment implements View.OnClickListener {

  private static final String TAG = SelectBleDeviceFragment.class.getSimpleName();

  private Button commissionButton;

  private CHIPBleDeviceAdapter bleDeviceAdapter;
  private CHIPBleDeviceDiscoverer bleDeviceDiscoverer;

  private CHIPBleDeviceInfo selectedBleDevice;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
   * screen orientation changes).
   */
  public SelectBleDeviceFragment() {
  }

  /*
  // TODO: Customize parameter initialization
  @SuppressWarnings("unused")
  public static SelectBleDeviceFragment newInstance(int columnCount) {
    SelectBleDeviceFragment fragment = new SelectBleDeviceFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_COLUMN_COUNT, columnCount);
    fragment.setArguments(args);
    return fragment;
  }
  */

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    bleDeviceAdapter = new CHIPBleDeviceAdapter(getContext());

    // TODO: initialize BLE device discoverer.
    bleDeviceDiscoverer = new CHIPBleDeviceDiscoverer(getContext(), bleDeviceAdapter);
    bleDeviceDiscoverer.start();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    bleDeviceDiscoverer.stop();

    CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
    commissionerActivity.finishCommissioning(Activity.RESULT_CANCELED);
  }

  @Override
  public void onPause() {
    super.onPause();

    bleDeviceDiscoverer.stop();
  }

  @Override
  public void onResume() {
    super.onResume();

    bleDeviceDiscoverer.start();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.commissioner_select_ble_device_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    commissionButton = view.findViewById(R.id.commission_ble_device_button);
    commissionButton.setOnClickListener(this);
    commissionButton.setVisibility(View.GONE);

    final ListView bleDeviceListView = view.findViewById(R.id.ble_devices);
    bleDeviceListView.setAdapter(bleDeviceAdapter);
    bleDeviceListView.setOnItemClickListener((AdapterView<?> adapterView, View v, int position, long id) -> {
      selectedBleDevice = (CHIPBleDeviceInfo) adapterView.getItemAtPosition(position);
      commissionButton.setVisibility(View.VISIBLE);
    });
  }

  @Override
  public void onClick(View view) {
    CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
    commissionerActivity.setJoinerBleDeviceAddr(selectedBleDevice.macAddr);
    commissionerActivity.showFragment(new BarcodeFragment());
  }
}