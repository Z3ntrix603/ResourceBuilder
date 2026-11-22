package de.resourcebuilder.profile;

public final class VisualProfile {
  public boolean colorEnabled;
  public double saturation = 1;
  public double contrast = 1;
  public double brightness = 1;
  public double gamma = 1;
  public double hue;

  public int critColor = 0xffffff;
  public int totemColor = 0xffffff;
  public int explosionColor = 0xffffff;
  public int enchantmentColor = 0xa875ff;
  public int finisherColor = 0x78e6ef;
  public boolean particleColorsEnabled;
  public double particleSize = 1;
  public double particleAmount = 1;
  public double particleLifetime = 1;
  public boolean particleFullbright;

  public double fireHeight = 1;
  public double fireOpacity = .9;
  public int fireColor = 0xffffff;
  public double fireSpeed = 1;

  public int glintColor = 0x9a5cc6;
  public double glintStrength = 1;
  public double glintSpeed = 1;
  public String glintPattern = "Vanilla";

  public boolean inventoryHighlight = true;
  public int inventoryHighlightColor = 0x64e6b5;
  public double inventoryHighlightOpacity = .35;

  public int sunColor = 0xffffff;
  public int moonColor = 0xffffff;
  public int starColor = 0xffffff;
  public int cloudColor = 0xffffff;
  public int fogColor = 0xffffff;
  public double starBrightness = 1;
  public double cloudOpacity = 1;
  public String skyImage = "";
  public String panoramaImage = "";
  public String menuImage = "";
  public int menuTint = 0x101a20;
  public double menuOpacity = .8;
  public double menuBlur = 1;

  public VisualProfile normalized() {
    saturation = clamp(saturation, 0, 2);
    contrast = clamp(contrast, .5, 1.5);
    brightness = clamp(brightness, .5, 1.5);
    gamma = clamp(gamma, .5, 1.5);
    hue = clamp(hue, -180, 180);
    particleSize = clamp(particleSize, .25, 3);
    particleAmount = clamp(particleAmount, 0, 3);
    particleLifetime = clamp(particleLifetime, .25, 3);
    fireHeight = clamp(fireHeight, 0, 1);
    fireOpacity = clamp(fireOpacity, 0, 1);
    fireSpeed = clamp(fireSpeed, .25, 3);
    glintStrength = clamp(glintStrength, 0, 2);
    glintSpeed = clamp(glintSpeed, .25, 3);
    inventoryHighlightOpacity = clamp(inventoryHighlightOpacity, 0, 1);
    starBrightness = clamp(starBrightness, 0, 2);
    cloudOpacity = clamp(cloudOpacity, 0, 1);
    menuOpacity = clamp(menuOpacity, 0, 1);
    menuBlur = clamp(menuBlur, 0, 2);
    critColor = rgb(critColor);
    totemColor = rgb(totemColor);
    explosionColor = rgb(explosionColor);
    enchantmentColor = rgb(enchantmentColor);
    finisherColor = rgb(finisherColor);
    fireColor = rgb(fireColor);
    glintColor = rgb(glintColor);
    inventoryHighlightColor = rgb(inventoryHighlightColor);
    sunColor = rgb(sunColor);
    moonColor = rgb(moonColor);
    starColor = rgb(starColor);
    cloudColor = rgb(cloudColor);
    fogColor = rgb(fogColor);
    menuTint = rgb(menuTint);
    if (glintPattern == null || glintPattern.length() > 32) glintPattern = "Vanilla";
    skyImage = safePath(skyImage);
    panoramaImage = safePath(panoramaImage);
    menuImage = safePath(menuImage);
    return this;
  }

  private static double clamp(double value, double min, double max) {
    return Double.isFinite(value) ? Math.clamp(value, min, max) : min;
  }

  private static int rgb(int value) {
    return value & 0xffffff;
  }

  private static String safePath(String value) {
    return value == null || value.length() > 512 ? "" : value;
  }
}
