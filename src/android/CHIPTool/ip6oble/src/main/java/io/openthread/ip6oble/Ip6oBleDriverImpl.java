package io.openthread.ip6oble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the OpenThread platform driver required by IP6oBLE.
 *
 * Call Ip6oBleModule.setIp6oBleDriver to tell OpenThread to use this driver.
 *
 */
public class Ip6oBleDriverImpl extends Ip6oBleDriver {

  public static final UUID UUID_C1   = UUID.fromString("18ee2ef5-263d-4559-959f-4f9c429f9d11");
  public static final UUID UUID_C2   = UUID.fromString("18ee2ef5-263d-4559-959f-4f9c429f9d12");
  public static final UUID UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  public static final UUID UUID_IP6OBLE_SERVICE = UUID.fromString("0000fffb-0000-1000-8000-00805f9b34fb");

  public static final byte[] CCCD_INDICATE = new byte[] {0x02, 0x00};

  private static final String TAG = Ip6oBleDriverImpl.class.getSimpleName();

  private static final int DEFAULT_MTU = 251;

  private Context context;
  private Ip6oBleRunner ip6oBleRunner;

  private BluetoothManager bluetoothManager;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothGatt bluetoothGattClient;
  private BluetoothLeScanner leScanner;

  private int gattMtu = DEFAULT_MTU;
  private Ip6oBleConnection connection;
  private int connectionState = BluetoothProfile.STATE_DISCONNECTED;

  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  private ConcurrentLinkedDeque<byte[]> c1Queue = new ConcurrentLinkedDeque<>();

