package de.resourcebuilder.gui;

import de.resourcebuilder.StudioSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class KillEffectCard extends StudioButton {
    static final String[] MODES = {"Off", "Lightning", "Ring", "Soul", "Burst"};
    private final String mode;

    KillEffectCard(int x, int y, int width, int height, String mode) {
        super(
                x,
                y,
                width,
                height,
                mode,
                () -> {
                    StudioSettings.S.killEffect = mode;
                    StudioSettings.S.save();
                });
        this.mode = mode;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int x = getX(), y = getY(), right = x + getWidth(), bottom = y + getHeight();
        boolean selected = mode.equals(StudioSettings.S.killEffect);
        int inset = selected ? 1 : 0;
        if (selected) StudioScreen.rounded(g, x, y, right, bottom, 7, TextureStudioScreen.ACCENT);
        int fill = selected ? 0xff293f3d : isHoveredOrFocused() ? 0xda334649 : 0xa7213239;
        StudioScreen.rounded(
                g, x + inset, y + inset, right - inset, bottom - inset, 7 - inset, fill);
        int cx = x + getWidth() / 2, cy = y + (getHeight() - 14) / 2;
        int scale = Math.max(4, Math.min(20, (getHeight() - 22) / 3));
        double phase =
                StudioSettings.S.animations
                        ? (System.nanoTime() % 1_600_000_000L) / 1_600_000_000d
                        : .6;
        switch (mode) {
            case "Lightning" -> {
                int color = phase < .17 ? 0xffffffff : 0xffb9e6ff;
                stroke(g, cx + scale / 3, cy - scale, cx - scale / 3, cy, color);
                stroke(g, cx - scale / 3, cy, cx + scale / 3, cy - 2, color);
                stroke(g, cx + scale / 3, cy - 2, cx - scale / 4, cy + scale, color);
            }
            case "Soul" -> {
                int lift = (int)(phase*scale);
                oval(g,cx,cy-lift,Math.max(3,scale/3),Math.max(4,scale/2),0xffa7f7ec);
                stroke(g,cx-2,cy-lift+scale/2,cx-5,cy+scale/2,0xff74bcce);
                g.fill(cx-3,cy-lift-2,cx-1,cy-lift,0xff142b34);
                g.fill(cx+2,cy-lift-2,cx+4,cy-lift,0xff142b34);
            }
            case "Burst" -> {
                for(int i=0;i<8;i++) {
                    double angle=i*Math.PI/4;
                    int radius=(int)(scale*(.4+phase));
                    int sx=cx+(int)(radius*Math.cos(angle)), sy=cy+(int)(radius*Math.sin(angle));
                    stroke(g,sx,sy,sx+(int)(4*Math.cos(angle)),sy+(int)(4*Math.sin(angle)),0xffffbe69);
                }
            }
            case "Ring" -> {
                int radius = (int) (scale * (.45 + phase));
                int alpha = (int) (250 - 140 * phase);
                oval(g, cx, cy, radius, Math.max(3, radius / 3), (alpha << 24) | 0x91e8d0);
            }
            default -> {
                int r = Math.max(3, scale / 2);
                oval(g, cx, cy, r, r, TextureStudioScreen.MUTED);
                stroke(
                        g,
                        cx - r * 2 / 3,
                        cy + r * 2 / 3,
                        cx + r * 2 / 3,
                        cy - r * 2 / 3,
                        TextureStudioScreen.MUTED);
            }
        }
        var font = Minecraft.getInstance().font;
        g.text(font, mode, cx - font.width(mode) / 2, bottom - 15, TextureStudioScreen.TEXT, false);
        if (selected) g.text(font, "✓", x + 6, y + 5, TextureStudioScreen.ACCENT, false);
    }

    private static void oval(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
        int px = cx + rx, py = cy;
        for (int i = 1; i <= 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            int x = cx + (int) Math.round(rx * Math.cos(angle)),
                    y = cy + (int) Math.round(ry * Math.sin(angle));
            stroke(g, px, py, x, y, color);
            px = x;
            py = y;
        }
    }

    private static void stroke(GuiGraphicsExtractor g, int x, int y, int ex, int ey, int color) {
        int steps = Math.max(1, Math.max(Math.abs(ex - x), Math.abs(ey - y)));
        for (int i = 0; i <= steps; i++) {
            int px = x + (ex - x) * i / steps, py = y + (ey - y) * i / steps;
            g.fill(px, py, px + 2, py + 1, color);
        }
    }
}
