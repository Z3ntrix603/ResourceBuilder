package de.resourcebuilder.gui;

import de.resourcebuilder.equipment.EquipmentSetDefinition;
import de.resourcebuilder.equipment.EquipmentSetManager;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EquipmentSetsScreen extends StudioScreen {
  public static final List<String> SET_NAMES =
      List.of("Void Warden", "Frost Seraph", "Crimson Revenant");

  private final Screen back;
  private int left, top, right, bottom, cardY, cardW, gap;

  public EquipmentSetsScreen(Screen back) {
    super(Component.literal("3D Equipment Sets"));
    this.back = back;
  }

  @Override
  protected void init() {
    int w = Math.min(width - 12, 780), h = Math.min(height - 12, 440);
    left = (width - w) / 2; right = left + w; top = (height - h) / 2; bottom = top + h;
    gap = 9; cardW = (w - 44 - gap * 2) / 3; cardY = top + 70;
    addRenderableWidget(new StudioButton(left + 12, top + 12, 58, 18, "Back", this::onClose));
    addRenderableWidget(new StudioButton(right - 68, top + 12, 56, 18, "Off", () -> {
      EquipmentSetManager.select("off"); rebuildWidgets();
    }));
    var sets = EquipmentSetManager.builtins();
    for (int i = 0; i < sets.size(); i++) {
      var set = sets.get(i); int x = left + 14 + i * (cardW + gap);
      addRenderableWidget(new StudioButton(x + 9, bottom - 61, cardW - 18, 20,
          EquipmentSetManager.active().id().equals(set.id()) ? "Active" : "Use set", () -> {
            EquipmentSetManager.select(set.id()); rebuildWidgets();
          }));
    }
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
    rounded(g, left, top, right, bottom, 11, 0xf0101a20);
    g.text(font, "Resource Builder · 3D Sets", left + 84, top + 15, TextureStudioScreen.TEXT, false);
    g.text(font, "Armor · Back attachment · Spear", left + 15, top + 45, TextureStudioScreen.ACCENT, false);
    var sets = EquipmentSetManager.builtins();
    for (int i = 0; i < sets.size(); i++) drawCard(g, sets.get(i), i);
    g.text(font, "Cached models · no per-frame file work", left + 15, bottom - 22,
        TextureStudioScreen.MUTED, false);
    super.extractRenderState(g, mx, my, dt);
  }

  private void drawCard(GuiGraphicsExtractor g, EquipmentSetDefinition set, int index) {
    int x = left + 14 + index * (cardW + gap), y = cardY, cardBottom = bottom - 70;
    boolean active = EquipmentSetManager.active().id().equals(set.id());
    rounded(g, x, y, x + cardW, cardBottom, 8, active ? 0xd1314b42 : 0xb61b272d);
    if (active) rounded(g, x + 1, y + 1, x + 4, cardBottom - 1, 2, TextureStudioScreen.ACCENT);
    g.centeredText(font, set.displayName(), x + cardW / 2, y + 12, TextureStudioScreen.TEXT);
    int cx = x + cardW / 2, cy = y + 75, main = 0xff000000 | set.primaryColor();
    int accent = 0xff000000 | set.accentColor();
    rounded(g, cx - 21, cy - 23, cx + 21, cy + 15, 5, main);
    rounded(g, cx - 15, cy - 45, cx + 15, cy - 19, 5, main);
    g.fill(cx - 18, cy - 47, cx - 12, cy - 29, accent);
    g.fill(cx + 12, cy - 47, cx + 18, cy - 29, accent);
    g.fill(cx - 30, cy - 19, cx - 19, cy + 24, main);
    g.fill(cx + 19, cy - 19, cx + 30, cy + 24, main);
    g.fill(cx - 19, cy + 14, cx - 3, cy + 59, main);
    g.fill(cx + 3, cy + 14, cx + 19, cy + 59, main);
    g.fill(cx - 4, cy - 17, cx + 4, cy + 11, accent);
    for (int n = 0; n < 9; n++) g.fill(cx + 27 + n * 5, cy + 42 - n * 7,
        cx + 32 + n * 5, cy + 48 - n * 7, n > 5 ? accent : main);
    g.centeredText(font, index == 0 ? "Heavy horned plate" : index == 1 ? "Winged frost plate" : "Barbed demon plate",
        cx, cardBottom - 49, TextureStudioScreen.MUTED);
  }

  @Override public void onClose() { minecraft.setScreenAndShow(back); }
}
