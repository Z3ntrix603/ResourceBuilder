package de.resourcebuilder.gui;

import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.pack.*;
import de.resourcebuilder.preset.*;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

final class ProfilesScreen extends StudioScreen {
    private final Screen back;
    private int offset;
    private String draft = "", message = "";

    ProfilesScreen(Screen parent) {
        super(Component.literal("Profiles"));
        back = parent;
    }

    @Override
    protected void init() {
        int l = width / 2 - Math.min(width - 14, 370) / 2, w = Math.min(width - 14, 370), y = 32;
        addRenderableWidget(new StudioButton(l, y, 54, 18, "Back", this::onClose));
        EditBox name = new EditBox(font, l, y + 28, w - 90, 18, Component.literal("Name"));
        name.setHint(Component.literal("Profile name"));
        name.setValue(draft);
        name.setResponder(s -> draft = s);
        addRenderableWidget(name);
        var pm = PresetManager.getInstance();
        var pack = LivePackManager.getInstance();
        addRenderableWidget(
                new StudioButton(
                        l + w - 84,
                        y + 28,
                        84,
                        18,
                        "Save",
                        () -> {
                            if (draft.isBlank()) return;
                            Map<PvPItem, String> choices = new EnumMap<>(PvPItem.class);
                            for (var item : PvPItem.values())
                                choices.put(item, pack.getActiveSelection(item).getPackName());
                            pm.savePreset(draft.trim(), choices);
                            draft = "";
                            rebuildWidgets();
                        }));
        int count = Math.max(1, (height - 122) / 22);
        offset = Math.clamp(offset, 0, Math.max(0, pm.getPresets().size() - count));
        for (int i = offset; i < Math.min(pm.getPresets().size(), offset + count); i++) {
            Preset p = pm.getPresets().get(i);
            int row = y + 56 + (i - offset) * 22;
            addRenderableWidget(
                    new StudioButton(
                            l,
                            row,
                            w - 62,
                            19,
                            p.getName() + "  ·  " + p.size(),
                            () -> {
                                pack.applyPreset(p);
                                onClose();
                            }));
            addRenderableWidget(
                    new StudioButton(
                            l + w - 57,
                            row,
                            57,
                            19,
                            "Delete",
                            () -> {
                                pm.deletePreset(p.getName());
                                rebuildWidgets();
                            }));
        }
        addRenderableWidget(
                new StudioButton(
                        l,
                        height - 30,
                        w / 2 - 3,
                        18,
                        "Reset active mix",
                        () -> {
                            pack.resetAll();
                            onClose();
                        }));
        addRenderableWidget(
                new StudioButton(
                        l + w / 2 + 2,
                        height - 30,
                        w / 2 - 2,
                        18,
                        "Export mix",
                        () -> {
                            try {
                                var file = pack.exportMix();
                                net.minecraft.util.Util.getPlatform()
                                        .openFile(file.getParent().toFile());
                                message = file.getFileName().toString();
                            } catch (java.io.IOException ex) {
                                message = "Apply a style first";
                            }
                        }));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        offset -= (int) Math.signum(v);
        rebuildWidgets();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float dt) {
        int margin = (width - Math.min(width - 8, 386)) / 2;
        rounded(g, margin, 5, width - margin, height - 5, 8, 0xec192227);
        g.centeredText(font, "Profiles", width / 2, 15, TextureStudioScreen.TEXT);
        if (!message.isBlank())
            g.centeredText(font, message, width / 2, height - 44, TextureStudioScreen.MUTED);
        super.extractRenderState(g, x, y, dt);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(back);
    }
}
