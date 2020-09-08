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
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.chip.chiptool.clusterclient.OnOffClientFragment
import com.google.chip.chiptool.commissioner.CommissionerActivity
import com.google.chip.chiptool.echoclient.EchoClientFragment
import com.google.chip.chiptool.setuppayloadscanner.BarcodeFragment
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceDetailsFragment
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo
import io.openthread.toble.TobleService

class CHIPToolActivity :
    AppCompatActivity(),
    BarcodeFragment.Callback,
    SelectActionFragment.Callback {

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

    startTobleService();
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

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == REQUEST_CODE_COMMISSIONING) {
      // Simply ignore the commissioning result.
      // TODO: tracking commissioned devices.
    } else if (requestCode == REQUEST_CODE_START_TOBLE) {
      if (resultCode == Activity.RESULT_OK) {
        var intent = Intent(this, TobleService::class.java)
        intent.setAction(TobleService.ACTION_START)
        intent.putExtra(TobleService.KEY_PEER_ADDR, "fe80:0:0:0:f3d9:2a82:c8d8:fe43")
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

  companion object {
    var REQUEST_CODE_COMMISSIONING = 0xB003
    var REQUEST_CODE_START_TOBLE = 0xB004
  }
}
