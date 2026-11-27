package de.resourcebuilder.gui;

import java.util.function.IntConsumer;

final class StudioColorButton extends StudioButton {
  static final int[] PALETTE = {
    0xffffff, 0xff5b5b, 0xff78c8, 0xb48cff, 0x62d9ff, 0x6ee0a1, 0xffca62, 0x222222
  };

  StudioColorButton(
      int x, int y, int width, String label, int current, IntConsumer change, Runnable refresh) {
    super(
        x,
        y,
        width,
        18,
        label + " · #" + String.format("%06X", current & 0xffffff),
        () -> {
          int index = 0;
          for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == current) index = i;
          change.accept(PALETTE[(index + 1) % PALETTE.length]);
          refresh.run();
        });
  }
}
