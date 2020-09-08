package io.openthread.toble;

import android.util.Log;
import java.net.Inet6Address;
import java.util.concurrent.Callable;

/**
 * This class implements the ToBLE service which sends IP packets over BLE link.
 *
 */
public class TobleService extends TobleCallbacks {

  private static final String TAG = TobleService.class.getSimpleName();

  private TobleVpnService vpnService;
  private Toble toble;
  private TobleDriverImpl tobleDriver;
  private Thread thread;

  private static final TobleService instance = new TobleService();

  private TobleService() {
    toble = Toble.getInstance();
    tobleDriver = new TobleDriverImpl();
  }

  public static TobleService getInstance() {
    return instance;
  }

  public void start(Inet6Address peerDeviceAddr) {
    if (isRunning()) {
      Log.e(TAG, "TobleService is already running");
      return;
    }

    toble.init(tobleDriver);
    vpnService = new TobleVpnService(peerDeviceAddr);
    vpnService.start();

    thread = new Thread(() -> {
      // TODO(wgtdkp):
      long timeout = toble.process();
    });

    thread.start();

    // TODO(wgtdkp): run the ToBLE stack in a dedicated Thread.
  }

  public void stop() {
    if (!isRunning()) {
      Log.e(TAG, "TobleService has already been stopped");
      return;
    }

    thread.interrupt();
    thread = null;

    vpnService.stop();
    toble.deinit();
  }

  public void postTask(Callable<Void> task) {

  }

  private boolean isRunning() {
    return thread != null;
  }

  @Override
  public void onIp6Receive(SWIGTYPE_p_unsigned_char aPacket, long aPacketLength) {
    Log.d(TAG, "::onIp6Receive");

  }
}
