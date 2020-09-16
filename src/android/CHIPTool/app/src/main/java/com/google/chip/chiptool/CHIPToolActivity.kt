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
package com.google.chip.chiptool

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.fragment.app.Fragment
import com.google.chip.chiptool.clusterclient.OnOffClientFragment
import com.google.chip.chiptool.commissioner.CommissionerActivity
import com.google.chip.chiptool.echoclient.EchoClientFragment
import com.google.chip.chiptool.setuppayloadscanner.BarcodeFragment
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceDetailsFragment
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo
import io.openthread.toble.TobleService
import kotlinx.android.synthetic.main.select_action_fragment.remoteBleAddressEd
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

class CHIPToolActivity :
    AppCompatActivity(),
    BarcodeFragment.Callback,
    SelectActionFragment.Callback {

  private var peerDeviceIp6Addr: String = "UNKNOWN"

  private var tobleServiceReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent != null) {
        peerDeviceIp6Addr = intent.getStringExtra(TobleService.KEY_PEER_IP6_ADDR)!!
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.top_activity)

    if (savedInstanceState == null) {
      val fragment = SelectActionFragment.newInstance()
      supportFragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, fragment, fragment.javaClass.simpleName)
          .commit()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    stopTobleService()
  }

  override fun onResume() {
    super.onResume()
    LocalBroadcastManager.getInstance(this).registerReceiver(tobleServiceReceiver, IntentFilter(TobleService.EVENT_6OBLE_CONNECTED))
  }

  override fun onPause() {
    super.onPause()
    LocalBroadcastManager.getInstance(this).unregisterReceiver(tobleServiceReceiver)
  }

  override fun onCHIPDeviceInfoReceived(deviceInfo: CHIPDeviceInfo) {
    showFragment(CHIPDeviceDetailsFragment.newInstance(deviceInfo))
  }

  override fun handleScanQrCodeClicked() {
    showFragment(BarcodeFragment.newInstance())
  }

  override fun handleCommissioningClicked() {
    var intent = Intent(this, CommissionerActivity::class.java)
    startActivityForResult(intent, REQUEST_CODE_COMMISSIONING)
  }

  override fun handleEchoClientClicked() {
    showFragment(EchoClientFragment.newInstance())
  }

  override fun handleOnOffClicked() {
    showFragment(OnOffClientFragment.newInstance())
  }

  override fun handleStartTobleServiceClicked() {
    startTobleService();
  }

  override fun handleStopTobleServiceClicked() {
    stopTobleService();
  }

  override fun handleSendUdpClicked() {
    val sendAndReceive =
      FutureTask(Callable<String> {
        var socket: DatagramSocket? = null
        try {
          val hello = "hello".toByteArray()
          socket = DatagramSocket(11095)
          //socket.connect( InetAddress.getByName(ThreadVpnService.LOCAL_ADDR), 6666);
          // We cannot use the address (2001:1983::de8) assigned to the TUN interface.
          socket.connect(InetAddress.getByName(peerDeviceIp6Addr + "%tun0"), TEST_REMOTE_DEVICE_PORT)
          val packet = DatagramPacket(hello, hello.size)
          Log.i("SEND_UDP", "Sending hello to VPN service")
          socket.send(packet)
          "hello"
        } catch (e: SocketException) {
          e.toString()
        } catch (e: IOException) {
          e.toString()
        } finally {
          socket?.close()
        }
      })

    var executor = Executors.newFixedThreadPool(2)
    executor.execute(sendAndReceive)
    try {
      val response = sendAndReceive.get()
      Log.d("SEND_UDP", String.format("sent: %s\n", response))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == REQUEST_CODE_COMMISSIONING) {
      // Simply ignore the commissioning result.
      // TODO: tracking commissioned devices.
    } else if (requestCode == REQUEST_CODE_START_TOBLE) {
      if (resultCode == Activity.RESULT_OK) {
        var intent = Intent(this, TobleService::class.java)
        intent.setAction(TobleService.ACTION_START)
        intent.putExtra(TobleService.KEY_PEER_BLE_ADDR, getPeerBleAddress())
        startService(intent)
      }
    }
  }

  private fun showFragment(fragment: Fragment) {
    supportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, fragment, fragment.javaClass.simpleName)
        .addToBackStack(null)
        .commit()
  }

  private fun startTobleService() {
    // Start ToBLE service.
    var intent = VpnService.prepare(this)
    if (intent != null) {
      startActivityForResult(intent, REQUEST_CODE_START_TOBLE)
    } else {
      onActivityResult(REQUEST_CODE_START_TOBLE, Activity.RESULT_OK, null)
    }
  }

  private fun stopTobleService() {
    var intent = Intent(this, TobleService::class.java)
    intent.setAction(TobleService.ACTION_STOP)
    startService(intent);
  }

  private fun getPeerBleAddress(): String {
    return remoteBleAddressEd.text.toString()
  }

  companion object {
    var REQUEST_CODE_COMMISSIONING = 0xB003
    var REQUEST_CODE_START_TOBLE = 0xB004
    // var TEST_REMOTE_DEVICE_IP = "fe80:0:0:0:1872:d5f4:559:3659" // "1983::2001:de8"
    var TEST_REMOTE_DEVICE_PORT = 11095
  }
}
