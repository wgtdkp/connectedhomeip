package io.openthread.toble;

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
}
