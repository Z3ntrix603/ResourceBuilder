package de.resourcebuilder.gui;

import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.pack.LivePackManager;
import de.resourcebuilder.pack.TextureVariant;
import de.resourcebuilder.preset.Preset;
import de.resourcebuilder.preset.PresetManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TextureStudioScreen extends Screen {

    private static final int WIN_WIDTH = 368;
    private static final int WIN_HEIGHT = 214;
    private static final int SIDEBAR_WIDTH = 112;
    private static final int HEADER_HEIGHT = 24;

    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 3;
    private static final int SLOTS_PER_PAGE = GRID_COLS * GRID_ROWS; // 18
    private static final int SLOT_SIZE = 34;
    private static final int SLOT_SPACING = 4;
    private static final int PREVIEW_SIZE = 24;

    // Modern Obsidian Dark UI Colors
    private static final int COLOR_BACKDROP = 0x85000000;
    private static final int COLOR_WIN_BG = 0xF4090E18;
    private static final int COLOR_WIN_HEADER = 0xFF0B1220;
    private static final int COLOR_WIN_BORDER = 0xFF1E293B;

    private static final int COLOR_SIDEBAR_BG = 0xFF080D16;
    private static final int COLOR_SIDEBAR_ITEM = 0xFF0F172A;
    private static final int COLOR_SIDEBAR_SELECTED = 0xFF1E293B;
    private static final int COLOR_CYAN = 0xFF00E5FF;
    private static final int COLOR_ACCENT = 0xFF38BDF8;

    private static final int COLOR_SLOT_BG = 0xFF0D1424;
    private static final int COLOR_SLOT_BORDER = 0xFF1C2840;
    private static final int COLOR_SLOT_HOVER = 0xFF2563EB;
    private static final int COLOR_SLOT_SELECTED = 0xFFFFFFFF;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY = 0xFF94A3B8;
    private static final int COLOR_SUCCESS = 0xFF4ADE80;

    private int activeItemIndex = 0;
    private int currentPage = 0;
    private String statusMessage = "Angewendet. Dein Mix ist gespeichert.";

    private boolean showPresetView = false;
    private boolean showSavePresetDialog = false;
    private EditBox presetNameField;

    private int sidebarScroll = 0;

    public TextureStudioScreen() {
        super(Component.literal("Texture Studio"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildUI();
    }

    private void rebuildUI() {
        this.clearWidgets();

        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        // Close button '✕'
        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(winX + WIN_WIDTH - 18, winY + 4, 14, 14).build());

        // Header Tab: Presets
        this.addRenderableWidget(Button.builder(Component.literal(showPresetView ? "← Items" : "Presets"), b -> {
            showPresetView = !showPresetView;
            showSavePresetDialog = false;
            rebuildUI();
        }).bounds(winX + WIN_WIDTH - 84, winY + 4, 62, 15).build());

        int contentX = winX + SIDEBAR_WIDTH + 10;
        int contentY = winY + HEADER_HEIGHT + 4;

        if (!showPresetView) {
            PvPItem currentItem = getCurrentItem();
            List<TextureVariant> variants = LivePackManager.getInstance().getVariants(currentItem);
            int totalPages = Math.max(1, (variants.size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);

            int pBtnY = contentY + 2;
            int rightAlign = winX + WIN_WIDTH - 10;

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    rebuildUI();
                }
            }).bounds(rightAlign - 14, pBtnY, 14, 14).build());

            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                if (currentPage > 0) {
                    currentPage--;
                    rebuildUI();
                }
            }).bounds(rightAlign - 30, pBtnY, 14, 14).build());

            // Bottom Buttons
            int bottomY = winY + WIN_HEIGHT - 22;

            this.addRenderableWidget(Button.builder(Component.literal("Preset speichern"), b -> {
                showSavePresetDialog = true;
                rebuildUI();
            }).bounds(contentX, bottomY, 110, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("Zurücksetzen"), b -> {
                LivePackManager.getInstance().resetAll();
                statusMessage = "Alle Texturen auf Standard zurückgesetzt.";
                rebuildUI();
            }).bounds(contentX + 115, bottomY, 82, 16).build());

        } else {
            // Presets view buttons
            int py = contentY + 18;
            List<Preset> presets = PresetManager.getInstance().getPresets();
            for (int i = 0; i < Math.min(presets.size(), 6); i++) {
                Preset p = presets.get(i);
                int btnY = py + i * 20;

                this.addRenderableWidget(Button.builder(Component.literal("Laden: " + truncate(p.getName(), 16)), b -> {
                    LivePackManager.getInstance().applyPreset(p);
                    statusMessage = "Preset '" + p.getName() + "' geladen!";
                    showPresetView = false;
                    rebuildUI();
                }).bounds(contentX, btnY, 195, 16).build());

                this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                    PresetManager.getInstance().deletePreset(p.getName());
                    rebuildUI();
                }).bounds(contentX + 198, btnY, 16, 16).build());
            }

            int bottomY = winY + WIN_HEIGHT - 22;
            this.addRenderableWidget(Button.builder(Component.literal("+ Aktuellen Mix speichern"), b -> {
                showSavePresetDialog = true;
                rebuildUI();
            }).bounds(contentX, bottomY, 160, 16).build());
        }

        // Save Preset Modal Dialog
        if (showSavePresetDialog) {
            int modalW = 190;
            int modalH = 75;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            presetNameField = new EditBox(this.font, modalX + 10, modalY + 22, modalW - 20, 16, Component.literal("Preset Name"));
            presetNameField.setValue("Mein PvP Mix");
            this.addRenderableWidget(presetNameField);

            this.addRenderableWidget(Button.builder(Component.literal("Speichern"), b -> {
                String name = presetNameField.getValue().trim();
                if (!name.isEmpty()) {
                    PresetManager.getInstance().savePreset(name, getCurrentSelectionsMap());
                    statusMessage = "Preset '" + name + "' gespeichert!";
                }
                showSavePresetDialog = false;
                rebuildUI();
            }).bounds(modalX + 10, modalY + 46, 80, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> {
                showSavePresetDialog = false;
                rebuildUI();
            }).bounds(modalX + 96, modalY + 46, 80, 16).build());
        }
    }

    private PvPItem getCurrentItem() {
        PvPItem[] items = PvPItem.values();
        if (activeItemIndex < 0 || activeItemIndex >= items.length) {
            activeItemIndex = 0;
        }
        return items[activeItemIndex];
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // Backdrop
        guiGraphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);

        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        // Clean Modern Rounded Window Frame
        drawRoundedBox(guiGraphics, winX, winY, WIN_WIDTH, WIN_HEIGHT, COLOR_WIN_BG, COLOR_WIN_BORDER);

        // Header Background
        guiGraphics.fill(winX + 2, winY + 2, winX + WIN_WIDTH - 2, winY + HEADER_HEIGHT, COLOR_WIN_HEADER);
        guiGraphics.fill(winX + 2, winY + HEADER_HEIGHT, winX + WIN_WIDTH - 2, winY + HEADER_HEIGHT + 1, COLOR_WIN_BORDER);

        // Header Title: Texture Studio (NO ugly gradient line!)
        guiGraphics.drawString(this.font, "Texture Studio", winX + 10, winY + 7, COLOR_TEXT_WHITE, true);

        // Sidebar Background
        int sidebarY = winY + HEADER_HEIGHT + 1;
        int sidebarH = WIN_HEIGHT - HEADER_HEIGHT - 3;
        guiGraphics.fill(winX + 2, sidebarY, winX + SIDEBAR_WIDTH, winY + WIN_HEIGHT - 2, COLOR_SIDEBAR_BG);
        guiGraphics.fill(winX + SIDEBAR_WIDTH, sidebarY, winX + SIDEBAR_WIDTH + 1, winY + WIN_HEIGHT - 2, COLOR_WIN_BORDER);

        // Sidebar Category Items
        PvPItem[] items = PvPItem.values();
        int itemH = 17;
        int maxVisible = (sidebarH - 6) / itemH;

        for (int i = 0; i < Math.min(items.length, maxVisible); i++) {
            int idx = i + sidebarScroll;
            if (idx >= items.length) break;

            PvPItem item = items[idx];
            int iy = sidebarY + 3 + i * itemH;
            boolean isSel = (idx == activeItemIndex) && !showPresetView;
            boolean isHov = mouseX >= winX + 2 && mouseX < winX + SIDEBAR_WIDTH - 2 && mouseY >= iy && mouseY < iy + itemH;

            int bg = isSel ? COLOR_SIDEBAR_SELECTED : (isHov ? COLOR_SIDEBAR_ITEM : COLOR_SIDEBAR_BG);
            drawRoundedBox(guiGraphics, winX + 2, iy, SIDEBAR_WIDTH - 4, itemH - 1, bg, isSel ? COLOR_ACCENT : COLOR_WIN_BORDER);

            String label = truncate((item.isSound() ? "♪ " : "") + item.getDisplayName(), 14);
            int textColor = isSel ? COLOR_TEXT_WHITE : (isHov ? COLOR_CYAN : COLOR_TEXT_GRAY);
            guiGraphics.drawString(this.font, label, winX + 8, iy + 4, textColor, isSel);
        }

        // Sidebar scrollbar indicator
        if (items.length > maxVisible) {
            int trackH = sidebarH - 8;
            int barH = Math.max(12, trackH * maxVisible / items.length);
            int barY = sidebarY + 4 + (trackH - barH) * sidebarScroll / (items.length - maxVisible);
            int barX = winX + SIDEBAR_WIDTH - 3;
            guiGraphics.fill(barX, barY, barX + 2, barY + barH, 0xFF33456B);
        }

        // Content Area
        int contentX = winX + SIDEBAR_WIDTH + 10;
        int contentY = winY + HEADER_HEIGHT + 4;

        if (!showPresetView) {
            PvPItem currentItem = getCurrentItem();
            guiGraphics.drawString(this.font, currentItem.getDisplayName(), contentX, contentY + 4, COLOR_TEXT_WHITE, true);

            List<TextureVariant> variants = LivePackManager.getInstance().getVariants(currentItem);
            int totalPages = Math.max(1, (variants.size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
            String pageStr = (currentPage + 1) + " / " + totalPages;
            int rightAlign = winX + WIN_WIDTH - 10;
            int pStrWidth = this.font.width(pageStr);
            guiGraphics.drawString(this.font, pageStr, rightAlign - 34 - pStrWidth, contentY + 5, COLOR_TEXT_GRAY, true);

            // Slot Grid
            int gridStartY = contentY + 18;
            int startIndex = currentPage * SLOTS_PER_PAGE;
            int endIndex = Math.min(variants.size(), startIndex + SLOTS_PER_PAGE);
            TextureVariant activeSel = LivePackManager.getInstance().getActiveSelection(currentItem);
            TextureVariant hoveredVariant = null;

            for (int i = startIndex; i < endIndex; i++) {
                TextureVariant v = variants.get(i);
                int slotIdx = i - startIndex;
                int col = slotIdx % GRID_COLS;
                int row = slotIdx / GRID_COLS;

                int sx = contentX + col * (SLOT_SIZE + SLOT_SPACING);
                int sy = gridStartY + row * (SLOT_SIZE + SLOT_SPACING);

                boolean isHov = mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= sy && mouseY < sy + SLOT_SIZE;
                boolean isSelected = activeSel != null && activeSel.getPackName().equals(v.getPackName());

                if (isHov) hoveredVariant = v;

                int borderColor = isSelected ? COLOR_SLOT_SELECTED : (isHov ? COLOR_SLOT_HOVER : COLOR_SLOT_BORDER);
                drawRoundedBox(guiGraphics, sx, sy, SLOT_SIZE, SLOT_SIZE, COLOR_SLOT_BG, borderColor);

                if (isSelected) {
                    // Double border highlight
                    guiGraphics.fill(sx + 2, sy + 2, sx + SLOT_SIZE - 2, sy + 3, COLOR_SLOT_SELECTED);
                    guiGraphics.fill(sx + 2, sy + SLOT_SIZE - 3, sx + SLOT_SIZE - 2, sy + SLOT_SIZE - 2, COLOR_SLOT_SELECTED);
                    guiGraphics.fill(sx + 2, sy + 3, sx + 3, sy + SLOT_SIZE - 3, COLOR_SLOT_SELECTED);
                    guiGraphics.fill(sx + SLOT_SIZE - 3, sy + 3, sx + SLOT_SIZE - 2, sy + SLOT_SIZE - 3, COLOR_SLOT_SELECTED);
                }

                // Render Preview (NO hearts!)
                int px = sx + (SLOT_SIZE - PREVIEW_SIZE) / 2;
                int py = sy + (SLOT_SIZE - PREVIEW_SIZE) / 2;

                if (v.isDefault()) {
                    guiGraphics.renderFakeItem(currentItem.getItemStack(), sx + (SLOT_SIZE - 16) / 2, sy + (SLOT_SIZE - 16) / 2);
                } else {
                    Identifier texId = v.getTextureId();
                    if (texId != null) {
                        guiGraphics.blit(texId, px, py, px + PREVIEW_SIZE, py + PREVIEW_SIZE, 0.0f, 1.0f, 0.0f, 1.0f);
                    } else {
                        guiGraphics.renderFakeItem(currentItem.getItemStack(), sx + (SLOT_SIZE - 16) / 2, sy + (SLOT_SIZE - 16) / 2);
                    }
                }

                if (currentItem.isSound()) {
                    guiGraphics.drawString(this.font, "♪", sx + SLOT_SIZE - 9, sy + 3, COLOR_CYAN, true);
                }
            }

            int statusY = winY + WIN_HEIGHT - 35;
            guiGraphics.drawString(this.font, statusMessage, contentX, statusY, COLOR_SUCCESS, true);

            super.render(guiGraphics, mouseX, mouseY, delta);

            // Tooltip
            if (hoveredVariant != null && !showSavePresetDialog) {
                String tip = hoveredVariant.isDefault() ? "Standard (Default Textur)" : hoveredVariant.getPackName();
                int tw = this.font.width(tip) + 8;
                int tx = Math.min(mouseX + 8, this.width - tw - 4);
                int ty = mouseY - 14;
                drawRoundedBox(guiGraphics, tx - 2, ty - 2, tw + 4, 15, 0xF2090E18, COLOR_CYAN);
                guiGraphics.drawString(this.font, tip, tx + 2, ty + 1, COLOR_TEXT_WHITE, true);
            }

        } else {
            guiGraphics.drawString(this.font, "Gespeicherte Presets", contentX, contentY + 4, COLOR_TEXT_WHITE, true);
            super.render(guiGraphics, mouseX, mouseY, delta);
        }

        if (showSavePresetDialog) {
            renderModalBox(guiGraphics, "Preset speichern", 190, 75);
        }
    }

    private void drawRoundedBox(GuiGraphics guiGraphics, int x, int y, int w, int h, int bg, int border) {
        guiGraphics.fill(x + 1, y, x + w - 1, y + h, bg);
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, bg);
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);

        guiGraphics.fill(x + 1, y, x + w - 1, y + 1, border);
        guiGraphics.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, border);
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
    }

    private void renderModalBox(GuiGraphics guiGraphics, String title, int w, int h) {
        int mx = (this.width - w) / 2;
        int my = (this.height - h) / 2;

        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);
        drawRoundedBox(guiGraphics, mx, my, w, h, 0xFF0F172A, COLOR_ACCENT);
        guiGraphics.drawString(this.font, title, mx + 10, my + 8, COLOR_TEXT_WHITE, true);
        super.render(guiGraphics, 0, 0, 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        double mouseX = event.x();
        double mouseY = event.y();

        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        int sidebarY = winY + HEADER_HEIGHT;
        if (mouseX >= winX && mouseX < winX + SIDEBAR_WIDTH && mouseY >= sidebarY && mouseY < winY + WIN_HEIGHT) {
            int itemH = 17;
            int clickedIdx = (int) (mouseY - (sidebarY + 3)) / itemH + sidebarScroll;
            if (clickedIdx >= 0 && clickedIdx < PvPItem.values().length) {
                activeItemIndex = clickedIdx;
                currentPage = 0;
                showPresetView = false;
                rebuildUI();
                return true;
            }
        }

        if (!showPresetView) {
            int contentX = winX + SIDEBAR_WIDTH + 10;
            int contentY = winY + HEADER_HEIGHT + 4;
            int gridStartY = contentY + 18;

            PvPItem currentItem = getCurrentItem();
            List<TextureVariant> variants = LivePackManager.getInstance().getVariants(currentItem);
            int startIndex = currentPage * SLOTS_PER_PAGE;
            int endIndex = Math.min(variants.size(), startIndex + SLOTS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                int slotIdx = i - startIndex;
                int col = slotIdx % GRID_COLS;
                int row = slotIdx / GRID_COLS;

                int sx = contentX + col * (SLOT_SIZE + SLOT_SPACING);
                int sy = gridStartY + row * (SLOT_SIZE + SLOT_SPACING);

                if (mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                    TextureVariant clicked = variants.get(i);
                    LivePackManager.getInstance().selectVariant(currentItem, clicked, true);
                    statusMessage = "Angewendet. Dein Mix ist gespeichert.";

                    if (currentItem.isSound() && this.minecraft != null) {
                        switch (currentItem) {
                            case SOUND_MACE -> this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.MACE_SMASH_GROUND, 1.0f));
                            case SOUND_CRYSTAL -> this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE, 1.0f));
                            case SOUND_SWORD -> this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f));
                            case SOUND_EAT -> this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_BURP, 1.0f));
                            case SOUND_TOTEM -> this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 1.0f));
                            default -> {}
                        }
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        if (mouseX >= winX && mouseX < winX + SIDEBAR_WIDTH) {
            sidebarScroll -= (int) Math.signum(verticalAmount);
            int maxScroll = Math.max(0, PvPItem.values().length - ((WIN_HEIGHT - HEADER_HEIGHT - 6) / 17));
            if (sidebarScroll < 0) sidebarScroll = 0;
            if (sidebarScroll > maxScroll) sidebarScroll = maxScroll;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (showSavePresetDialog) {
                showSavePresetDialog = false;
                rebuildUI();
                return true;
            }
            if (showPresetView) {
                showPresetView = false;
                rebuildUI();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(null);
        }
    }

    private java.util.Map<PvPItem, String> getCurrentSelectionsMap() {
        java.util.Map<PvPItem, String> map = new java.util.EnumMap<>(PvPItem.class);
        for (PvPItem item : PvPItem.values()) {
            TextureVariant v = LivePackManager.getInstance().getActiveSelection(item);
            if (v != null && !v.isDefault()) {
                map.put(item, v.getPackName());
            }
        }
        return map;
    }

    private String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 2) + "..";
    }
}
