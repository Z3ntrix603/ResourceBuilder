package de.resourcebuilder.gui;

import de.resourcebuilder.StudioSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class MaceEffectCard extends StudioButton {
    static final String[] MODES = {"Off", "Shockwave", "Fracture", "Impact"};
    private final String mode;

    MaceEffectCard(int x, int y, int width, int height, String mode) {
        super(
                x,
                y,
                width,
                height,
                mode,
                () -> {
                    StudioSettings.S.maceEffect = mode;
                    StudioSettings.S.save();
                });
        this.mode = mode;
    }

    @Override
    protected void renderContents(GuiGraphics g, int mx, int my, float dt) {
        int x = getX(), y = getY(), right = x + getWidth(), bottom = y + getHeight();
        boolean selected = mode.equals(StudioSettings.S.maceEffect);
        int inset = selected ? 1 : 0;
        if (selected) StudioScreen.rounded(g, x, y, right, bottom, 7, TextureStudioScreen.ACCENT);
        StudioScreen.rounded(
                g,
                x + inset,
                y + inset,
                right - inset,
                bottom - inset,
                7 - inset,
                selected ? 0xff293f3d : isHoveredOrFocused() ? 0xda334649 : 0xa7213239);
        int cx = x + getWidth() / 2, cy = y + (getHeight() - 14) / 2;
        double phase = (System.nanoTime() % 1_300_000_000L) / 1_300_000_000d;
        switch (mode) {
            case "Shockwave" -> oval(g, cx, cy, 7 + (int) (phase * 15), 3 + (int) (phase * 5), 0xff78e6ef);
            case "Fracture" -> {
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    line(g, cx, cy, cx + (int) (20 * Math.cos(a)), cy + (int) (10 * Math.sin(a)), 0xffd88b3d);
                }
            }
            case "Impact" -> {
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI * 2 / 10;
                    int r = 8 + (int) (phase * 12);
                    g.fill(cx + (int) (r * Math.cos(a)), cy + (int) (r * Math.sin(a)), cx + (int) (r * Math.cos(a)) + 2, cy + (int) (r * Math.sin(a)) + 2, 0xffffc566);
                }
            }
            default -> line(g, cx - 8, cy + 8, cx + 8, cy - 8, TextureStudioScreen.MUTED);
        }
        var font = Minecraft.getInstance().font;
        g.drawString(font, mode, cx - font.width(mode) / 2, bottom - 15, TextureStudioScreen.TEXT, false);
        if (selected) g.drawString(font, "✓", x + 6, y + 5, TextureStudioScreen.ACCENT, false);
    }

    private static void oval(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
        int px = cx + rx, py = cy;
        for (int i = 1; i <= 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            int x = cx + (int) Math.round(rx * Math.cos(angle));
            int y = cy + (int) Math.round(ry * Math.sin(angle));
            line(g, px, py, x, y, color);
            px = x;
            py = y;
        }
    }

    private static void line(GuiGraphics g, int x, int y, int ex, int ey, int color) {
        int steps = Math.max(1, Math.max(Math.abs(ex - x), Math.abs(ey - y)));
        for (int i = 0; i <= steps; i++) {
            int px = x + (ex - x) * i / steps, py = y + (ey - y) * i / steps;
            g.fill(px, py, px + 2, py + 1, color);
        }
    }
}
