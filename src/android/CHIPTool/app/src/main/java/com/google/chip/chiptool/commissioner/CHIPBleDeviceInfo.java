package com.google.chip.chiptool.commissioner;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

public class CHIPBleDeviceInfo implements Parcelable {

  @NonNull
  public String name;

  @NonNull
  public String macAddr;

  @NonNull
  public CHIPBleServiceData serviceData;

  CHIPBleDeviceInfo(@NonNull String name,
                    @NonNull String macAddr,
                    @NonNull CHIPBleServiceData serviceData) {
    this.name = name;
    this.macAddr = macAddr;
    this.serviceData = serviceData;
  }

  protected CHIPBleDeviceInfo(Parcel in) {
    name = in.readString();
    macAddr = in.readString();
    serviceData = in.readParcelable(CHIPBleServiceData.class.getClassLoader());
  }

  public static final Creator<CHIPBleDeviceInfo> CREATOR = new Creator<CHIPBleDeviceInfo>() {
    @Override
    public CHIPBleDeviceInfo createFromParcel(Parcel in) {
      return new CHIPBleDeviceInfo(in);
    }

    @Override
    public CHIPBleDeviceInfo[] newArray(int size) {
      return new CHIPBleDeviceInfo[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeString(name);
    parcel.writeString(macAddr);
    parcel.writeParcelable(serviceData, flags);
  }
}
