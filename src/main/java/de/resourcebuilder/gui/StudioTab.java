package de.resourcebuilder.gui;

import de.resourcebuilder.category.PvPItem;

enum StudioTab {
  ITEMS("Items", PvPItem.CRYSTALS),
  ARMOR("Armor", PvPItem.DIAMOND_ARMOR),
  HOTBAR("Hotbar", PvPItem.HUD),
  VITALS("Vitals", PvPItem.HEARTS),
  SKY("Sky", PvPItem.SKY),
  FONT("Font", PvPItem.FONT),
  CROSSHAIR("Crosshair", PvPItem.CROSSHAIR),
  EFFECTS("Effects", PvPItem.CRIT_PARTICLES),
  MACE("Mace FX", null),
  KILLS("Kill Effects", null),
  SOUNDS("Sounds", PvPItem.SOUND_MACE);

  final String label;
  final PvPItem first;

  StudioTab(String label, PvPItem first) {
    this.label = label;
    this.first = first;
  }

  boolean hasSidebar() {
    return this == ITEMS || this == ARMOR || this == VITALS || this == EFFECTS || this == SOUNDS;
  }

  static StudioTab of(PvPItem item) {
    if (item.isSound()) return SOUNDS;
    return switch (item) {
      case DIAMOND_ARMOR, NETHERITE_ARMOR, ELYTRA, SHIELD -> ARMOR;
      case SKY -> SKY;
      case HUD -> HOTBAR;
      case HEARTS, HUNGER, XP_BAR -> VITALS;
      case CROSSHAIR -> CROSSHAIR;
      case FONT -> FONT;
      case CRIT_PARTICLES, CRYSTAL_EXPLOSION, TOTEM_POPS, FIRE -> EFFECTS;
      default -> ITEMS;
    };
  }
}
