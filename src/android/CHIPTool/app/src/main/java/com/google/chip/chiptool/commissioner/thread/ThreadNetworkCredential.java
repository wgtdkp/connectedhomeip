package com.google.chip.chiptool.commissioner.thread;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.chip.chiptool.commissioner.NetworkCredential;
import io.openthread.commissioner.ActiveOperationalDataset;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ThreadNetworkCredential implements NetworkCredential, Parcelable {

  private static final String TAG = ThreadNetworkCredential.class.getSimpleName();

  private static final int MAX_THREAD_NETWORK_NAME_LENGTH = 16;
  private static final int THREAD_EXTENDED_PAN_ID_LENGTH = 8;
  private static final int THREAD_MESH_PREFIX_LENGTH = 8;
  private static final int THREAD_MASTER_KEY_LENGTH = 16;
  private static final int THREAD_PSKC_LENGTH = 16;


  private byte[] activeOperationalDataset;

  public static ThreadNetworkCredential fromActiveOperationalDataset(ActiveOperationalDataset activeOperationalDataset) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try {

      assert(activeOperationalDataset.getNetworkName().length() <= MAX_THREAD_NETWORK_NAME_LENGTH);
      bos.write(activeOperationalDataset.getNetworkName().getBytes());
      byte[] padding = new byte[MAX_THREAD_NETWORK_NAME_LENGTH + 1 - activeOperationalDataset
          .getNetworkName().length()];
      Arrays.fill(padding, (byte) 0);
      bos.write(padding);

      assert(activeOperationalDataset.getExtendedPanId().size() == THREAD_EXTENDED_PAN_ID_LENGTH);
      bos.write(CommissionerUtils.getByteArray(activeOperationalDataset.getExtendedPanId()));

      assert(activeOperationalDataset.getMeshLocalPrefix().size() == THREAD_MESH_PREFIX_LENGTH);
      bos.write(CommissionerUtils.getByteArray(activeOperationalDataset.getMeshLocalPrefix()));

      assert(activeOperationalDataset.getNetworkMasterKey().size() == THREAD_MASTER_KEY_LENGTH);
      bos.write(CommissionerUtils.getByteArray(activeOperationalDataset.getNetworkMasterKey()));

      assert(activeOperationalDataset.getPSKc().size() == THREAD_PSKC_LENGTH);
      bos.write(CommissionerUtils.getByteArray(activeOperationalDataset.getPSKc()));

      bos.write(new byte[]{(byte)(activeOperationalDataset.getPanId() & 0xff), (byte)((activeOperationalDataset.getPanId() >> 8) & 0xff)});
      bos.write(new byte[]{(byte)(activeOperationalDataset.getChannel().getNumber() & 0xff)});

      // Fields present flags for ExtendedPanId, MeshLocalPrefix and PSKc.
      bos.write(new byte[]{1, 1, 1});
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.d(TAG, String.format("created Thread Network Credential from Active Operational Dataset: %s", CommissionerUtils.getHexString(bos.toByteArray())));
    return new ThreadNetworkCredential(bos.toByteArray());
  }

  public ThreadNetworkCredential(@NonNull byte[] activeOperationalDataset) {
    this.activeOperationalDataset = activeOperationalDataset;
  }

  public byte[] getActiveOperationalDataset() {
    return activeOperationalDataset;
  }

  @Override
  public byte[] getEncoded() {
    return activeOperationalDataset;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeByteArray(activeOperationalDataset);
  }
}
