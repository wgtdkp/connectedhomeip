/*
 *   Copyright (c) 2020 Project CHIP Authors
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.google.chip.chiptool.commissioner.thread.internal;

import android.Manifest;
import android.Manifest.permission;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import com.google.chip.chiptool.commissioner.thread.BorderAgentInfo;
import com.google.chip.chiptool.commissioner.thread.ThreadNetworkInfo;
import java.util.Map;

class BorderAgentDiscoverer implements NsdManager.DiscoveryListener {

  private static final String TAG = BorderAgentDiscoverer.class.getSimpleName();

  private static final String SERVICE_TYPE = "_meshcop._udp";
  private static final String KEY_DISCRIMINATOR = "discriminator";
  private static final String KEY_NETWORK_NAME = "nn";
  private static final String KEY_EXTENDED_PAN_ID = "xp";

  private WifiManager.MulticastLock wifiMulticastLock;
  private NsdManager nsdManager;
  private BorderAgentListener borderAgentListener;

  private boolean isScanning = false;

  public interface BorderAgentListener {
    void onBorderAgentFound(BorderAgentInfo borderAgentInfo);
    void onBorderAgentLost(String discriminator);
  }

  @RequiresPermission(permission.INTERNET)
  public BorderAgentDiscoverer(Context context, BorderAgentListener borderAgentListener) {
    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    wifiMulticastLock = wifi.createMulticastLock("multicastLock");

    nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

    this.borderAgentListener = borderAgentListener;
  }

  public void start() {
    if (isScanning) {
      Log.w(TAG, "the Border Agent discoverer is already running!");
      return;
    }

    isScanning = true;

    wifiMulticastLock.setReferenceCounted(true);
    wifiMulticastLock.acquire();

    nsdManager.discoverServices(
        BorderAgentDiscoverer.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
  }

  public void stop() {
    if (!isScanning) {
      Log.w(TAG, "the Border Agent discoverer has already been stopped!");
      return;
    }

    nsdManager.stopServiceDiscovery(this);

    if (wifiMulticastLock != null) {
      wifiMulticastLock.release();
    }

    isScanning = false;
  }

  @Override
  public void onDiscoveryStarted(String serviceType) {
    Log.d(TAG, "start discovering Border Agent");
  }

  @Override
  public void onDiscoveryStopped(String serviceType) {
    Log.d(TAG, "stop discovering Border Agent");
  }

  @Override
  public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
    Log.d(TAG, "a Border Agent service found");

    nsdManager.resolveService(
        nsdServiceInfo,
        new NsdManager.ResolveListener() {
          @Override
          public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(
                TAG,
                String.format(
                    "failed to resolve service %s, error: %d", serviceInfo.toString(), errorCode));
          }

          @Override
          public void onServiceResolved(NsdServiceInfo serviceInfo) {
            BorderAgentInfo borderAgent = getBorderAgentInfo(serviceInfo);
            if (borderAgent != null) {
              Log.d(TAG, "successfully resolved service: " + serviceInfo.toString());
              borderAgentListener.onBorderAgentFound(borderAgent);
            }
          }
        });
  }

  @Override
  public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
    String discriminator = getBorderAgentDiscriminator(nsdServiceInfo);
    if (discriminator != null) {
      Log.d(TAG, "a Border Agent service is gone");
      borderAgentListener.onBorderAgentLost(discriminator);
    }
  }

  @Override
  public void onStartDiscoveryFailed(String serviceType, int errorCode) {
    Log.d(TAG, "start discovering Border Agent failed: " + errorCode);
  }

  @Override
  public void onStopDiscoveryFailed(String serviceType, int errorCode) {
    Log.d(TAG, "stop discovering Border Agent failed: " + errorCode);
  }

  private BorderAgentInfo getBorderAgentInfo(NsdServiceInfo serviceInfo) {
    Map<String, byte[]> attrs = serviceInfo.getAttributes();

    // Use the host address as default discriminator.
    String discriminator = serviceInfo.getHost().getHostAddress();

    if (attrs.containsKey(KEY_DISCRIMINATOR)) {
      discriminator = new String(attrs.get(KEY_DISCRIMINATOR));
    }

    if (!attrs.containsKey(KEY_NETWORK_NAME) || !attrs.containsKey(KEY_EXTENDED_PAN_ID)) {
      return null;
    }

    return new BorderAgentInfo(
        discriminator,
        new String(attrs.get(KEY_NETWORK_NAME)),
        attrs.get(KEY_EXTENDED_PAN_ID),
        serviceInfo.getHost(),
        serviceInfo.getPort());
  }

  private String getBorderAgentDiscriminator(NsdServiceInfo serviceInfo) {
    Map<String, byte[]> attrs = serviceInfo.getAttributes();

    // Use the host address as default discriminator.
    String discriminator = null;

    if (serviceInfo.getHost() != null) {
      discriminator = serviceInfo.getHost().getHostAddress();
    }

    if (attrs.containsKey(KEY_DISCRIMINATOR)) {
      discriminator = new String(attrs.get(KEY_DISCRIMINATOR));
    }

    return discriminator;
  }
}
