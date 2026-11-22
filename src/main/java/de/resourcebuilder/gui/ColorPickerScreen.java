package de.resourcebuilder.gui;

import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarrationElementOutput;

final class ColorPickerScreen extends StudioScreen {
  private final Screen back;
  private final int original;
  private final IntConsumer change;
  private final Runnable refresh;
  private float hue, saturation, value;
  private int current;
  private boolean applied, syncing;
  private int left, top, right, bottom;
  private EditBox hex;

  ColorPickerScreen(Screen back, int color, IntConsumer change, Runnable refresh) {
    super(Component.literal("Color picker"));
    this.back = back;
    this.original = color & 0xffffff;
    this.current = original;
    this.change = change;
    this.refresh = refresh;
    float[] hsv = ColorPickerModel.toHsv(current);
    hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
  }

  @Override protected void init() {
    int w = Math.min(width - 16, 480), h = Math.min(height - 16, 330);
    left = (width - w) / 2; right = left + w; top = (height - h) / 2; bottom = top + h;
    int planeX = left + 22, planeY = top + 58, planeW = Math.max(160, w - 190), planeH = 150;
    addRenderableWidget(new ColorPlane(planeX, planeY, planeW, planeH));
    addRenderableWidget(new HueSlider(planeX, planeY + planeH + 12, planeW));
    hex = new EditBox(font, right - 140, top + 91, 112, 20, Component.literal("Hex color"));
    hex.setMaxLength(7); hex.setValue(format(current));
    hex.setResponder(text -> {
      if (syncing) return;
      int parsed = ColorPickerModel.parseHex(text, current);
      if (parsed != current || text.matches("#?[0-9a-fA-F]{6}")) setRgb(parsed, false);
    });
    addRenderableWidget(hex);
    int[] palette = StudioColorButton.PALETTE;
    for (int i = 0; i < palette.length; i++) {
      int color = palette[i], x = right - 140 + (i % 4) * 29, y = top + 132 + (i / 4) * 29;
      addRenderableWidget(new SwatchButton(x, y, color));
    }
    addRenderableWidget(new StudioButton(right - 140, bottom - 44, 54, 19, "Cancel", this::onClose));
    addRenderableWidget(new StudioButton(right - 80, bottom - 44, 54, 19, "Apply", () -> {
      applied = true; change.accept(current); refresh.run(); minecraft.setScreenAndShow(back);
    }));
  }

  private void setHsv(float h, float s, float v) {
    hue = h; saturation = s; value = v;
    setRgb(ColorPickerModel.fromHsv(hue, saturation, value), true);
  }

  private void setRgb(int rgb, boolean updateHex) {
    current = rgb & 0xffffff;
    if (!updateHex) {
      float[] hsv = ColorPickerModel.toHsv(current);
      hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
    }
    if (hex != null && updateHex) {
      syncing = true; hex.setValue(format(current)); syncing = false;
    }
    change.accept(current);
  }

  private static String format(int rgb) { return String.format("#%06X", rgb & 0xffffff); }

  @Override public void render(GuiGraphics g, int mx, int my, float dt) {
    rounded(g, left, top, right, bottom, 11, 0xf4101a20);
    g.drawString(font, "Choose color", left + 22, top + 20, TextureStudioScreen.TEXT, false);
    g.drawString(font, "Saturation / brightness", left + 22, top + 43, TextureStudioScreen.MUTED, false);
    g.drawString(font, "LIVE PREVIEW", right - 140, top + 43, TextureStudioScreen.ACCENT, false);
    rounded(g, right - 140, top + 56, right - 28, top + 82, 6, 0xff000000 | current);
    g.renderOutline(right - 140, top + 56, 112, 26, 0x80ffffff);
    g.drawString(font, "Hex", right - 140, top + 84, TextureStudioScreen.MUTED, false);
    g.drawString(font, format(current), right - 140, top + 196, TextureStudioScreen.TEXT, false);
    super.render(g, mx, my, dt);
  }

  @Override public void onClose() {
    if (!applied) change.accept(original);
    refresh.run(); minecraft.setScreenAndShow(back);
  }

  private final class ColorPlane extends AbstractWidget {
    ColorPlane(int x, int y, int w, int h) { super(x, y, w, h, Component.literal("Saturation and brightness")); }
    private void pick(double mx, double my) {
      setHsv(hue, (float) Math.clamp((mx - getX()) / width, 0, 1),
          1 - (float) Math.clamp((my - getY()) / height, 0, 1));
    }
    @Override public void onClick(MouseButtonEvent event, boolean twice) { pick(event.x(), event.y()); }
    @Override protected void onDrag(MouseButtonEvent event, double dx, double dy) { pick(event.x(), event.y()); }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
      int cols = 32, rows = 18;
      for (int yy = 0; yy < rows; yy++) for (int xx = 0; xx < cols; xx++) {
        int x1 = getX() + xx * width / cols, x2 = getX() + (xx + 1) * width / cols;
        int y1 = getY() + yy * height / rows, y2 = getY() + (yy + 1) * height / rows;
        g.fill(x1, y1, x2, y2, 0xff000000 | ColorPickerModel.fromHsv(hue,
            xx / (float) (cols - 1), 1 - yy / (float) (rows - 1)));
      }
      int cx = getX() + Math.round(saturation * width), cy = getY() + Math.round((1 - value) * height);
      g.renderOutline(cx - 3, cy - 3, 7, 7, 0xffffffff); g.renderOutline(cx - 2, cy - 2, 5, 5, 0xff000000);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
  }

  private final class HueSlider extends AbstractSliderButton {
    HueSlider(int x, int y, int w) { super(x, y, w, 18, Component.empty(), hue); updateMessage(); }
    @Override protected void updateMessage() { setMessage(Component.literal("Hue  " + Math.round(value * 360) + "°")); }
    @Override protected void applyValue() { setHsv((float) value, saturation, ColorPickerScreen.this.value); }
    @Override public void renderWidget(GuiGraphics g, int mx, int my, float dt) {
      for (int i = 0; i < 36; i++) g.fill(getX() + i * width / 36, getY(), getX() + (i + 1) * width / 36,
          getY() + height, 0xff000000 | ColorPickerModel.fromHsv(i / 36f, 1, 1));
      g.fill(getX() + (int) (value * (width - 4)), getY() - 1,
          getX() + (int) (value * (width - 4)) + 4, getY() + height + 1, 0xffffffff);
      g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 5, 0xff101418);
    }
  }

  private final class SwatchButton extends AbstractWidget {
    private final int color;
    SwatchButton(int x, int y, int color) { super(x, y, 23, 23, Component.literal(format(color))); this.color = color; }
    @Override public void onClick(MouseButtonEvent event, boolean twice) { setRgb(color, true); }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
      rounded(g, getX(), getY(), getX() + width, getY() + height, 5, 0xff000000 | color);
      if (isHoveredOrFocused()) g.renderOutline(getX(), getY(), width, height, 0xffffffff);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
  }
}
