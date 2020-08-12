package com.google.chip.chiptool;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.net.InetAddress;
import java.util.Vector;

public class CommissionedDeviceAdapter extends BaseAdapter {

  private Vector<CommissionedDeviceInfo> devices;

  private LayoutInflater inflater;

  public CommissionedDeviceAdapter(Context context) {
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    devices = new Vector<>();
  }

  public boolean addDevice(CommissionedDeviceInfo newDevice) {
    CommissionedDeviceInfo existingDevice = null;

    for (CommissionedDeviceInfo device : devices) {
      if (newDevice.ipAddress.equals(device.ipAddress)) {
        existingDevice = device;
        break;
      }
    }

    if (existingDevice != null) {
      existingDevice.name = newDevice.name;
      existingDevice.type = newDevice.type;
    } else {
      devices.add(newDevice);
    }
    notifyDataSetChanged();

    return existingDevice == null;
  }

  public boolean removeDevice(InetAddress ipAddress) {
    for (CommissionedDeviceInfo device : devices) {
      if (device.ipAddress.equals(ipAddress)) {
        devices.removeElement(device);
        notifyDataSetChanged();
        return true;
      }
    }
    return false;
  }

  @Override
  public int getCount() {
    return devices.size();
  }

  @Override
  public Object getItem(int position) {
    return devices.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup container) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.commissioned_device_list_item, container, false);
    }

    CommissionedDeviceInfo device = devices.get(position);

    TextView deviceNameText = convertView.findViewById(R.id.device_name);
    deviceNameText.setText(device.name);

    TextView deviceIpAddressText = convertView.findViewById(R.id.device_ip_address);
    deviceIpAddressText.setText(device.ipAddress.getHostAddress());

    return convertView;
  }
}
