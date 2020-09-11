package io.openthread.toble;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class TobleUtils {

  public static ByteArray getByteArray(byte[] bytes) {
    return getByteArray(bytes, bytes.length);
  }

  public static ByteArray getByteArray(byte[] bytes, int length) {
    ByteArray result = new ByteArray(bytes.length);
    for (int i = 0; i < length; ++i) {
      result.setitem(i, bytes[i]);
    }
    return result;
  }

  public static byte[] getByteArray(ByteArray bytes, int length) {
    byte[] result = new byte[length];
    for (int i = 0; i < length; ++i) {
      result[i] = (byte) (bytes.getitem(i) & 0xff);
    }
    return result;
  }

  public static byte[] getByteArray(short[] bytes) {
    byte[] result = new byte[bytes.length];
    for (int i  = 0; i < bytes.length; ++i) {
      result[i] = (byte) (bytes[i] & 0xff);
    }
    return result;
  }

  public static short[] getShortArray(byte[] bytes) {
    short[] result = new short[bytes.length];
    for (int i  = 0; i < bytes.length; ++i) {
      result[i] = (short) bytes[i];
    }
    return result;
  }

  public static String getHexString(byte[] bytes) {
    return getHexString(bytes, bytes.length);
  }

  public static String getHexString(byte[] bytes, int length) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; ++i) {
      builder.append(String.format("%02X", bytes[i]));
    }
    return builder.toString();
  }

  public static String tobleAddrToString(otTobleAddress tobleAddr) {
    byte[] addr = getByteArray(tobleAddr.getAddress());
    return  String.format("%02X:%02X:%02X:%02X:%02X:%02X", addr[5], addr[4],
                          addr[3], addr[2], addr[1], addr[0]);
  }

  public static otTobleAddress tobleAddrFromString(String addr) {
    addr = addr.replace(":", "");
    byte[] buf = hexStringToByteArray(addr);

    buf = reverseByteArray(buf);

    otTobleAddress tobleAddr = new otTobleAddress();
    tobleAddr.setAddress(TobleUtils.getShortArray(buf));
    tobleAddr.setType(otTobleAddressType.OT_TOBLE_ADDRESS_TYPE_PUBLIC);

    return tobleAddr;
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }

  public static byte[] reverseByteArray(byte[] bytes) {
    byte[] result = new byte[bytes.length];
    for (int i = 0; i < bytes.length; ++i) {
      result[bytes.length-i-1] = bytes[i];
    }
    return result;
  }
}
