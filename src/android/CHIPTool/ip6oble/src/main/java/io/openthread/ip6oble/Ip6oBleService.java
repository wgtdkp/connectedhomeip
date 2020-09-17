package io.openthread.ip6oble;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

interface Ip6oBleRunner {
  void postTask(Runnable task);
}

/**
 * This class implements the IP6oBLE VPN service that creates a dedicated TUN interface
 * and send/receive IP packets to/from IP6oBLE.
 *
 */
public class Ip6oBleService extends VpnService implements Ip6oBleRunner {

  private static final String  TAG = Ip6oBleService.class.getSimpleName();

  public static final String ACTION_START = "io.openthread.ip6oble.Ip6oBleService.START";
  public static final String ACTION_STOP = "io.openthread.ip6oble.Ip6oBleService.STOP";
  public static final String KEY_PEER_BLE_ADDR = "peer_ble_addr";
  public static final String KEY_PEER_IP6_ADDR = "peer_ip6_addr";
  public static final String KEY_LOCAL_IP6_ADDR = "local_ip6_addr";

  public static final String EVENT_6OBLE_CONNECTED = "io.openthread.ip6oble.Ip6oBleService.IP6OBLE_CONNECTED";
  public static final String EVENT_6OBLE_DISCONNECTED = "io.openthread.ip6oble.Ip6oBleService.IP6OBLE_DISCONNECTED";

  /** Maximum packet size is constrained by the MTU, which is given as a signed short. */
  private static final int MAX_PACKET_SIZE = 1024;
  private static final int BLE_CONNECTION_INTERVAL = 1000; // In Milliseconds.
  private static final int BLE_CONNECTION_SCAN_INTERVAL = 50; // In Milliseconds.
  private static final int BLE_CONNECTION_SCAN_WINDOW = 40; // In Milliseconds.

  private Ip6oBle ip6oble = Ip6oBle.getInstance();
  private Ip6oBleHandler ip6oBleHandler = new Ip6oBleHandler();
  private Ip6oBleDriverImpl ip6oBleDriver = new Ip6oBleDriverImpl(this, this);

  private BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<Runnable>(256);

  private Thread thread;

  private ParcelFileDescriptor tunInterface;
  private FileInputStream tunInterfaceIn;
  private FileOutputStream tunInterfaceOut;

  private static final int STATE_IDLE = 0;
  private static final int STATE_CONNECTING = 1;
  private static final int STATE_CONNECTED = 2;
  private static final int STATE_DISCONNECTED = 3;

