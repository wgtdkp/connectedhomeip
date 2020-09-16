package com.google.chip.chiptool.blescanner.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by Android template
 * wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class CHIPBleDeviceContainer {

  /**
   * An array of sample (dummy) items.
   */
  public static final List<CHIPBleDeviceInfo> ITEMS = new ArrayList<CHIPBleDeviceInfo>();

  /**
   * A map of sample (dummy) items, by ID.
   */
  public static final Map<String, CHIPBleDeviceInfo> ITEM_MAP = new HashMap<String, CHIPBleDeviceInfo>();

  private static final int COUNT = 25;

  static {
    // Add some sample items.
    for (int i = 1; i <= COUNT; i++) {
      addItem(createDummyItem(i));
    }
  }

  private static void addItem(CHIPBleDeviceInfo item) {
    ITEMS.add(item);
    ITEM_MAP.put(item.id, item);
  }

  private static CHIPBleDeviceInfo createDummyItem(int position) {
    return new CHIPBleDeviceInfo(String.valueOf(position), "Item " + position, makeDetails(position));
  }

  private static String makeDetails(int position) {
    StringBuilder builder = new StringBuilder();
    builder.append("Details about Item: ").append(position);
    for (int i = 0; i < position; i++) {
      builder.append("\nMore details information here.");
    }
    return builder.toString();
  }

  /**
   * A dummy item representing a piece of content.
   */
  public static class CHIPBleDeviceInfo {

    public final String id;
    public final String content;
    public final String details;

    public CHIPBleDeviceInfo(String id, String content, String details) {
      this.id = id;
      this.content = content;
      this.details = details;
    }

    @Override
    public String toString() {
      return content;
    }
  }
}