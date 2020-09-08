package io.openthread.toble;

import android.util.Log;

/**
 * This class implements the OpenThread platform driver required by ToBLE.
 *
 * Call TobleModule.setTobleDriver to tell OpenThread to use this driver.
 *
 */
public class TobleDriverImpl extends TobleDriver {

  private static final String TAG = TobleDriverImpl.class.getSimpleName();

  @Override
  public void init() {
    Log.d(TAG, "::init");

    // TODO(wgtdkp):
  }

  @Override
  public void process() {
    Log.d(TAG, "::process");

    // TODO(wgtdkp):
  }

  @Override
  public void disconnect(TobleConnection aConn) {
    Log.d(TAG,  "disconnect");

    // TODO(wgtdkp):
  }

  @Override
  public int getMtu(TobleConnection aConn) {
    Log.d(TAG, "::getMtu");

    // TODO(wgtdkp):
    return 0;
  }

  @Override
  public otError scanStart(int aInterval, int aWindow, boolean aActive) {
    Log.d(TAG, "::scanStart");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }

  @Override
  public otError scanStop() {
    Log.d(TAG, "::scanStop");

    // TODO(wgtdkp):
    return otError.OT_ERROR_NOT_IMPLEMENTED;
  }

  @Override
  public TobleConnection createConnection(otTobleAddress aPeerAddress, otTobleConnectionConfig aConfig) {
    Log.d(TAG, "::createConnection");

    // TODO(wgtdkp):
    return null;
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
