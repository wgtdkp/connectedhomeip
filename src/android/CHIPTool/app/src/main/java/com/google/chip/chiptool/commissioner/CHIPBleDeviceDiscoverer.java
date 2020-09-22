package com.google.chip.chiptool.commissioner;

import android.Manifest.permission;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CHIPBleDeviceDiscoverer {

  private static final String TAG = CHIPBleDeviceDiscoverer.class.getSimpleName();

  private static final ParcelUuid UUID_CHIP_SERVICE = ParcelUuid.fromString("0000affe-0000-1000-8000-00805f9b34fb");

  private boolean isScanning = false;

  private BluetoothManager bluetoothManager;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothLeScanner bleScanner;

  private CHIPBleDeviceListener bleDeviceListener;

  public interface CHIPBleDeviceListener {
    void onBleDeviceFound(CHIPBleDeviceInfo bleDevice);
    void onBleDeviceLost(CHIPBleDeviceInfo bleDevice);
  }

  private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      super.onScanResult(callbackType, result);

      byte[] serviceData = result.getScanRecord().getServiceData(UUID_CHIP_SERVICE);
      if (serviceData == null) {
        Log.e(TAG, "didn't filter out non-CHIP devices");
        return;
      }

      String name = result.getDevice().getName();
      String addr = result.getDevice().getAddress();

      Log.d(TAG, String.format("found new CHIP BLE device: %s, address=%s", name, addr));

      bleDeviceListener.onBleDeviceFound(new CHIPBleDeviceInfo(name, addr, CHIPBleServiceData.fromByteArray(serviceData)));
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      super.onBatchScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {
      super.onScanFailed(errorCode);

      Log.e(TAG, String.format("failed to scan CHIP BLE devices: %d", errorCode));
    }
  };

  public CHIPBleDeviceDiscoverer(@NonNull Context context, @NonNull CHIPBleDeviceListener bleDeviceListener) {
    this.bleDeviceListener = bleDeviceListener;

    bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothAdapter = bluetoothManager.getAdapter();
    bleScanner = bluetoothAdapter.getBluetoothLeScanner();
  }

  public void start() {
    if (isScanning) {
      Log.w(TAG, "the BLE discoverer is already running!");
      return;
    }

    isScanning = true;

    ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
    filterBuilder.setServiceUuid(UUID_CHIP_SERVICE);

    ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
    settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

    // TODO: check if we have location permission and request for it if not.
    bleScanner.startScan(Collections.singletonList(filterBuilder.build()), settingsBuilder.build(), scanCallback);
  }

  public void stop() {
    if (!isScanning) {
      Log.w(TAG, "the BLE discoverer has already been stopped!");
      return;
    }

    isScanning = false;
    bleScanner.stopScan(scanCallback);
  }
}
