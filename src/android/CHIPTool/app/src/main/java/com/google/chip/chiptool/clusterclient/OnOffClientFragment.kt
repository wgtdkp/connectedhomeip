package com.google.chip.chiptool.clusterclient

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.fragment.app.Fragment
import chip.devicecontroller.ChipCommandType
import chip.devicecontroller.ChipDeviceController
import com.google.chip.chiptool.ChipClient
import com.google.chip.chiptool.CommissionedDeviceAdapter
import com.google.chip.chiptool.CommissionedDeviceDiscoverer
import com.google.chip.chiptool.CommissionedDeviceInfo
import com.google.chip.chiptool.R
import com.google.chip.chiptool.commissioner.NetworkInfo
import kotlinx.android.synthetic.main.on_off_client_fragment.commandStatusTv
import kotlinx.android.synthetic.main.on_off_client_fragment.ipAddressEd
import kotlinx.android.synthetic.main.on_off_client_fragment.view.offBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.onBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.toggleBtn
import kotlinx.android.synthetic.main.on_off_client_fragment.view.commissionedDeviceList

class OnOffClientFragment : Fragment(), ChipDeviceController.CompletionListener {
  private val deviceController: ChipDeviceController
    get() = ChipClient.getDeviceController()

  private var commandType: ChipCommandType? = null

  private var deviceAdapter: CommissionedDeviceAdapter? = null
  private var deviceDiscoverer: CommissionedDeviceDiscoverer? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    deviceAdapter = CommissionedDeviceAdapter(context)
    deviceDiscoverer = CommissionedDeviceDiscoverer(context, deviceAdapter)
    deviceDiscoverer!!.start()
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

      commissionedDeviceList.adapter = deviceAdapter

      commissionedDeviceList.onItemClickListener =
        OnItemClickListener { adapterView, view, position, id ->
          var selectedDevice = deviceAdapter?.getItem(position) as CommissionedDeviceInfo
          ipAddressEd.setText(selectedDevice.ipAddress.getHostAddress())
        }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    deviceDiscoverer?.stop()
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

  companion object {
    private const val TAG = "OnOffClientFragment"
    fun newInstance(): OnOffClientFragment = OnOffClientFragment()
  }
}
