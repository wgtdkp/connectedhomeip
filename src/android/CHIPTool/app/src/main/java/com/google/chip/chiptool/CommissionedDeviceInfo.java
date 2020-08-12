package com.google.chip.chiptool;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class CommissionedDeviceInfo implements Parcelable {
  public InetAddress ipAddress;
  public String name;
  public String type;

  public CommissionedDeviceInfo(InetAddress ipAddress, String name, String type) {
    this.ipAddress = ipAddress;
    this.name = name;
    this.type = type;
  }

  protected CommissionedDeviceInfo(Parcel in) {
    try {
      ipAddress = InetAddress.getByAddress(in.createByteArray());
    } catch (UnknownHostException e) {
    }
    name = in.readString();
    type = in.readString();
  }

  public static final Creator<CommissionedDeviceInfo> CREATOR =
      new Creator<CommissionedDeviceInfo>() {
        @Override
        public CommissionedDeviceInfo createFromParcel(Parcel in) {
          return new CommissionedDeviceInfo(in);
        }

        @Override
        public CommissionedDeviceInfo[] newArray(int size) {
          return new CommissionedDeviceInfo[size];
        }
      };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeByteArray(ipAddress.getAddress());
    parcel.writeString(name);
    parcel.writeString(type);
  }
}
