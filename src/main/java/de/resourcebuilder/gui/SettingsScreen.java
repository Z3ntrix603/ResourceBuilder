package de.resourcebuilder.gui;

import static de.resourcebuilder.StudioSettings.S;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.*;

final class SettingsScreen extends StudioScreen {
    private static final String[] PAGES = {
        "Hit color", "Crystal glow", "Swing animation", "Held items", "Ground items",
        "Item position", "Enchant glint", "Zoom", "Crosshair", "Fullbright",
        "Weather & time", "Particles", "Low fire", "Controls", "Window"
    };
    private final Screen back;
    private final PreviewCache preview = new PreviewCache();
    private int page, offset, firstItem, navRows, l, r, nav, x, y, w;
    private String needle = "", feedback = "";

    SettingsScreen(Screen back) {
        super(Component.literal("Settings"));
        this.back = back;
    }

    @Override
    protected void init() {
        int panel = Math.min(width - 12, 620);
        l = (width - panel) / 2;
        r = l + panel;
        nav = Math.min(125, panel / 3);
        x = l + nav + 10;
        w = r - x - 7;
        y = 57;
        navRows = Math.max(1, (height - 56) / 20);
        addRenderableWidget(new StudioButton(l + 4, 10, 58, 18, "Back", this::onClose));
        addRenderableWidget(new StudioButton(r - 70, 10, 63, 18, "Reset", this::resetPage));
        offset = Math.clamp(offset, 0, Math.max(0, PAGES.length - navRows));
        for (int i = offset; i < Math.min(PAGES.length, offset + navRows); i++) {
            int key = i;
            addRenderableWidget(
                    new StudioButton(
                            l + 4,
                            37 + (i - offset) * 20,
                            nav - 9,
                            18,
                            (page == i ? "› " : "") + PAGES[i],
                            () -> {
                                page = key;
                                firstItem = 0;
                                needle = "";
                                rebuildWidgets();
                            }));
        }
        switch (page) {
            case 0 -> {
                String[] colors = {
                    "Default", "Ruby", "Rose", "Lilac", "Ice", "Mint", "Gold", "Custom"
                };
                action(
                        "Color · " + colors[S.hitColor],
                        () -> S.hitColor = (S.hitColor + 1) % colors.length);
                if (S.hitColor == 7) slider("Hue", S.customHue, 0, 1, v -> S.customHue = v);
                action(
                        "Apply to · " + new String[] {"Everything", "Skin", "Armor"}[S.hitMode],
                        () -> S.hitMode = (S.hitMode + 1) % 3);
                toggle("Fade smoothly", S.fadeHit, () -> S.fadeHit = !S.fadeHit);
                toggle("Include self", S.selfHit, () -> S.selfHit = !S.selfHit);
                slider("Strength", S.hitStrength, 0, 1, v -> S.hitStrength = v);
            }
            case 1 -> slider("Glow strength", S.glow, 0, 1, v -> S.glow = v);
            case 2 -> {
                action(
                        "Swing · "
                                + new String[] {
                                            "Vanilla", "Smooth", "Slash", "Stab", "Spin", "Light"
                                        }
                                        [S.swing],
                        () -> S.swing = (S.swing + 1) % 6);
                slider("Speed", S.swingSpeed, .25, 2, v -> S.swingSpeed = v);
            }
            case 3, 4 -> sizes(page == 3);
            case 5 -> {
                slider("X", S.handX, -1, 1, v -> S.handX = v);
                slider("Y", S.handY, -1, 1, v -> S.handY = v);
                slider("Z", S.handZ, -1, 1, v -> S.handZ = v);
            }
            case 6 -> {
                var opt = minecraft.options;
                slider(
                        "Strength",
                        opt.glintStrength().get(),
                        0,
                        1,
                        v -> opt.glintStrength().set(v));
                slider("Speed", opt.glintSpeed().get(), 0, 1, v -> opt.glintSpeed().set(v));
            }
            case 7 -> {
                action(
                        "Mode · " + (S.zoomToggle ? "Toggle" : "Hold"),
                        () -> S.zoomToggle = !S.zoomToggle);
                slider("Zoom ×", S.zoom, 1, 12, v -> S.zoom = v);
                toggle("Smooth zoom", S.smoothZoom, () -> S.smoothZoom = !S.smoothZoom);
                toggle("Scroll to zoom", S.scrollZoom, () -> S.scrollZoom = !S.scrollZoom);
                toggle("Cinematic camera", S.cinematic, () -> S.cinematic = !S.cinematic);
            }
            case 8 -> {
                slider("Size", S.crosshairSize, .25, 2.5, v -> S.crosshairSize = v);
                action(
                        "Color · "
                                + new String[] {
                                            "Vanilla", "Ruby", "Rose", "Lilac", "Ice", "Mint",
                                            "Gold"
                                        }
                                        [S.crosshairColor],
                        () -> S.crosshairColor = (S.crosshairColor + 1) % 7);
                toggle(
                        "Target indicator",
                        S.targetIndicator,
                        () -> S.targetIndicator = !S.targetIndicator);
            }
            case 9 -> toggle("Fullbright", S.fullbright, () -> S.fullbright = !S.fullbright);
            case 10 -> {
                toggle("Clear weather", S.clearWeather, () -> S.clearWeather = !S.clearWeather);
                action(
                        "Time · " + new String[] {"Normal", "Day", "Night"}[S.skyTime],
                        () -> S.skyTime = (S.skyTime + 1) % 3);
            }
            case 11 -> {
                action(
                        "Particles · " + minecraft.options.particles().get(),
                        () -> {
                            var opts = net.minecraft.server.level.ParticleStatus.values();
                            var current = minecraft.options.particles();
                            current.set(opts[(current.get().ordinal() + 1) % opts.length]);
                        });
                for (String p :
                        List.of(
                                "crit",
                                "enchanted_hit",
                                "explosion",
                                "totem_of_undying",
                                "smoke",
                                "poof")) {
                    toggle(
                            p,
                            !S.hiddenParticles.contains(p),
                            () -> {
                                if (!S.hiddenParticles.remove(p)) S.hiddenParticles.add(p);
                            });
                }
            }
            case 12 -> {
                slider("Fire height", S.fireHeight, 0, 1, v -> S.fireHeight = v);
                slider("Opacity", S.fireOpacity, 0, 1, v -> S.fireOpacity = v);
            }
            case 13 -> {
                action(
                        "Open key bindings",
                        () -> minecraft.setScreen(new KeyBindsScreen(this, minecraft.options)));
                toggle("Rotate 3D previews", S.animations, () -> S.animations = !S.animations);
                toggle("Click sounds", S.clickSounds, () -> S.clickSounds = !S.clickSounds);
                toggle("Pause singleplayer", S.pauseMenu, () -> S.pauseMenu = !S.pauseMenu);
            }
            case 14 -> {
                action(
                        "Window theme · " + new String[] {"Graphite", "Plum", "Slate"}[S.theme],
                        () -> S.theme = (S.theme + 1) % 3);
                action(
                        "Background · "
                                + new String[] {"Game", "Sunset", "Night", "Aurora", "Custom"}
                                        [S.background],
                        () ->
                                S.background =
                                        (S.background + 1)
                                                % (java.nio.file.Files.exists(StudioBackdrop.CUSTOM)
                                                        ? 5
                                                        : 4));
                slider("Opacity", S.opacity, .45, 1, v -> S.opacity = v);
                slider("Darken background", S.darken, 0, 1, v -> S.darken = v);
                slider("Menu size", S.windowSize, .76, 1, v -> S.windowSize = v);
            }
        }
    }

