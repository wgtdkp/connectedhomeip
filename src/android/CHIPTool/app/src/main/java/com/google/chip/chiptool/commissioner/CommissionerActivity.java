package com.google.chip.chiptool.commissioner;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.chip.chiptool.CHIPToolActivity;
import com.google.chip.chiptool.R;
import com.google.chip.chiptool.setuppayloadscanner.CHIPDeviceInfo;

public class CommissionerActivity extends AppCompatActivity {

  private CHIPDeviceInfo deviceInfo;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.commissioner_activity);

    deviceInfo = getIntent().getExtras().getParcelable(Constants.KEY_DEVICE_INFO);
  }

  public CHIPDeviceInfo getDeviceInfo() {
    return deviceInfo;
  }

  public void finishCommissioning(int resultCode) {
    Intent resultIntent = new Intent();
    setResult(resultCode, resultIntent);
    finish();
  }
}
