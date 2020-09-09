package io.openthread.toble;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

interface TobleRunner {
  void postTask(Runnable task);
}

/**
 * This class implements the ToBLE VPN service that creates a dedicated TUN interface
 * and send/receive IP packets to/from ToBLE.
 *
 */
public class TobleService extends VpnService implements TobleRunner {

  private static final String  TAG = TobleService.class.getSimpleName();

  public static final String ACTION_START = "io.openthread.toble.TobleService.START";
  public static final String ACTION_STOP = "io.openthread.toble.TobleService.STOP";
  public static final String KEY_LOCAL_ADDR = "local_addr";
  public static final String KEY_PEER_ADDR = "peer_addr";

  /** Maximum packet size is constrained by the MTU, which is given as a signed short. */
  private static final int MAX_PACKET_SIZE = 1024;

  private Toble toble = Toble.getInstance();
  private TobleHandler tobleHandler = new TobleHandler();
  private TobleDriverImpl tobleDriver = new TobleDriverImpl(getApplicationContext(), this);

  private Thread thread;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }

    if (ACTION_STOP.equals(intent.getAction())) {
      stop();
      return START_NOT_STICKY;
    } else {
      try {
        Inet6Address peerAddr = (Inet6Address) InetAddress.getByName(intent.getExtras().getString(KEY_PEER_ADDR));

        start(peerAddr);
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
      return START_STICKY;
    }
  }

  @Override
  public void onDestroy() {
    stop();
    super.onDestroy();
  }

  private void start(Inet6Address peerAddr) {
    if (isRunning()) {
      Log.w(TAG, "The VPN service is already running");
      return;
    }

    String localAddrStr = toble.init(tobleHandler, tobleDriver);

    Log.d(TAG, "init with link local address: " + localAddrStr);

    thread = new Thread(() -> {
      long timeout = 0;

      Inet6Address localAddr = null;
      try {
        localAddr = (Inet6Address) Inet6Address.getByName(localAddrStr);
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }

      // Configure the TUN and get the interface.
      ParcelFileDescriptor tunInterface = new Builder().setSession("MyVPNService")
          .addAddress(localAddr, 128)
          .addRoute(peerAddr, 128)
          .setBlocking(false)
          .establish();

      // Packets to be sent are queued in this input stream.
      FileInputStream in = new FileInputStream(
          tunInterface.getFileDescriptor());

      // Packets received need to be written to this output stream.
      FileOutputStream out = new FileOutputStream(
          tunInterface.getFileDescriptor());

      byte[] packet = new byte[MAX_PACKET_SIZE];

      try {
        while (true) {
          // Read the outgoing packet from the input stream.
          int length = in.read(packet);

          if (length > 0) {
            Log.d(TAG, String.format("sending packet via ToBLE: %s", TobleUtils.getHexString(packet, length)));

            otError error = toble.ip6Send(TobleUtils.getByteArray(packet, length).cast(), length);
            if (!error.equals(otError.OT_ERROR_NONE)) {
              Log.e(TAG, "sending packets to ToBLE failed");
            }
          }

          timeout = toble.process();
        }
      } catch (Exception e) {
        Log.e(TAG, "Cannot use socket", e);
      } finally {
        if (tunInterface != null) {
          try {
            tunInterface.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    });

    thread.start();
  }

  private void stop() {
    if (thread != null) {
      thread.interrupt();
      thread = null;
    }
  }

  public boolean isRunning() {
    return thread != null;
  }

  private void receivePacketFromToble(byte[] packet) {
    Log.d(TAG, String.format("received packet from ToBLE: %s", TobleUtils.getHexString(packet, packet.length)));
  }

  @Override
  public void postTask(Runnable task) {
    // TODO(wgtdkp):
  }

  class TobleHandler extends TobleCallbacks {
    @Override
    public void onIp6Receive(SWIGTYPE_p_unsigned_char aPacket, long aPacketLength) {
      ByteArray packet = ByteArray.frompointer(aPacket);
      receivePacketFromToble(TobleUtils.getByteArray(packet, (int)aPacketLength));
    }
  }
}
