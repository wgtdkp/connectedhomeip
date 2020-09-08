package io.openthread.toble;

import android.util.Log;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * This class implements the ToBLE VPN service that creates a dedicated TUN interface
 * and send/receive IP packets to/from ToBLE.
 *
 */
public class TobleVpnService {

  private static final String  TAG = TobleVpnService.class.getSimpleName();

  private InetAddress peerDeviceAddr;

  public TobleVpnService(Inet6Address peerDeviceAddr) {
    this.peerDeviceAddr = peerDeviceAddr;
  }

  public void start() {
    // TODO(wgtdkp):
  }

  public void stop() {
    // TODO(wgtdkp):
  }
}
