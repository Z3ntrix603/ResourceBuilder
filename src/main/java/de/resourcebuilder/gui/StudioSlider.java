package de.resourcebuilder.gui;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

final class StudioSlider extends AbstractSliderButton {
  private final String label;
  private final double min;
  private final double max;
  private final DoubleConsumer change;

  StudioSlider(
      int x,
      int y,
      int width,
      String label,
      double initial,
      double min,
      double max,
      DoubleConsumer change) {
    super(x, y, width, 18, Component.empty(), Math.clamp((initial - min) / (max - min), 0, 1));
    this.label = label;
    this.min = min;
    this.max = max;
    this.change = change;
    updateMessage();
  }

  double current() {
    return min + value * (max - min);
  }

  @Override
  protected void updateMessage() {
    setMessage(Component.literal(label + "  " + format(current())));
  }

  @Override
  protected void applyValue() {
    change.accept(current());
  }

  private String format(double number) {
    if (label.equals("Hue")) return Math.round(number) + "°";
    if (label.equals("Amount")) return String.format(Locale.ROOT, "%.1f×", number);
    return Math.round(number * 100) + "%";
  }
}
