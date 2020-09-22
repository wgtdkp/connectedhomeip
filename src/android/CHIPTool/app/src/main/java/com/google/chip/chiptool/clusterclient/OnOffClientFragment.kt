package com.google.chip.chiptool.clusterclient

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import chip.devicecontroller.ChipCommandType
import chip.devicecontroller.ChipDeviceController
import com.google.chip.chiptool.CHIPToolActivity
import com.google.chip.chiptool.ChipClient
import com.google.chip.chiptool.R
import com.google.chip.chiptool.commissioner.CHIPBleDeviceAdapter
import com.google.chip.chiptool.commissioner.CHIPBleDeviceDiscoverer
import com.google.chip.chiptool.commissioner.CHIPBleDeviceInfo
import io.openthread.ip6oble.Ip6oBleService
import kotlinx.android.synthetic.main.on_off_client_fragment.commandStatusTv
import kotlinx.android.synthetic.main.on_off_client_fragment.ipAddressEd
import kotlinx.android.synthetic.main.on_off_client_fragment.startIp6oBleServiceBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.offBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.onBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.sendUdpBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.startIp6oBleServiceBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.stopIp6oBleServiceBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.toggleBtn
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

class OnOffClientFragment : Fragment(), ChipDeviceController.CompletionListener {
  private val deviceController: ChipDeviceController
    get() = ChipClient.getDeviceController()

  private var commandType: ChipCommandType? = null

  private var bleDeviceAdapter: CHIPBleDeviceAdapter? = null
  private var bleDeviceDiscoverer: CHIPBleDeviceDiscoverer? = null

  private var selectedBleDevice: CHIPBleDeviceInfo? = null

  private var peerDeviceIp6Addr: String = ""

  private var ip6oBleServiceReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent != null) {
        peerDeviceIp6Addr = intent.getStringExtra(Ip6oBleService.KEY_PEER_IP6_ADDR)?: return

        Handler(Looper.getMainLooper()).post(Runnable { ipAddressEd.setText(peerDeviceIp6Addr) })
      }
    }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.on_off_client_fragment, container, false).apply {
      deviceController.setCompletionListener(this@OnOffClientFragment)

      onBtn.setOnClickListener { sendOnCommandClick() }
      offBtn.setOnClickListener { sendOffCommandClick() }
      toggleBtn.setOnClickListener { sendToggleCommandClick() }
      startIp6oBleServiceBtn.setOnClickListener { handleStartIp6oBleServiceClicked() }
      stopIp6oBleServiceBtn.setOnClickListener { handleStopIp6oBleServiceClicked() }
      sendUdpBtn.setOnClickListener { handleSendUdpClicked() }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bleDeviceAdapter = CHIPBleDeviceAdapter(requireContext())
    bleDeviceDiscoverer = CHIPBleDeviceDiscoverer(requireContext(), bleDeviceAdapter!!)

    val bleDeviceListView =
      view.findViewById<ListView>(R.id.ble_devices)
    bleDeviceListView.adapter = bleDeviceAdapter
    bleDeviceListView.onItemClickListener =
      OnItemClickListener { adapterView: AdapterView<*>, v: View?, position: Int, id: Long ->
        selectedBleDevice =
          adapterView.getItemAtPosition(
            position
          ) as CHIPBleDeviceInfo
      }

    bleDeviceDiscoverer!!.start()
  }

  override fun onResume() {
    super.onResume()
    LocalBroadcastManager.getInstance(requireContext()).registerReceiver(ip6oBleServiceReceiver, IntentFilter(
      Ip6oBleService.EVENT_6OBLE_CONNECTED)
    )
  }

  override fun onPause() {
    super.onPause()
    LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(ip6oBleServiceReceiver)
  }

  override fun onDestroy() {
    super.onDestroy()
    bleDeviceDiscoverer!!.stop()
    stopIp6oBleService()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == CHIPToolActivity.REQUEST_CODE_START_IP6OBLE) {
      if (resultCode == Activity.RESULT_OK) {
        var intent = Intent(requireContext(), Ip6oBleService::class.java)
        intent.setAction(Ip6oBleService.ACTION_START)
        intent.putExtra(Ip6oBleService.KEY_PEER_BLE_ADDR, getPeerBleAddress())
        activity?.startService(intent)
      }
    }
  }

  override fun onConnectDeviceComplete() {
    sendCommand()
  }

  override fun onSendMessageComplete(message: String?) {
    commandStatusTv.text = requireContext().getString(R.string.echo_status_response, message)
  }

  override fun onError(error: Throwable) {
    Log.d(TAG, "onError: $error")
  }

  private fun sendOnCommandClick() {
    commandType = ChipCommandType.ON
    if (deviceController.isConnected) sendCommand() else connectToDevice()
  }

  private fun sendOffCommandClick() {
    commandType = ChipCommandType.OFF
    if (deviceController.isConnected) sendCommand() else connectToDevice()
  }

  private fun sendToggleCommandClick() {
    commandType = ChipCommandType.TOGGLE
    if (deviceController.isConnected) sendCommand() else connectToDevice()
  }

  private fun connectToDevice() {
    commandStatusTv.text = requireContext().getString(R.string.echo_status_connecting)
    deviceController.apply {
      disconnectDevice()
      beginConnectDevice(ipAddressEd.text.toString())
    }
  }

  private fun sendCommand() {
    commandType ?: run {
      Log.e(TAG, "No ChipCommandType specified.")
      return
    }

    commandStatusTv.text = requireContext().getString(R.string.echo_status_sending_message)

    deviceController.beginSendCommand(commandType)
  }

  private fun handleSendUdpClicked() {
    val sendAndReceive =
      FutureTask(Callable<String> {
        var socket: DatagramSocket? = null
        try {
          val hello = "hello".toByteArray()
          socket = DatagramSocket(11095)
          //socket.connect( InetAddress.getByName(ThreadVpnService.LOCAL_ADDR), 6666);
          // We cannot use the address (2001:1983::de8) assigned to the TUN interface.
          socket.connect(InetAddress.getByName(peerDeviceIp6Addr + "%tun0"), CHIPToolActivity.TEST_REMOTE_DEVICE_PORT)
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

  private fun handleStartIp6oBleServiceClicked() {
    bleDeviceDiscoverer!!.stop()

    // TODO: we may need to wait a while for the BLE device discoverer to stop.
    startIp6oBleService();
  }

  private fun handleStopIp6oBleServiceClicked() {
    stopIp6oBleService();

    bleDeviceDiscoverer!!.start()
    ipAddressEd.setText(R.string.enter_ip_address_hint_text)
  }

  private fun startIp6oBleService() {
    // Start IP6oBLE service.
    var intent = VpnService.prepare(requireContext())
    if (intent != null) {
      startActivityForResult(intent, CHIPToolActivity.REQUEST_CODE_START_IP6OBLE)
    } else {
      onActivityResult(CHIPToolActivity.REQUEST_CODE_START_IP6OBLE, Activity.RESULT_OK, null)
    }
  }

  private fun stopIp6oBleService() {

    var intent = Intent(requireContext(), Ip6oBleService::class.java)
    intent.setAction(Ip6oBleService.ACTION_STOP)
    activity?.startService(intent);
  }

  private fun getPeerBleAddress(): String {
    return selectedBleDevice!!.macAddr
  }

  companion object {
    private const val TAG = "OnOffClientFragment"
    fun newInstance(): OnOffClientFragment = OnOffClientFragment()
  }
}