    private void resetPage() {
        switch (page) {
            case 0 -> {
                S.hitColor = 0;
                S.hitMode = 0;
                S.hitStrength = .5;
                S.fadeHit = true;
                S.selfHit = true;
            }
            case 1 -> S.glow = 0;
            case 2 -> {
                S.swing = 0;
                S.swingSpeed = 1;
            }
            case 3 -> {
                S.handSize = 1;
                S.handItems.clear();
            }
            case 4 -> {
                S.groundSize = 1;
                S.groundItems.clear();
            }
            case 5 -> {
                S.handX = 0;
                S.handY = 0;
                S.handZ = 0;
            }
            case 6 -> {
                minecraft.options.glintStrength().set(1d);
                minecraft.options.glintSpeed().set(.5);
            }
            case 7 -> {
                S.zoom = 4;
                S.zoomToggle = false;
                S.scrollZoom = true;
                S.smoothZoom = true;
                S.cinematic = false;
            }
            case 8 -> {
                S.crosshairSize = 1;
                S.crosshairColor = 0;
                S.targetIndicator = false;
            }
            case 9 -> S.fullbright = false;
            case 10 -> {
                S.clearWeather = false;
                S.skyTime = 0;
            }
            case 11 -> {
                S.hiddenParticles.clear();
                minecraft.options.particles().set(net.minecraft.server.level.ParticleStatus.ALL);
            }
            case 12 -> {
                S.fireHeight = 1;
                S.fireOpacity = .9;
            }
            case 13 -> {
                S.clickSounds = true;
                S.pauseMenu = false;
                S.animations = true;
            }
            case 14 -> {
                S.theme = 0;
                S.background = 0;
                S.darken = .55;
                S.opacity = .96;
                S.windowSize = 1;
            }
        }
        S.save();
        rebuildWidgets();
    }

