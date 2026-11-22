package de.resourcebuilder.gui;

public final class ColorPickerModel {
  private ColorPickerModel() {}

  public static float[] toHsv(int rgb) {
    return java.awt.Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
  }

  public static int fromHsv(float hue, float saturation, float value) {
    hue = hue - (float) Math.floor(hue);
    return java.awt.Color.HSBtoRGB(hue, Math.clamp(saturation, 0, 1), Math.clamp(value, 0, 1))
        & 0xffffff;
  }

  public static int parseHex(String text, int fallback) {
    if (text == null) return fallback & 0xffffff;
    String value = text.strip();
    if (value.startsWith("#")) value = value.substring(1);
    if (!value.matches("[0-9a-fA-F]{6}")) return fallback & 0xffffff;
    return Integer.parseInt(value, 16);
  }
}