  private AtomicInteger state = new AtomicInteger(STATE_IDLE);
  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }

    if (ACTION_STOP.equals(intent.getAction())) {
      stop();
      return START_NOT_STICKY;
    } else {
      start(intent.getExtras().getString(KEY_PEER_BLE_ADDR));
      return START_STICKY;
    }
  }

  @Override
  public void onDestroy() {
    stop();
    super.onDestroy();
  }

  private void connectToPeer(String peerBleAddr) {
    otError error = ip6oble.connect(Ip6oBleUtils.ip6oBleAddrFromString(peerBleAddr),
                                  BLE_CONNECTION_INTERVAL,
                                  BLE_CONNECTION_SCAN_INTERVAL,
                                  BLE_CONNECTION_SCAN_WINDOW);
    if (error != otError.OT_ERROR_NONE) {
      Log.e(TAG, String.format("failed to connect to peer device: %s", peerBleAddr));
    }
  }

  private void forwardPacketTo6oble() {
    byte[] packet = new byte[MAX_PACKET_SIZE];

    assert(state.get() == STATE_CONNECTED);

    try {
      int length = tunInterfaceIn.read(packet);
      if (length > 0) {
        Log.d(TAG, String.format("sending packet via IP6oBLE: %s", Ip6oBleUtils
            .getHexString(packet, length)));

        otError error = ip6oble.ip6Send(Ip6oBleUtils.getByteArray(packet, length).cast(), length);
        if (!error.equals(otError.OT_ERROR_NONE)) {
          Log.e(TAG, "sending packets to IP6oBLE failed");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void start(String peerBleAddr) {
    if (state.get() == STATE_CONNECTING || state.get() == STATE_CONNECTED) {
      Log.w(TAG, "The 6oBLE service is already running");
      return;
    }

    state.set(STATE_IDLE);

    Log.d(TAG, "start 6oBLE service with remote BLE address: " + peerBleAddr.toString());

    ip6oble.init(ip6oBleHandler, ip6oBleDriver);

    thread = new Thread(() -> {
      connectToPeer(peerBleAddr);

      try {
        while (!shouldStop.get() && (state.get() == STATE_CONNECTING || state.get() == STATE_CONNECTED)) {
          if (state.get() == STATE_CONNECTED) {
            forwardPacketTo6oble();
          } else if (state.get() == STATE_DISCONNECTED) {
            shouldStop.set(true);
          }

          while (!taskQueue.isEmpty()) {
            taskQueue.take().run();
          }

          ip6oble.process();
        }

        Log.d(TAG, "6oBLE service is stopped");
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        state.set(STATE_DISCONNECTED);
        ip6oBleDriver.finalize();
        ip6oble.deinit();
        destroyTunInterface();
      }
    });

    state.set(STATE_CONNECTING);
    thread.start();
  }

  /*
  private void runIp6oBle(Inet6Address peerAddr) {
    if (isRunning.get()) {
      Log.w(TAG, "The VPN service is already running");
      return;
    }

    String localAddrStr = ip6oBle.init(ip6oBleHandler, ip6oBleDriver);

    Log.d(TAG, "init with link local address: " + localAddrStr);
    Log.d(TAG, "init with peer address: " + peerAddr.toString());

    isRunning.set(true);
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
      outputStream = new FileOutputStream(tunInterface.getFileDescriptor());

      byte[] packet = new byte[MAX_PACKET_SIZE];

      try {
        while (isRunning.get()) {
          // Read the outgoing packet from the input stream.
          int length = in.read(packet);

          if (length > 0) {
            Log.d(TAG, String.format("sending packet via IP6oBLE: %s", Ip6oBleUtils.getHexString(packet, length)));

            otError error = ip6oBle.ip6Send(Ip6oBleUtils.getByteArray(packet, length).cast(), length);
            if (!error.equals(otError.OT_ERROR_NONE)) {
              Log.e(TAG, "sending packets to IP6oBLE failed");
            }
          }

          while (!taskQueue.isEmpty()) {
            taskQueue.take().run();
          }

          timeout = ip6oBle.process();
        }

        Log.d(TAG, "IPoBLE service is stopped");
      } catch (Exception e) {
        Log.e(TAG, "Cannot use socket", e);
      } finally {
        if (tunInterface != null) {
          try {
            ip6oBleDriver.finalize();
            ip6oBle.deinit();
            tunInterface.close();

          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    });

    thread.start();
  }
  */

  private void stop() {
    shouldStop.set(true);
    thread = null;
  }

  private void receivePacketFromIp6oBle(byte[] packet) {
    if (state.get() == STATE_CONNECTED) {
      postTask(() -> {
        try {
          tunInterfaceOut.write(packet);
          Log.d(TAG, String.format("received packet from IP6oBLE: %s", Ip6oBleUtils
              .getHexString(packet, packet.length)));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    } else {
      Log.e(TAG, "6oBLE serivce is down, dropping packet");
    }
  }

  @Override
  public void postTask(Runnable task) {
    if (state.get() == STATE_CONNECTING || state.get() == STATE_CONNECTED) {
      taskQueue.add(task);
    } else {
      Log.e(TAG, "6oBLE service is down, dropping task!");
    }
  }

  private void createTunInterface(String localAddr, String peerAddr) {
    Log.d(TAG, String.format("create TUN interface with localAddr=%s, peerAddr=%s", localAddr, peerAddr));

    // Configure the TUN and get the interface.
    tunInterface = new Builder().setSession("My6obleService")
        .addAddress(localAddr, 128)
        .addRoute(peerAddr, 128)
        .setBlocking(false)
        .establish();

    // Packets to be sent are queued in this input stream.
    tunInterfaceIn = new FileInputStream(tunInterface.getFileDescriptor());

    // Packets received need to be written to this output stream.
    tunInterfaceOut = new FileOutputStream(tunInterface.getFileDescriptor());
  }

  private void destroyTunInterface() {
    if (tunInterface != null) {
      try {
        tunInterfaceIn.close();
        tunInterfaceOut.close();
        tunInterface.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      tunInterfaceIn = null;
      tunInterfaceOut = null;
      tunInterface = null;
    }
  }

  private void sendConnectedBroadcast(String localAddr, String peerAddr) {
    Intent intent = new Intent(EVENT_6OBLE_CONNECTED);
    intent.putExtra(KEY_LOCAL_IP6_ADDR, localAddr);
    intent.putExtra(KEY_PEER_IP6_ADDR, peerAddr);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  private void sendDisconnectedBroadcast() {
    Intent intent = new Intent(EVENT_6OBLE_DISCONNECTED);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  class Ip6oBleHandler extends Ip6oBleCallbacks {
    @Override
    public void onConnected(otError aError, Ip6oBleConnection aConn, String aLocalAddress, String aPeerAddress) {
      if (aError != aError.OT_ERROR_NONE) {
        postTask(() -> {
          Log.e(TAG, "failed to connect to peer device");
          sendConnectedBroadcast(null, null);
          state.set(STATE_DISCONNECTED);
        });
      } else {
        postTask(() -> {
          Log.i(TAG, String.format("connected to peer device: %s", aPeerAddress));
          createTunInterface(aLocalAddress, aPeerAddress);
          sendConnectedBroadcast(aLocalAddress, aPeerAddress);
          state.set(STATE_CONNECTED);
        });
      }
    }

    @Override
    public void onDisconnected(otError aError, Ip6oBleConnection aConn) {
      postTask(() -> {
        Log.d(TAG, "disconnecting from peer device");
        destroyTunInterface();
        sendDisconnectedBroadcast();
        state.set(STATE_DISCONNECTED);
      });
    }

    @Override
    public void onIp6Receive(SWIGTYPE_p_unsigned_char aPacket, long aPacketLength) {
      ByteArray packet = ByteArray.frompointer(aPacket);
      receivePacketFromIp6oBle(Ip6oBleUtils.getByteArray(packet, (int)aPacketLength));
    }
  }
}
