package verify;

import de.resourcebuilder.gui.ColorPickerModel;

public final class ColorPickerSmoke {
  public static void run() {
    int[] colors = {0x000000, 0xffffff, 0xff0000, 0x62d9ff, 0x808080};
    for (int color : colors) {
      float[] hsv = ColorPickerModel.toHsv(color);
      int roundTrip = ColorPickerModel.fromHsv(hsv[0], hsv[1], hsv[2]);
      if (roundTrip != color) throw new AssertionError("HSV round trip: " + Integer.toHexString(color));
    }
    if (ColorPickerModel.parseHex("#62D9FF", 0) != 0x62d9ff) throw new AssertionError("Hex parse");
    if (ColorPickerModel.parseHex("nope", 0x123456) != 0x123456) throw new AssertionError("Invalid hex");
  }
}
