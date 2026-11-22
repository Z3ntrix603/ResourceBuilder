package de.resourcebuilder.gui;

import static de.resourcebuilder.StudioSettings.S;
import static de.resourcebuilder.StudioSettings.favorite;

import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.pack.*;
import de.resourcebuilder.preset.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class TextureStudioScreen extends StudioScreen {

    static final int TEXT = 0xffedf3ef, MUTED = 0xffa0afa9, ACCENT = 0xff92e2be;

    private final LivePackManager packs = LivePackManager.getInstance();

    private final PreviewCache previews = new PreviewCache();

    private PvPItem item = PvPItem.CRYSTALS;

    private String query = "", status = "";

    private StudioTab tab = StudioTab.ITEMS;
    private final EnumMap<StudioTab, PvPItem> lastGroup = new EnumMap<>(StudioTab.class);

    private int left,
            top,
            right,
            bottom,
            nav,
            gridX,
            gridY,
            cols,
            rows,
            cell,
            cellH,
            firstRow,
            navOffset;

    private int toolbarY;
    private EditBox search;

    private List<TextureVariant> entries = List.of();

    private List<PvPItem> groups = List.of();

    public TextureStudioScreen() {
        super(Component.literal("Resource Builder"));
    }

    @Override
    protected void init() {

        packs.scanAllPacks();

        layout();

        refresh();

        int header = top + 7;

        int pw = Math.min(72, (right - left) / 7);

        addRenderableWidget(
                new StudioButton(
                        right - pw * 2 - 10,
                        header,
                        pw,
                        17,
                        "Profiles",
                        () -> minecraft.setScreen(new ProfilesScreen(this))));

        addRenderableWidget(
                new StudioButton(
                        right - pw - 7,
                        header,
                        pw,
                        17,
                        "Settings",
                        () -> minecraft.setScreen(new SettingsScreen(this))));

        int tabX = left + 9, tabY = top + 34;
        for (StudioTab section : StudioTab.values()) {
            int tabW = tabWidth(section);
            if (tabX + tabW > right - 8) {
                tabX = left + 9;
                tabY += 19;
            }
            addRenderableWidget(
                    new StudioButton(
                            tabX,
                            tabY,
                            tabW,
                            17,
                            section.label,
                            section == tab,
                            () -> openTab(section)));
            tabX += tabW + 2;
        }

        addRenderableWidget(
                new StudioButton(right - 65, bottom - 23, 58, 16, "Done", this::onClose));
        if (tab == StudioTab.KILLS || tab == StudioTab.MACE) {
            search = null;
            int cardWidth = (right - gridX - 8) / 3;
            int cardHeight = Math.min(100, (bottom - 36 - gridY) / 2);
            String[] modes = tab == StudioTab.KILLS ? KillEffectCard.MODES : MaceEffectCard.MODES;
            for (int i = 0; i < modes.length; i++) {
                addRenderableWidget(
                        tab == StudioTab.KILLS
                                ? new KillEffectCard(
                                        gridX + (i % 3) * cardWidth,
                                        gridY + (i / 3) * (cardHeight + 4),
                                        cardWidth - 4,
                                        cardHeight,
                                        modes[i])
                                : new MaceEffectCard(
                                        gridX + (i % 3) * cardWidth,
                                        gridY + (i / 3) * (cardHeight + 4),
                                        cardWidth - 4,
                                        cardHeight,
                                        modes[i]));
            }
            addRenderableWidget(
                    new StudioButton(
                            right - 117,
                            toolbarY,
                            106,
                            17,
                            (tab == StudioTab.KILLS ? "Finisher" : "Mace")
                                    + " sound · "
                                    + ((tab == StudioTab.KILLS ? S.finisherSounds : S.maceSounds)
                                            ? "On"
                                            : "Off"),
                            () -> {
                                if (tab == StudioTab.KILLS) S.finisherSounds = !S.finisherSounds;
                                else S.maceSounds = !S.maceSounds;
                                S.save();
                                rebuildWidgets();
                            }));
            return;
        }
        int y = toolbarY;
        int sw = Math.min(128, (right - gridX) / 2);
        search = new EditBox(font, right - sw - 9, y + 5, sw, 16, Component.literal("Find style"));

        search.setBordered(false);
        search.setMaxLength(80);
        search.setValue(query);
        search.setHint(Component.literal("Find style ...").withColor(MUTED & 0xffffff));

        search.setResponder(
                value -> {
                    query = value;
                    firstRow = 0;
                    refresh();
                });
        addRenderableWidget(search);

        addRenderableWidget(
                new StudioButton(
                        left + 7,
                        bottom - 23,
                        Math.max(65, nav - 14),
                        16,
                        S.favoritesOnly ? "★ Favorites" : "☆ Favorites",
                        () -> {
                            S.favoritesOnly = !S.favoritesOnly;
                            S.save();
                            firstRow = 0;
                            rebuildWidgets();
                        }));
    }

    private int tabWidth(StudioTab section) {
        return font.width(section.label) + (section == StudioTab.HOTBAR ? 27 : 13);
    }

    private boolean widePreview() {
        return tab == StudioTab.SKY || tab == StudioTab.HOTBAR || tab == StudioTab.VITALS;
    }

    private void layout() {

        int padX = Math.clamp(width / 15, 6, 64);
        int padY = Math.clamp(height / 13, 5, 34);
        int w =
                Math.max(
                        Math.min(width - 8, 296),
                        (int) (Math.min(width - padX * 2, 870) * Math.clamp(S.windowSize, .76, 1)));
        int h =
                Math.max(
                        Math.min(height - 8, 208),
                        (int)
                                (Math.min(height - padY * 2, 510)
                                        * Math.clamp(S.windowSize, .76, 1)));
        left = (width - w) / 2;
        right = left + w;
        top = (height - h) / 2;
        bottom = top + h;
        nav = tab.hasSidebar() ? Math.clamp(w / 5, 78, 126) : 0;
        int used = 0;
        toolbarY = top + 58;
        for (StudioTab section : StudioTab.values()) {
            int tabW = tabWidth(section);
            if (used + tabW > w - 17) {
                toolbarY += 19;
                used = 0;
            }
            used += tabW + 2;
        }
        gridX = left + nav + 10;
        gridY = toolbarY + 24;
        int avail = right - gridX - 10;
        cols = Math.clamp(avail / (widePreview() ? 100 : 70), 2, 8);
        cell = avail / cols;

        cellH = Math.min(84, Math.max(47, cell + 7));
        int usable = bottom - 29 - gridY;
        rows = Math.max(1, Math.round(usable / (float) cellH));
        cellH = Math.max(16, usable / rows);
    }

    private void refresh() {

        groups = Arrays.stream(PvPItem.values()).filter(i -> StudioTab.of(i) == tab).toList();

        var needle = query.toLowerCase(Locale.ROOT);

        entries =
                (tab == StudioTab.KILLS || tab == StudioTab.MACE)
                        ? List.of()
                        : packs.getVariants(item).stream()
                                .filter(
                                        v ->
                                                v.getPackName()
                                                        .toLowerCase(Locale.ROOT)
                                                        .contains(needle))
                                .filter(
                                        v ->
                                                !S.favoritesOnly
                                                        || S.favorites.contains(
                                                                favorite(item, v.getPackName())))
                                .toList();

        firstRow = Math.clamp(firstRow, 0, maxRow());
    }

    private int navRows() {
        return Math.max(1, (bottom - 30 - gridY) / 19);
    }

    private int maxRow() {
        return Math.max(0, (entries.size() + cols - 1) / cols - rows);
    }

    private void openTab(StudioTab section) {
        if (section == tab) return;
        if (item != null) lastGroup.put(tab, item);
        tab = section;
        item = lastGroup.getOrDefault(tab, tab.first);
        navOffset = 0;
        firstRow = 0;
        query = "";
        status = "";
        rebuildWidgets();
        if (item != null)
            navOffset =
                    Math.clamp(
                            groups.indexOf(item) - navRows() + 1,
                            0,
                            Math.max(0, groups.size() - navRows()));
    }

    private void selectGroup(PvPItem type) {
        item = type;
        layout();
        firstRow = 0;
        status = "";
        refresh();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {

        int bg = new int[] {0x101a20, 0x211a25, 0x131e2a}[Math.floorMod(S.theme, 3)];
        rounded(g, left - 1, top + 2, right + 1, bottom + 3, 10, 0x32101820);
        rounded(g, left, top, right, bottom, 10, ((int) (196 * S.opacity) << 24) | bg);
        if (S.background != 0)
            StudioBackdrop.draw(
                    g, left + nav + 2, toolbarY - 3, right - 2, bottom - 26, S.background);
        g.fill(left + 10, toolbarY - 4, right - 10, toolbarY - 3, 0x405e7478);
        if (nav > 0) g.fill(left + nav, toolbarY + 3, left + nav + 1, bottom - 28, 0x38596b68);
        rounded(g, left + 11, top + 10, left + 14, top + 26, 1, ACCENT);
        g.drawString(font, "Resource Builder", left + 20, top + 8, TEXT, false);
        g.drawString(font, "Made by Zentrix", left + 20, top + 20, MUTED, false);
        if (tab == StudioTab.KILLS || tab == StudioTab.MACE) {
            g.drawString(
                    font,
                    tab == StudioTab.KILLS ? "On kill" : "On mace hit",
                    gridX + 2,
                    toolbarY + 6,
                    TEXT,
                    false);
            g.drawString(font, "Only visible to you", gridX + 2, bottom - 18, MUTED, false);
            super.render(g, mx, my, dt);
            return;
        }
        if (nav > 0) g.drawString(font, "Categories", left + 10, toolbarY + 5, MUTED, false);
        rounded(g, search.getX() - 4, toolbarY, right - 8, toolbarY + 17, 5, 0x92203239);
        for (int n = 0; nav > 0 && n < navRows() && n + navOffset < groups.size(); n++) {

            PvPItem group = groups.get(n + navOffset);
            int y = gridY + n * 19;

            boolean selected = group == item;

            if (selected || (mx >= left + 4 && mx < left + nav && my >= y && my < y + 18))
                rounded(
                        g,
                        left + 4,
                        y,
                        left + nav - 4,
                        y + 18,
                        4,
                        selected ? 0xcd314b42 : 0x9c324246);

            if (selected) rounded(g, left + 5, y + 5, left + 7, y + 13, 1, ACCENT);

            if (minecraft.level != null) g.renderFakeItem(group.getItemStack(), left + 8, y + 1);

            g.drawString(
                    font,
                    font.plainSubstrByWidth(group.getDisplayName(), nav - 32),
                    left + 28,
                    y + 6,
                    selected ? TEXT : MUTED,
                    false);
        }

        g.drawString(
                font,
                font.plainSubstrByWidth(item.getDisplayName(), search.getX() - gridX - 4),
                gridX,
                toolbarY + 5,
                TEXT,
                false);

        var active = packs.getActiveSelection(item);

        String hovered = "";

        for (int i = 0; i < rows * cols && i + firstRow * cols < entries.size(); i++) {

            TextureVariant v = entries.get(i + firstRow * cols);
            int x = gridX + (i % cols) * cell,
                    y = gridY + (i / cols) * cellH,
                    w = cell - 3,
                    h = cellH - 3;

            boolean chosen = active == v, hover = mx >= x && mx < x + w && my >= y && my < y + h;

            if (chosen) {
                rounded(g, x, y, x + w, y + h, 6, 0xff79b9a5);
                rounded(g, x + 1, y + 1, x + w - 1, y + h - 1, 5, 0xff2b433e);
            } else {
                rounded(g, x, y, x + w, y + h, 6, hover ? 0xd0344448 : 0x9d233239);
            }

            if (hover) hovered = v.getPackName();

            int dim = Math.max(1, Math.min(w - 8, h - 19));
            int previewW = widePreview() ? Math.max(dim, Math.min(w - 6, dim * 2)) : dim;
            int px = x + (w - previewW) / 2, py = y + 3;

            boolean rendered = !item.isSound() && previews.draw(g, item, v, px, py, previewW, dim);

            if (!rendered && minecraft.level != null)
                g.renderFakeItem(item.getItemStack(), x + (w - 16) / 2, y + (h - 26) / 2);

            if (S.favorites.contains(favorite(item, v.getPackName())))
                g.drawString(font, "★", x + w - 10, y + 2, 0xffffcf78, false);

            if (chosen) g.drawString(font, "✓", x + 3, y + 2, ACCENT, false);

            String name = v.getPackName();
            if (font.width(name) > w - 5)
                name = font.plainSubstrByWidth(name, w - 5 - font.width("...")) + "...";

            g.drawString(font, name, x + (w - font.width(name)) / 2, y + h - 11, TEXT, false);
        }

        if (entries.isEmpty())
            g.drawString(font, "No matching styles", gridX + 3, gridY + 12, MUTED, false);

        if (maxRow() > 0) {

            int track = rows * cellH - 3,
                    thumb = Math.max(8, (int) (track * rows / (float) (rows + maxRow())));

            int sy = gridY + (track - thumb) * firstRow / maxRow();

            g.fill(right - 6, gridY, right - 4, gridY + track, 0xff2a3334);
            g.fill(right - 6, sy, right - 4, sy + thumb, 0xff829e90);
        }

        String hint =
                status.isEmpty()
                        ? entries.size() + " Styles  ·  " + "Right click: favorite"
                        : status;

        if (!hovered.isBlank()) hint = hovered + "  ·  " + "Right click: favorite";

        int hintX = Math.max(gridX, left + 80);
        g.drawString(
                font,
                font.plainSubstrByWidth(hint, right - hintX - 73),
                hintX,
                bottom - 18,
                MUTED,
                false);

        super.render(g, mx, my, dt);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean twice) {

        if (super.mouseClicked(e, twice)) return true;
        if (tab == StudioTab.KILLS || tab == StudioTab.MACE) return false;

        double x = e.x(), y = e.y();

        if (e.button() == 0
                && x >= left + 4
                && x < left + nav
                && y >= gridY
                && y < gridY + navRows() * 19) {

            int i = navOffset + (int) (y - gridY) / 19;
            if (i < groups.size()) selectGroup(groups.get(i));
            return true;
        }

        if (x >= gridX && x < gridX + cols * cell && y >= gridY && y < gridY + rows * cellH) {

            int idx = (firstRow + (int) (y - gridY) / cellH) * cols + (int) (x - gridX) / cell;

            if (idx >= entries.size()) return false;

            var v = entries.get(idx);

            if (e.button() == 1) {

                String key = favorite(item, v.getPackName());
                if (!S.favorites.remove(key)) S.favorites.add(key);

                S.save();
                refresh();
                return true;
            }

            if (e.button() == 0) {

                try {

                    if (packs.getActiveSelection(item) != v) packs.selectVariant(item, v, true);

                    if (item.isSound()) {

                        de.resourcebuilder.ResourceBuilderMod.previewSound(
                                LivePackManager.soundEvents(item).getFirst());
                    }

                    status = "Active · " + v.getPackName();

                } catch (RuntimeException error) {
                    status = "Could not save";
                    de.resourcebuilder.ResourceBuilderMod.LOGGER.error(status, error);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {

        int step = (int) Math.signum(vertical);

        if (x < left + nav)
            navOffset = Math.clamp(navOffset - step, 0, Math.max(0, groups.size() - navRows()));
        else firstRow = Math.clamp(firstRow - step, 0, maxRow());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        int key = event.key();
        if (groups.size() > 1
                && search != null
                && !search.isFocused()
                && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {

            int i =
                    Math.floorMod(
                            groups.indexOf(item) + (key == GLFW.GLFW_KEY_LEFT ? -1 : 1),
                            groups.size());
            selectGroup(groups.get(i));

            navOffset = Math.clamp(i - navRows() / 2, 0, Math.max(0, groups.size() - navRows()));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        previews.close();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return S.pauseMenu;
    }
}
