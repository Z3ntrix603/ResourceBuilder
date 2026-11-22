package de.resourcebuilder.render;

import de.resourcebuilder.StudioSettings;
import de.resourcebuilder.mixin.GameRendererPostAccess;
import de.resourcebuilder.profile.VisualProfile;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class VisualRuntime {
  public static final Identifier COLOR_GRADE =
      Identifier.fromNamespaceAndPath("resourcebuilder", "color_grade");

  private VisualRuntime() {}

  public static int transformRgb(int rgb, VisualProfile profile) {
    if (!profile.colorEnabled) return rgb & 0xffffff;
    float[] hsb = Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
    hsb[0] = (float) Math.floorMod((long) (hsb[0] * 360 + profile.hue), 360) / 360f;
    hsb[1] = (float) Math.clamp(hsb[1] * profile.saturation, 0, 1);
    int adjusted = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    int result = 0;
    for (int shift : new int[] {16, 8, 0}) {
      double channel = (adjusted >> shift & 255) / 255d;
      channel = Math.pow(channel, 1 / profile.gamma);
      channel = ((channel - .5) * profile.contrast + .5) * profile.brightness;
      result |= (int) Math.round(Math.clamp(channel, 0, 1) * 255) << shift;
    }
    return result;
  }

  public static int particleColor(String type, VisualProfile profile) {
    return switch (type) {
      case "crit", "enchanted_hit" -> profile.critColor;
      case "totem_of_undying" -> profile.totemColor;
      case "explosion", "explosion_emitter" -> profile.explosionColor;
      case "enchant", "nautilus" -> profile.enchantmentColor;
      default -> 0xffffff;
    };
  }

  public static int extraCopies(double amount, double sample) {
    amount = Math.clamp(amount, 0, 3);
    int whole = (int) amount;
    int copies = Math.max(0, whole - 1);
    if (sample < amount - whole) copies++;
    return Math.min(2, copies);
  }

  public static void tick(Minecraft client) {
    var renderer = client.gameRenderer;
    var current = renderer.currentPostEffect();
    if (StudioSettings.S.visual.colorEnabled) {
      if (current == null) ((GameRendererPostAccess) renderer).rb$setPostEffect(COLOR_GRADE);
    } else if (COLOR_GRADE.equals(current)) {
      renderer.clearPostEffect();
    }
    client.options.glintStrength().set(Math.clamp(StudioSettings.S.visual.glintStrength / 2, 0, 1));
    client.options.glintSpeed().set(Math.clamp(StudioSettings.S.visual.glintSpeed / 2, 0, 1));
  }
}
