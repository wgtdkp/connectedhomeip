package com.google.chip.chiptool;

import android.Manifest.permission;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import java.util.Map;

public class CommissionedDeviceDiscoverer implements NsdManager.DiscoveryListener {

  private static final String TAG = CommissionedDeviceDiscoverer.class.getSimpleName();

  private static final String SERVICE_TYPE = "_chip._udp";
  private static final String KEY_NAME = "name";
  private static final String KEY_TYPE = "type";

  private static final byte[] PSKC = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

  private WifiManager.MulticastLock wifiMulticastLock;
  private NsdManager nsdManager;
  private CommissionedDeviceAdapter deviceAdapter;

  private boolean isRunning = false;

  @RequiresPermission(permission.INTERNET)
  public CommissionedDeviceDiscoverer(Context context, CommissionedDeviceAdapter deviceAdapter) {
    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    wifiMulticastLock = wifi.createMulticastLock("multicastLock");

    nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

    this.deviceAdapter = deviceAdapter;
  }

  public void start() {
    if (isRunning) {
      return;
    }

    isRunning = true;
    wifiMulticastLock.setReferenceCounted(true);
    wifiMulticastLock.acquire();

    nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
  }

  public void stop() {
    if (!isRunning) {
      return;
    }

    nsdManager.stopServiceDiscovery(this);
    if (wifiMulticastLock != null) {
      wifiMulticastLock.release();
      wifiMulticastLock = null;
    }
    isRunning = false;
  }

  @Override
  public void onDiscoveryStarted(String serviceType) {
    Log.d(TAG, "start discovering CHIP devices");
  }

  @Override
  public void onDiscoveryStopped(String serviceType) {
    Log.d(TAG, "stop discovering CHIP devices");
  }

  @Override
  public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
    Log.d(TAG, "a CHIP device found");

    nsdManager.resolveService(
        nsdServiceInfo,
        new NsdManager.ResolveListener() {
          @Override
          public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(
                TAG,
                String.format(
                    "failed to resolve CHIP device service %s, error: %d", serviceInfo.toString(), errorCode));
          }

          @Override
          public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            Log.d(TAG, "successfully resolved CHIP device service " + serviceInfo.toString());

            Map<String, byte[]> attrs = serviceInfo.getAttributes();

            try {
              final String deviceName = new String(attrs.get(KEY_NAME));
              final String deviceType = new String(attrs.get(KEY_TYPE));

              Handler handler = new Handler(Looper.getMainLooper());
              handler.post(
                  new Runnable() {
                    @Override
                    public void run() {
                      deviceAdapter.addDevice(new CommissionedDeviceInfo(serviceInfo.getHost(), deviceName, deviceType));
                    }
                  });
            } catch (Exception e) {
              Log.e(TAG, "invalid CHIP device service: " + e.toString());
            }
          }
        });
  }

  @Override
  public void onServiceLost(final NsdServiceInfo serviceInfo) {
    Log.d(TAG, "a CHIP device is gone");

    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            deviceAdapter.removeDevice(serviceInfo.getHost());
          }
        });
  }

  @Override
  public void onStartDiscoveryFailed(String serviceType, int errorCode) {
    Log.d(TAG, "start discovering CHIP devices failed: " + errorCode);
  }

  @Override
  public void onStopDiscoveryFailed(String serviceType, int errorCode) {
    Log.d(TAG, "stop discovering CHIP devices failed: " + errorCode);
  }
}
