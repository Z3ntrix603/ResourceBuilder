package de.resourcebuilder.gui;

import static de.resourcebuilder.StudioSettings.S;

import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VisualsScreen extends StudioScreen {
  public static final List<String> PAGE_LABELS =
      List.of("Global Color", "Particles", "Fire", "Glint");

  private final Screen back;
  private int page;
  private int left, right, top, bottom, nav, contentX, rowY;

  public VisualsScreen(Screen back) {
    super(Component.literal("Visuals"));
    this.back = back;
  }

  @Override
  protected void init() {
    int width = Math.min(this.width - 12, 720);
    int height = Math.min(this.height - 12, 480);
    left = (this.width - width) / 2;
    right = left + width;
    top = (this.height - height) / 2;
    bottom = top + height;
    nav = Math.clamp(width / 4, 120, 160);
    contentX = left + nav + 18;
    rowY = top + 72;

    addRenderableWidget(new StudioButton(left + 10, top + 10, 58, 18, "Back", this::onClose));
    for (int i = 0; i < PAGE_LABELS.size(); i++) {
      int selected = i;
      addRenderableWidget(
          new StudioButton(
              left + 10,
              top + 51 + i * 24,
              nav - 20,
              20,
              PAGE_LABELS.get(i),
              page == i,
              () -> {
                page = selected;
                rebuildWidgets();
              }));
    }
    addRenderableWidget(
        new StudioButton(
            right - 80,
            top + 10,
            70,
            18,
            "Reset",
            () -> {
              resetPage();
              S.save();
              rebuildWidgets();
            }));

    switch (page) {
      case 0 -> globalColor();
      case 1 -> particles();
      case 2 -> fire();
      case 3 -> glint();
    }
  }

  private void globalColor() {
    action("Enabled · " + (S.visual.colorEnabled ? "On" : "Off"), () -> S.visual.colorEnabled = !S.visual.colorEnabled);
    slider("Saturation", S.visual.saturation, 0, 2, value -> S.visual.saturation = value);
    slider("Contrast", S.visual.contrast, .5, 1.5, value -> S.visual.contrast = value);
    slider("Brightness", S.visual.brightness, .5, 1.5, value -> S.visual.brightness = value);
    slider("Gamma", S.visual.gamma, .5, 1.5, value -> S.visual.gamma = value);
    slider("Hue", S.visual.hue, -180, 180, value -> S.visual.hue = value);
  }

  private void particles() {
    color("Critical hits", S.visual.critColor, value -> S.visual.critColor = value);
    color("Totem pops", S.visual.totemColor, value -> S.visual.totemColor = value);
    color("Explosions", S.visual.explosionColor, value -> S.visual.explosionColor = value);
    color("Enchantment", S.visual.enchantmentColor, value -> S.visual.enchantmentColor = value);
    slider("Size", S.visual.particleSize, .25, 3, value -> S.visual.particleSize = value);
    slider("Amount", S.visual.particleAmount, 0, 3, value -> S.visual.particleAmount = value);
    slider("Lifetime", S.visual.particleLifetime, .25, 3, value -> S.visual.particleLifetime = value);
    action("Fullbright · " + (S.visual.particleFullbright ? "On" : "Off"), () -> S.visual.particleFullbright = !S.visual.particleFullbright);
  }

  private void fire() {
    color("Color", S.visual.fireColor, value -> S.visual.fireColor = value);
    slider("Height", S.visual.fireHeight, 0, 1, value -> S.visual.fireHeight = value);
    slider("Opacity", S.visual.fireOpacity, 0, 1, value -> S.visual.fireOpacity = value);
    slider("Speed", S.visual.fireSpeed, .25, 3, value -> S.visual.fireSpeed = value);
  }

  private void glint() {
    color("Color", S.visual.glintColor, value -> S.visual.glintColor = value);
    slider("Strength", S.visual.glintStrength, 0, 2, value -> S.visual.glintStrength = value);
    slider("Speed", S.visual.glintSpeed, .25, 3, value -> S.visual.glintSpeed = value);
    action(
        "Pattern · " + S.visual.glintPattern,
        () -> {
          String[] patterns = {"Vanilla", "Diagonal", "Pulse", "Soft"};
          int index = java.util.Arrays.asList(patterns).indexOf(S.visual.glintPattern);
          S.visual.glintPattern = patterns[(index + 1) % patterns.length];
        });
  }

  private void slider(String label, double value, double min, double max, DoubleConsumer change) {
    addRenderableWidget(
        new StudioSlider(
            contentX,
            rowY,
            right - contentX - 18,
            label,
            value,
            min,
            max,
            changed -> {
              change.accept(changed);
              S.visual.normalized();
              S.save();
            }));
    rowY += 25;
  }

  private void color(String label, int value, java.util.function.IntConsumer change) {
    addRenderableWidget(
        new StudioColorButton(
            contentX,
            rowY,
            right - contentX - 18,
            label,
            value,
            changed -> {
              change.accept(changed);
              S.save();
            },
            this::rebuildWidgets));
    rowY += 25;
  }

  private void action(String label, Runnable action) {
    addRenderableWidget(
        new StudioButton(
            contentX,
            rowY,
            right - contentX - 18,
            18,
            label,
            () -> {
              action.run();
              S.save();
              rebuildWidgets();
            }));
    rowY += 25;
  }

  private void resetPage() {
    var defaults = new de.resourcebuilder.profile.VisualProfile();
    switch (page) {
      case 0 -> {
        S.visual.colorEnabled = false;
        S.visual.saturation = defaults.saturation;
        S.visual.contrast = defaults.contrast;
        S.visual.brightness = defaults.brightness;
        S.visual.gamma = defaults.gamma;
        S.visual.hue = defaults.hue;
      }
      case 1 -> {
        S.visual.critColor = defaults.critColor;
        S.visual.totemColor = defaults.totemColor;
        S.visual.explosionColor = defaults.explosionColor;
        S.visual.enchantmentColor = defaults.enchantmentColor;
        S.visual.particleSize = defaults.particleSize;
        S.visual.particleAmount = defaults.particleAmount;
        S.visual.particleLifetime = defaults.particleLifetime;
        S.visual.particleFullbright = false;
      }
      case 2 -> {
        S.visual.fireColor = defaults.fireColor;
        S.visual.fireHeight = defaults.fireHeight;
        S.visual.fireOpacity = defaults.fireOpacity;
        S.visual.fireSpeed = defaults.fireSpeed;
      }
      case 3 -> {
        S.visual.glintColor = defaults.glintColor;
        S.visual.glintStrength = defaults.glintStrength;
        S.visual.glintSpeed = defaults.glintSpeed;
        S.visual.glintPattern = defaults.glintPattern;
      }
    }
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float dt) {
    rounded(graphics, left, top, right, bottom, 10, 0xec101a20);
    rounded(graphics, left + 5, top + 43, left + nav, bottom - 8, 7, 0x78152227);
    graphics.text(font, "Resource Builder", left + 80, top + 11, TextureStudioScreen.TEXT, false);
    graphics.text(font, "Visuals · " + PAGE_LABELS.get(page), contentX, top + 48, TextureStudioScreen.ACCENT, false);
    graphics.text(font, "Mod effect · live preview", contentX, bottom - 20, TextureStudioScreen.MUTED, false);
    super.extractRenderState(graphics, mx, my, dt);
  }

  @Override
  public void onClose() {
    S.save();
    minecraft.setScreenAndShow(back);
  }
}
