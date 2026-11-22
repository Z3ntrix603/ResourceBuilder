package de.resourcebuilder;

import com.google.gson.Gson;
import de.resourcebuilder.category.PvPItem;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class StudioSettings {
  private static final Path FILE = Path.of("config", "resourcebuilder", "studio.json");
  public static final StudioSettings S = load();
  public boolean selfHit = true, fadeHit = true;
  public Set<String> hiddenParticles = new HashSet<>();
  public String killEffect = "Off";
  public String maceEffect = "Shockwave";
  public boolean finisherSounds = true;
  public boolean maceSounds = true;
  public boolean favoritesOnly,
      fullbright,
      clearWeather,
      zoomToggle,
      smoothZoom = true,
      scrollZoom = true,
      cinematic;
  public boolean animations = true, pauseMenu, clickSounds = true, targetIndicator;
  public double darken = .55, swingSpeed = 1, fireOpacity = .9;
  public double customHue = .55;
  public int hitMode, crosshairColor, theme, background, skyTime, swing, hitColor, particleMode;
  public double glow,
      hitStrength = .5,
      handSize = 1,
      groundSize = 1,
      handX,
      handY,
      handZ,
      fireHeight = 1,
      zoom = 4,
      opacity = .96,
      windowSize = 1,
      crosshairSize = 1;
  public Map<String, Double> handItems = new HashMap<>(), groundItems = new HashMap<>();
  public Set<String> favorites = new HashSet<>();
  public static final int[] HIT = {0, 0xff4444, 0xff7eba, 0x9981ff, 0x5cddff, 0x75e993, 0xffc65c};

  private static StudioSettings load() {
    if (Files.isRegularFile(FILE))
      try (var reader = Files.newBufferedReader(FILE)) {
        var settings = new Gson().fromJson(reader, StudioSettings.class);
        if (settings != null) return settings;
      } catch (Exception e) {
        ResourceBuilderMod.LOGGER.warn("Cannot read studio settings", e);
      }
    return new StudioSettings();
  }

  public int hitRgb() {
    return hitColor == 7
        ? java.awt.Color.HSBtoRGB((float) customHue, .85f, 1) & 0xffffff
        : HIT[hitColor];
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      Files.writeString(FILE, new Gson().toJson(this));
    } catch (IOException e) {
      ResourceBuilderMod.LOGGER.warn("Cannot save studio settings", e);
    }
  }

  public static String favorite(PvPItem item, String name) {
    return item.name() + "/" + name;
  }
}
