package io.openthread.toble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

/**
 * This class implements the OpenThread platform driver required by ToBLE.
 *
 * Call TobleModule.setTobleDriver to tell OpenThread to use this driver.
 *
 */
public class TobleDriverImpl extends TobleDriver {

  private static final String TAG = TobleDriverImpl.class.getSimpleName();

  private static final int GATT_MTU = 1024;

  private Context context;
  private TobleRunner tobleRunner;

  private BluetoothManager bluetoothManager;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothGatt bluetoothGattClient;

  private int gattMtu = GATT_MTU;
  private TobleConnection connection;
  private int connectionState = BluetoothProfile.STATE_DISCONNECTED;

  private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      Log.d(TAG, "");
      byte[] data = result.getScanRecord().getBytes();

      otTobleAdvPacket advPacket = new otTobleAdvPacket();
      result.getDevice().getAddress()

      advPacket.setSrcAddress();
      advPacket.setData(TobleUtils.getByteArray(data).cast());
      advPacket.setLength(data.length);
      advPacket.setRssi((byte)result.getRssi());
    }
  };

  private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.d(TAG, "connected to GATT server");

        gatt.requestMtu(gattMtu);

        tobleRunner.postTask(() -> onConnected(connection));
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.d(TAG,"disconnected from GATT server");

        tobleRunner.postTask(() -> onDisconnected(connection));
      }

      connectionState = newState;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      super.onServicesDiscovered(gatt, status);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
        int status) {
      super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
        BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
        BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
        int status) {
      super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
        int status) {
      super.onDescriptorWrite(gatt, descriptor, status);
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
      super.onReliableWriteCompleted(gatt, status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
      gattMtu = mtu;

      tobleRunner.postTask(() -> onConnectionIsReady(connection));
    }
  };

  public TobleDriverImpl(Context context, TobleRunner tobleRunner) {
    this.context = context;
    this.tobleRunner = tobleRunner;
  }

  @Override
  public void init() {
    Log.d(TAG, "::init");

    if (bluetoothManager == null) {
      bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
      if (bluetoothManager == null) {
        Log.e(TAG, "cannot initialize BluetoothManager");
        return;
      }
    }

    bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter == null) {
      Log.e(TAG,  "cannot initialize a BluetoothAdapter ");
      return;
    }
  }

  @Override
  public void process() {
    Log.d(TAG, "::process");

    // TODO(wgtdkp):
  }

  @Override
  public TobleConnection createConnection(otTobleAddress aPeerAddress, otTobleConnectionConfig aConfig) {
    String peerAddr = TobleUtils.tobleAddrToString(aPeerAddress);

    Log.d(TAG, String.format("connecting to device %s", peerAddr));

    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(peerAddr);
    if (device == null) {
      Log.w(TAG, String.format("cannot find the device: %s", peerAddr));
      return null;
    }

    bluetoothGattClient = device.connectGatt(context, false, bluetoothGattCallback);
    connection = new TobleConnection();
    connectionState = BluetoothProfile.STATE_CONNECTING;

    return connection;
  }

  @Override
  public void disconnect(TobleConnection aConn) {
    Log.d(TAG,  "::disconnect");

    if (bluetoothGattClient != null) {
      bluetoothGattClient.disconnect();
    } else {
      Log.w(TAG, "Bluetooth GATT client not initialized");
    }
  }

  @Override
  public int getMtu(TobleConnection aConn) {
    Log.d(TAG, "::getMtu");

    return gattMtu;
  }

  @Override
  public otError scanStart(int aInterval, int aWindow, boolean aActive) {
    Log.d(TAG, "::scanStart");

    if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
      Log.w(TAG, "starting scan when not disconnected");
      return otError.OT_ERROR_INVALID_STATE;
    }

    BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();

    ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
    settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

    leScanner.startScan(null, settingsBuilder.build(), scanCallback);

    return otError.OT_ERROR_NONE;
  }

  @Override
  public otError scanStop() {
    Log.d(TAG, "::scanStop");

    BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();

    leScanner.stopScan(scanCallback);

    return otError.OT_ERROR_NONE;
  }

  @Override
  public otError c1Write(TobleConnection aConn, SWIGTYPE_p_unsigned_char aBuffer, int aLength) {
    Log.d(TAG, "::c1Write");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }

  @Override
  public void c2Subscribe(TobleConnection aConn, boolean aSubscribe) {
    Log.d(TAG, "::c2Subscribe");

    // TODO(wgtdkp):
  }

  @Override
  public otError advStart(otTobleAdvConfig aConfig) {
    Log.d(TAG, "::advStart");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }

  @Override
  public otError advStop() {
    Log.d(TAG, "::advStop");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }

  @Override
  public otError c2Notificate(TobleConnection aConn, SWIGTYPE_p_unsigned_char aBuffer, int aLength) {
    Log.d(TAG, "::c2Notificate");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }
}
