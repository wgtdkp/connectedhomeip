package com.google.chip.chiptool.commissioner;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.util.ArrayList;

public class NetworkInfo implements Parcelable {

  private ArrayList<BorderAgentInfo> borderAgents;

  public NetworkInfo(BorderAgentInfo borderAgent) {
    borderAgents = new ArrayList<>();
    borderAgents.add(borderAgent);
  }

  public InetAddress getHost() { return borderAgents.get(0).host; }

  public int getPort() { return borderAgents.get(0).port; }

  public String getNetworkName() {
    return borderAgents.get(0).networkName;
  }

  public byte[] getExtendedPanId() {
    return borderAgents.get(0).extendedPanId;
  }

  public void merge(NetworkInfo networkInfo) {
    borderAgents.addAll(networkInfo.borderAgents);
  }

  public void addBorderAgent(BorderAgentInfo borderAgent) {
    // TODO(wgtdkp): verify that the network name and extended PAN ID match.
    borderAgents.add(borderAgent);
  }

  protected NetworkInfo(Parcel in) {
    borderAgents = in.readArrayList(BorderAgentInfo.class.getClassLoader());
  }

  public static final Creator<NetworkInfo> CREATOR = new Creator<NetworkInfo>() {
    @Override
    public NetworkInfo createFromParcel(Parcel in) {
      return new NetworkInfo(in);
    }

    @Override
    public NetworkInfo[] newArray(int size) {
      return new NetworkInfo[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeList(borderAgents);
  }
}
