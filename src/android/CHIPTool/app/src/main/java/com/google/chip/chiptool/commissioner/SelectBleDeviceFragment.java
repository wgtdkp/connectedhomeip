package com.google.chip.chiptool.commissioner;

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

  private static final int REQUEST_CODE_LOCATION_PERMISSION = 9527;

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


    if (hasLocationPermission()) {
      bleDeviceDiscoverer.start();
    } else {
      requestLocationPermission();
    }
  }

  @Override
  public void onClick(View view) {
    CommissionerActivity commissionerActivity = (CommissionerActivity) getActivity();
    commissionerActivity.setJoinerBleDeviceAddr(selectedBleDevice.macAddr);
    commissionerActivity.showFragment(new BarcodeFragment());
  }

  private boolean hasLocationPermission() {
    return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getContext(),
        permission.ACCESS_FINE_LOCATION);
  }

  private void requestLocationPermission() {
    requestPermissions(new String[] {permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
  }

  private void showLocationPermissionAlert() {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.commissioner_location_permission_missing_alert_title)
        .setMessage(R.string.commissioner_location_permission_missing_alert_message)
        .setPositiveButton(R.string.commissioner_location_permission_missing_alert_try_again, (diag, which) -> {
          requestLocationPermission();
        }).setCancelable(false)
        .create().show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        bleDeviceDiscoverer.start();
      } else {
        showLocationPermissionAlert();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }
}