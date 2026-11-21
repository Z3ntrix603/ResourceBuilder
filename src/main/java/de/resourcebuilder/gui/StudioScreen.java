package de.resourcebuilder.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class StudioScreen extends Screen {
    StudioScreen(Component title) {
        super(title);
    }

    // Screen has already blurred the world in the preceding render stratum.
    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, height, 0x20081119);
    }

    static void rounded(
            GuiGraphicsExtractor g, int x, int y, int right, int bottom, int radius, int color) {
        int r = Math.min(radius, Math.min(right - x, bottom - y) / 2);
        if (r <= 0) {
            g.fill(x, y, right, bottom, color);
            return;
        }
        g.fill(x, y + r, right, bottom - r, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - .5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + row, right - inset, y + row + 1, color);
            g.fill(x + inset, bottom - row - 1, right - inset, bottom - row, color);
        }
    }
}