  public void finalize() {
    if (bluetoothGattClient != null) {

      if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
        disconnect(connection);
      } else {
        releaseConnection();
      }
    }
  }

  private String CharacteristicToString(BluetoothGattCharacteristic characteristic) {
    return String.format("(uuid=%s)", characteristic.getUuid().toString());
  }

  private String ServiceToString(BluetoothGattService service) {
    return String.format("(uuid=%s)", service.getUuid());
  }

  private void postDelayed(Runnable task, int delay, TimeUnit timeUnit) {
    executor.schedule(task, delay, timeUnit);
  }

  private void releaseConnection() {
    connectionState = BluetoothGatt.STATE_DISCONNECTED;
    connection = null;

    if (bluetoothGattClient != null) {
      bluetoothGattClient.close();
      bluetoothGattClient = null;
    }
  }

  private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      String deviceAddr = result.getDevice().getAddress();

      //Log.d(TAG, String.format("received scan response from: %s", deviceAddr));

      byte[] data = result.getScanRecord().getBytes();
      otBleAdvPacket advPacket = new otBleAdvPacket();

      advPacket.setSrcAddress(Ip6oBleUtils.ip6oBleAddrFromString(deviceAddr));
      advPacket.setData(Ip6oBleUtils.getByteArray(data).cast());
      advPacket.setLength(data.length);
      advPacket.setRssi((byte)result.getRssi());

      // TODO(wgtdkp): filter out devices we don't care.
      ip6oBleRunner.postTask(() -> onAdvReceived(otBleAdvType.OT_BLE_ADV_IND, advPacket));
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      Log.d(TAG, "received batched scan responses");
    }

    @Override
    public void onScanFailed(int errorCode) {
      Log.e(TAG, String.format("failed to start BLE scan: %d", errorCode));
    }
  };

  private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, String.format("unexpected GATT error: %d", status));

        ip6oBleRunner.postTask(() -> onDisconnected(connection));
        releaseConnection();
        return;
      }

      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.d(TAG, "connected to GATT server");

        int bondState = gatt.getDevice().getBondState();
        if (bondState == BluetoothDevice.BOND_NONE || bondState == BluetoothDevice.BOND_BONDED) {
          int delay = 0;
          if (bondState == BluetoothDevice.BOND_BONDED && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            delay = 1000;
          }
          postDelayed(() -> { gatt.discoverServices(); }, delay, TimeUnit.MILLISECONDS);
        } else if (bondState == BluetoothDevice.BOND_BONDING) {
          Log.i(TAG, "waiting for bonding to complete");
        }

        connectionState = BluetoothGatt.STATE_CONNECTED;
        ip6oBleRunner.postTask(() -> onConnected(connection));
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.d(TAG,"disconnected from GATT server");
        ip6oBleRunner.postTask(() -> onDisconnected(connection));
        releaseConnection();
      } else {
        Log.d(TAG, String.format("new connection state: %d", newState));
        connectionState = newState;
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      Log.d(TAG, "::onServicesDiscovered");

      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.e(TAG, "failed to discover services");
        return;
      }

      for (BluetoothGattService service : gatt.getServices()) {
        Log.i(TAG, String.format("discovered service: %s", ServiceToString(service)));
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
          Log.i(TAG, String.format("discovered characteristic: %s", CharacteristicToString(characteristic)));
          for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            Log.i(TAG, String.format("discovered descriptor: %s", descriptor));
          }
        }
      }

      gatt.requestMtu(gattMtu);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
        int status) {
      super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
        BluetoothGattCharacteristic characteristic, int status) {
      Log.d(TAG, "::onCharacteristicWrite");

      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.e(TAG,  String.format("failed to write C1 characteristic: %s", characteristic.toString()));
        return;
      }

      if (c1Queue.isEmpty()) {
        Log.e(TAG, "onCharacteristicWrite: c1Queue is empty!");
        return;
      }

      byte[] value = c1Queue.poll();
      ip6oBleRunner.postTask(() -> onC1WriteDone(connection));

      if (!c1Queue.isEmpty()) {
        value = c1Queue.peek();
        Log.d(TAG, String
            .format("onCharacteristicWrite: c1Write, (length=%d), (hex=%s)", value.length, Ip6oBleUtils
                .getHexString(value)));
        BluetoothGattCharacteristic c1 = bluetoothGattClient.getService(UUID_IP6OBLE_SERVICE)
            .getCharacteristic(UUID_C1);
        c1.setValue(value);

        bluetoothGattClient.writeCharacteristic(c1);
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
        BluetoothGattCharacteristic characteristic) {
      Log.d(TAG, String.format("::onCharacteristicChanged, (uuid=%s)", characteristic.getUuid()));

      byte[] value = characteristic.getValue();
      ip6oBleRunner
          .postTask(() -> onC2Notification(connection, Ip6oBleUtils.getByteArray(value).cast(), value.length));
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
        int status) {
      Log.d(TAG, "::onDescriptorRead");
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
        int status) {
      Log.d(TAG, String.format("::onDescriptorWrite, (uuid=%s), (status=%d)", descriptor.getUuid(), status));

      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.e(TAG, String.format("failed to write descriptor: %s", descriptor.toString()));
      }

      // onC2Subscribed(connection, subscribeC2);
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
      Log.d(TAG, "::onReliableWriteCompleted");
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
      Log.d(TAG, String.format("::onMtuChanged, (mtu=%d), (status=%d)", mtu, status));

      gattMtu = mtu;

      ip6oBleRunner.postTask(() -> onConnectionIsReady(connection));
    }
  };

  public Ip6oBleDriverImpl(Context context, Ip6oBleRunner ip6oBleRunner) {
    this.context = context;
    this.ip6oBleRunner = ip6oBleRunner;
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
    // TODO(wgtdkp):
  }

  @Override
  public Ip6oBleConnection createConnection(otBleAddress aPeerAddress, otBleConnectionConfig aConfig) {
    String peerAddr = Ip6oBleUtils.ip6oBleAddrToString(aPeerAddress);

    Log.d(TAG, String.format("connecting to device %s", peerAddr));

    if (connectionState == BluetoothGatt.STATE_CONNECTING) {
      Log.w(TAG, "we are already connecting, please wait...");
      return null;
    }

    if (connectionState == BluetoothGatt.STATE_CONNECTED) {
      Log.d(TAG, "already connected, using exiting connection");
      return connection;
    }

    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(peerAddr);
    if (device == null) {
      Log.w(TAG, String.format("cannot find the device: %s", peerAddr));
      return null;
    }

    Log.d(TAG, "create connection: " + Calendar.getInstance().getTimeInMillis());

    // device.createBond();
    bluetoothGattClient = device.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
    connection = new Ip6oBleConnection();
    connectionState = BluetoothProfile.STATE_CONNECTING;

    return connection;
  }

  @Override
  public void disconnect(Ip6oBleConnection aConn) {
    Log.d(TAG,  "::disconnect");

    if (bluetoothGattClient != null) {

      Log.d(TAG, "disconnect: " + Calendar.getInstance().getTimeInMillis());

      bluetoothGattClient.disconnect();
    } else {
      Log.w(TAG, "Bluetooth GATT client not initialized");
    }
  }

  @Override
  public int getMtu(Ip6oBleConnection aConn) {
    Log.d(TAG, String.format("::getMtu, (mtu=%d)", gattMtu));

    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
      Log.e(TAG, "trying to get MTU before connecting");
      return 0;
    }

    return gattMtu;
  }

  @Override
  public otError scanStart(int aInterval, int aWindow, boolean aActive) {
    Log.d(TAG,  String.format("start scanning: interval=%d, window=%d, active=%b", aInterval, aWindow, aActive));

    // TODO: remove the scanner.
    assert(false);

    if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
      Log.w(TAG, "starting scan when not disconnected");
      return otError.OT_ERROR_INVALID_STATE;
    }

    if (leScanner == null) {
      leScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
    settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

    leScanner.startScan(null, settingsBuilder.build(), scanCallback);

    return otError.OT_ERROR_NONE;
  }

  @Override
  public otError scanStop() {
    Log.d(TAG, "::scanStop");

    // TODO: remove the scanner.
    assert(false);

    BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();

    leScanner.stopScan(scanCallback);

    return otError.OT_ERROR_NONE;
  }

  @Override
  public otError c1Write(Ip6oBleConnection aConn, SWIGTYPE_p_unsigned_char aBuffer, int aLength) {
    Log.d(TAG, "::c1Write");

    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
      Log.e(TAG, "c1Write: not connected!");
      return otError.OT_ERROR_NONE;
    }

    byte[] value = Ip6oBleUtils.getByteArray(ByteArray.frompointer(aBuffer), aLength);

    if (!c1Queue.isEmpty())
    {
      Log.d(TAG, String.format("there is ongoing C1 write, queueing: %s", Ip6oBleUtils.getHexString(value)));
      c1Queue.offer(value);
      return otError.OT_ERROR_NONE;
    } else {
      Log.d(TAG, String
          .format("c1Write, (length=%d), (hex=%s)", value.length, Ip6oBleUtils.getHexString(value)));
      BluetoothGattCharacteristic c1 = bluetoothGattClient.getService(UUID_IP6OBLE_SERVICE)
          .getCharacteristic(UUID_C1);
      c1.setValue(value);

      bluetoothGattClient.writeCharacteristic(c1);
      c1Queue.offer(value);
      return otError.OT_ERROR_NONE;
    }
  }

  @Override
  public void c2Subscribe(Ip6oBleConnection aConn, boolean aSubscribe) {
    Log.d(TAG, "::c2Subscribe");

    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
      Log.e(TAG, "c2Subscribe: not connected!");
      return;
    }

    BluetoothGattCharacteristic c2 = bluetoothGattClient.getService(UUID_IP6OBLE_SERVICE).getCharacteristic(UUID_C2);

    setNotify(c2, aSubscribe);
  }

  public boolean setNotify(BluetoothGattCharacteristic characteristic, final boolean enable) {
    // Check if characteristic is valid
    if(characteristic == null) {
      Log.e(TAG, "ERROR: Characteristic is 'null', ignoring setNotify request");
      return false;
    }

    // Get the CCC Descriptor for the characteristic
    final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CCCD);
    if(descriptor == null) {
      Log.e(TAG, String.format("ERROR: Could not get CCC descriptor for characteristic %s", characteristic.getUuid()));
      return false;
    }

    // Check if characteristic has NOTIFY or INDICATE properties and set the correct byte value to be written
    byte[] value;
    int properties = characteristic.getProperties();
    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
      value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
      value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
    } else {
      Log.e(TAG, String.format("ERROR: Characteristic %s does not have notify or indicate property", characteristic.getUuid()));
      return false;
    }
    final byte[] finalValue = enable ? value : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

    Log.d(TAG, String.format("subscribe to notification, (value=%s)", Ip6oBleUtils.getHexString(finalValue)));

    descriptor.setValue(finalValue);
    bluetoothGattClient.setCharacteristicNotification(characteristic, enable);
    return bluetoothGattClient.writeDescriptor(descriptor);
  }

  @Override
  public otError advStart(otBleAdvConfig aConfig) {
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
  public otError c2Notificate(Ip6oBleConnection aConn, SWIGTYPE_p_unsigned_char aBuffer, int aLength) {
    Log.d(TAG, "::c2Notificate");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }
}