    private void sizes(boolean hand) {
        slider(
                "All items",
                hand ? S.handSize : S.groundSize,
                .25,
                3,
                v -> {
                    if (hand) S.handSize = v;
                    else S.groundSize = v;
                });
        EditBox field = new EditBox(font, x, y, w, 17, Component.literal("Item"));
        field.setValue(needle);
        field.setHint(Component.literal("Find item …"));
        addRenderableWidget(field);
        y += 22;
        var origin = y;
        java.util.List<AbstractWidget> sliders = new ArrayList<>();
        Runnable update =
                () -> {
                    sliders.forEach(this::removeWidget);
                    sliders.clear();
                    y = origin;
                    var map = hand ? S.handItems : S.groundItems;
                    var results =
                            BuiltInRegistries.ITEM.keySet().stream()
                                    .filter(
                                            id ->
                                                    id.getPath()
                                                            .contains(
                                                                    needle.toLowerCase(
                                                                            Locale.ROOT)))
                                    .sorted()
                                    .toList();
                    int limit = Math.max(1, (height - y - 10) / 21);
                    firstItem = Math.clamp(firstItem, 0, Math.max(0, results.size() - limit));
                    for (int i = firstItem; i < Math.min(results.size(), firstItem + limit); i++) {
                        var id = results.get(i);
                        String key = id.toString();
                        sliders.add(
                                slider(
                                        id.getPath(),
                                        map.getOrDefault(key, 1d),
                                        .25,
                                        3,
                                        v -> {
                                            if (Math.abs(v - 1) < .02) map.remove(key);
                                            else map.put(key, v);
                                        }));
                    }
                };
        field.setResponder(
                s -> {
                    needle = s;
                    firstItem = 0;
                    update.run();
                });
        update.run();
    }

    private void toggle(String label, boolean value, Runnable change) {
        action(label + " · " + (value ? "On" : "Off"), change);
    }

    private void action(String label, Runnable fn) {
        addRenderableWidget(
                new StudioButton(
                        x,
                        y,
                        w,
                        18,
                        label,
                        () -> {
                            fn.run();
                            S.save();
                            if (minecraft.screen == this) rebuildWidgets();
                        }));
        y += 22;
    }

    private AbstractWidget slider(
            String label, double initial, double min, double max, DoubleConsumer change) {
        var control =
                new AbstractSliderButton(
                        x,
                        y,
                        w,
                        18,
                        Component.empty(),
                        Math.clamp((initial - min) / (max - min), 0, 1)) {
                    {
                        updateMessage();
                    }

                    double current() {
                        return min + value * (max - min);
                    }

                    protected void updateMessage() {
                        setMessage(
                                Component.literal(
                                        label
                                                + "  "
                                                + (label.equals("Zoom ×")
                                                        ? String.format(
                                                                Locale.ROOT, "%.1f", current())
                                                        : Math.round(current() * 100) + "%")));
                    }

                    protected void applyValue() {
                        change.accept(current());
                    }
                };
        addRenderableWidget(control);
        y += 21;
        return control;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx < l + nav) offset -= (int) Math.signum(v);
        else if (page == 3 || page == 4) firstItem -= (int) Math.signum(v);
        else return false;
        rebuildWidgets();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        rounded(g, l, 5, r, height - 5, 8, 0xec192227);
        rounded(g, l + 3, 33, l + nav - 2, height - 8, 5, 0x60101920);
        g.drawString(font, "Settings", l + 70, 16, TextureStudioScreen.TEXT, false);
        g.drawString(font, PAGES[page], x, 40, TextureStudioScreen.ACCENT, false);
        super.render(g, mx, my, dt);
        if (page == 14)
            g.drawString(
                    font,
                    feedback.isEmpty() ? "Drop a background image here" : feedback,
                    x,
                    y + 2,
                    TextureStudioScreen.MUTED,
                    false);
        if (page == 0 && y + 30 < height) {
            var group = de.resourcebuilder.category.PvPItem.DIAMOND_ARMOR;
            var style =
                    de.resourcebuilder.pack.LivePackManager.getInstance().getActiveSelection(group);
            int size = Math.min(94, height - y - 8), rgb = S.hitColor == 0 ? 0xff4444 : S.hitRgb();
            int tint =
                    net.minecraft.util.ARGB.srgbLerp((float) S.hitStrength, -1, 0xff000000 | rgb);
            preview.portrait(g, group, style, x + (w - size) / 2, y, size, tint);
        }
    }

    @Override
    public void onFilesDrop(List<java.nio.file.Path> paths) {
        if (page != 14 || paths.isEmpty()) return;
        try {
            StudioBackdrop.importImage(paths.getFirst());
            S.background = 4;
            S.save();
            feedback = "";
            rebuildWidgets();
        } catch (java.io.IOException error) {
            feedback = error.getMessage();
        }
    }

    @Override
    public void removed() {
        preview.close();
        S.save();
        minecraft.options.save();
        super.removed();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(back);
    }
}
