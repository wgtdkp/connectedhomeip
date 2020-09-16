package com.google.chip.chiptool.commissioner;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.BaseAdapter;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.chip.chiptool.R;

import com.google.chip.chiptool.commissioner.CHIPBleDeviceDiscoverer.CHIPBleDeviceListener;
import java.util.Vector;

/**
 * {@link RecyclerView.Adapter} that can display a {@link CHIPBleDeviceInfo}. TODO: Replace the
 * implementation with code for your data type.
 */
public class CHIPBleDeviceAdapter extends BaseAdapter implements CHIPBleDeviceListener {
  private Vector<CHIPBleDeviceInfo> bleDeviceInfos;

  private LayoutInflater inflater;

  CHIPBleDeviceAdapter(Context context) {
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    bleDeviceInfos = new Vector<>();
  }

  public void addBleDevice(CHIPBleDeviceInfo newBleDevice) {
    for (CHIPBleDeviceInfo bleDevice : bleDeviceInfos) {
      if (bleDevice.macAddr.equals(newBleDevice.macAddr)) {
        bleDeviceInfos.removeElement(bleDevice);
        break;
      }
    }

    bleDeviceInfos.add(newBleDevice);

    new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
  }

  public void removeBleDevice(CHIPBleDeviceInfo lostBleDevice) {
    for (CHIPBleDeviceInfo bleDevice : bleDeviceInfos) {
      if (bleDevice.macAddr.equals(lostBleDevice.macAddr)) {
        bleDeviceInfos.removeElement(bleDevice);

        new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
        break;
      }
    }
  }

  @Override
  public int getCount() {
    return bleDeviceInfos.size();
  }

  @Override
  public Object getItem(int position) {
    return bleDeviceInfos.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup container) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.commissioner_ble_device_list_item, container, false);
    }

    TextView descriptionText = convertView.findViewById(R.id.ble_device_description);
    TextView macAddrText = convertView.findViewById(R.id.ble_device_mac_addr);
    descriptionText.setText(bleDeviceInfos.get(position).name);
    macAddrText.setText(bleDeviceInfos.get(position).macAddr);

    return convertView;
  }

  @Override
  public void onBleDeviceFound(CHIPBleDeviceInfo bleDevice) {
    addBleDevice(bleDevice);
  }

  @Override
  public void onBleDeviceLost(CHIPBleDeviceInfo bleDevice) {
    removeBleDevice(bleDevice);
  }
}