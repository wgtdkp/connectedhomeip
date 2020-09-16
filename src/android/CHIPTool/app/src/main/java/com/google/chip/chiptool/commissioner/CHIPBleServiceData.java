package com.google.chip.chiptool.commissioner;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.lang.reflect.Array;
import java.util.Arrays;

public class CHIPBleServiceData implements Parcelable {

  @NonNull
  public boolean isPaired;

  @NonNull
  public byte[] discriminator;

  @NonNull
  public byte[] vendorId;

  @NonNull
  public byte[] productId;

  public static CHIPBleServiceData fromByteArray(@NonNull byte[] bytes) {
    CHIPBleServiceData serviceData = new CHIPBleServiceData();
    serviceData.isPaired = (bytes[0] != 0);
    serviceData.discriminator = Arrays.copyOfRange(bytes, 1, 3);
    serviceData.vendorId = Arrays.copyOfRange(bytes, 3, 5);
    serviceData.productId = Arrays.copyOfRange(bytes, 5, 7);

    return serviceData;
  }

  private CHIPBleServiceData() {

  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {

  }
}
