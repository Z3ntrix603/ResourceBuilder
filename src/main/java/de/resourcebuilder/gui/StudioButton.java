package de.resourcebuilder.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

class StudioButton extends Button {
    private final Boolean selectedTab;

    StudioButton(int x, int y, int w, int h, String label, Runnable action) {
        this(x, y, w, h, label, null, action);
    }

    StudioButton(int x, int y, int w, int h, String label, Boolean selectedTab, Runnable action) {
        super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.selectedTab = selectedTab;
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {
        if (de.resourcebuilder.StudioSettings.S.clickSounds) super.playDownSound(sounds);
    }

    @Override
    protected void renderContents(GuiGraphics g, int mx, int my, float dt) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean selected = Boolean.TRUE.equals(selectedTab);
        if (isHoveredOrFocused() || selected || selectedTab == null) {
            int color = isHoveredOrFocused() ? 0x99516364 : selected ? 0x773e5854 : 0x663d5056;
            StudioScreen.rounded(g, x, y, x + w, y + h, 5, color);
        }
        var font = Minecraft.getInstance().font;
        boolean showHotbar = selectedTab != null && getMessage().getString().equals("Hotbar");
        int iconWidth = showHotbar ? 14 : 0;
        String label =
                font.plainSubstrByWidth(getMessage().getString(), Math.max(1, w - iconWidth - 4));
        int start = x + (w - font.width(label) - iconWidth) / 2;
        if (showHotbar) {
            g.renderOutline(start, y + (h - 6) / 2, 11, 6, TextureStudioScreen.MUTED);
            for (int i = 1; i <= 2; i++)
                g.fill(
                        start + 1 + i * 3,
                        y + (h - 6) / 2 + 1,
                        start + 2 + i * 3,
                        y + (h - 6) / 2 + 5,
                        TextureStudioScreen.MUTED);
        }
        g.drawString(
                font,
                label,
                start + iconWidth,
                y + (h - 8) / 2,
                selected ? TextureStudioScreen.ACCENT : active ? 0xffdfebe6 : 0xff829089,
                false);
    }
}
