package de.resourcebuilder.equipment;

import static de.resourcebuilder.equipment.EquipmentSetDefinition.Anchor.*;

import de.resourcebuilder.StudioSettings;
import java.util.List;

public final class EquipmentSetManager {
  private static final EquipmentSetDefinition OFF =
      new EquipmentSetDefinition("off", "Off", 0, 0, "", "", List.of());
  private static final List<EquipmentSetDefinition> BUILTINS = List.of(
      voidWarden(), frostSeraph(), crimsonRevenant());

  private EquipmentSetManager() {}

  public static List<EquipmentSetDefinition> builtins() { return BUILTINS; }

  public static EquipmentSetDefinition resolve(String id) {
    if (id == null || "off".equals(id)) return OFF;
    return BUILTINS.stream().filter(set -> set.id().equals(id)).findFirst().orElse(OFF);
  }

  public static EquipmentSetDefinition active() { return resolve(StudioSettings.S.equipmentSet); }

  public static void select(String id) {
    StudioSettings.S.equipmentSet = resolve(id).id();
    StudioSettings.S.save();
  }

  private static EquipmentSetDefinition voidWarden() {
    return set("void_warden", "Void Warden", 0x20232c, 0xe9edf2, "minecraft:textures/entity/equipment/humanoid/netherite.png",
        "resourcebuilder:item/void_warden_spear", 0);
  }

  private static EquipmentSetDefinition frostSeraph() {
    return set("frost_seraph", "Frost Seraph", 0xdcecf2, 0x61d9ff, "minecraft:textures/entity/equipment/humanoid/diamond.png",
        "resourcebuilder:item/frost_seraph_spear", 1);
  }

  private static EquipmentSetDefinition crimsonRevenant() {
    return set("crimson_revenant", "Crimson Revenant", 0x171419, 0xf22b36, "minecraft:textures/entity/equipment/humanoid/netherite.png",
        "resourcebuilder:item/crimson_revenant_spear", 2);
  }

  private static EquipmentSetDefinition set(String id, String name, int primary, int accent, String texture, String spear, int style) {
    float horn = style == 1 ? .52f : .72f;
    var pieces = List.of(
        piece("helm_crown", HEAD, 0, -7.8f, 0, 0, 0, 0, -4.8f, -1.7f, -4.8f, 9.6f, 3.2f, 9.6f, .35f, 0, 0),
        piece("horn_left", HEAD, 3.2f, -7.4f, 0, 0, 0, -horn, -.8f, -5.2f, -1.3f, 2.2f, 6.5f, 2.6f, 0, 0, 0),
        piece("horn_right", HEAD, -3.2f, -7.4f, 0, 0, 0, horn, -1.4f, -5.2f, -1.3f, 2.2f, 6.5f, 2.6f, 0, 0, 0),
        piece("visor", HEAD, 0, -4.2f, -4.2f, 0, 0, 0, -4.6f, -1.5f, -.7f, 9.2f, 3f, 1.4f, 0, 16, 0),
        piece("chest", BODY, 0, 5.7f, 0, 0, 0, 0, -4.8f, -6.2f, -2.7f, 9.6f, 12.4f, 5.4f, .25f, 16, 16),
        piece("shoulder_left", LEFT_ARM, 0, 1.5f, 0, 0, 0, -.18f, -3.2f, -3.2f, -3.1f, 6.4f, 5.2f, 6.2f, .2f, 40, 16),
        piece("shoulder_right", RIGHT_ARM, 0, 1.5f, 0, 0, 0, .18f, -3.2f, -3.2f, -3.1f, 6.4f, 5.2f, 6.2f, .2f, 40, 16),
        piece("gauntlet_left", LEFT_ARM, 0, 8.2f, 0, 0, 0, 0, -2.7f, -3f, -2.7f, 5.4f, 6f, 5.4f, .1f, 40, 16),
        piece("gauntlet_right", RIGHT_ARM, 0, 8.2f, 0, 0, 0, 0, -2.7f, -3f, -2.7f, 5.4f, 6f, 5.4f, .1f, 40, 16),
        piece("back_spine", BODY, 0, 4f, 3.2f, .12f, 0, 0, -1.4f, -7f, -.8f, 2.8f, 14f, 1.6f, 0, 16, 16),
        piece("back_left", BODY, 2.8f, 3f, 3.2f, 0, .15f, -.3f, -.8f, -7f, -.7f, 1.6f, 12f, 1.4f, 0, 16, 16),
        piece("back_right", BODY, -2.8f, 3f, 3.2f, 0, -.15f, .3f, -.8f, -7f, -.7f, 1.6f, 12f, 1.4f, 0, 16, 16),
        piece("greave_left", LEFT_LEG, 0, 7.5f, 0, 0, 0, 0, -2.5f, -4.5f, -2.5f, 5f, 9f, 5f, .08f, 0, 16),
        piece("greave_right", RIGHT_LEG, 0, 7.5f, 0, 0, 0, 0, -2.5f, -4.5f, -2.5f, 5f, 9f, 5f, .08f, 0, 16));
    return new EquipmentSetDefinition(id, name, primary, accent, texture, spear, pieces);
  }

  private static EquipmentSetDefinition.Piece piece(String name, EquipmentSetDefinition.Anchor anchor,
      float px, float py, float pz, float rx, float ry, float rz,
      float x, float y, float z, float w, float h, float d, float inflate, int u, int v) {
    return new EquipmentSetDefinition.Piece(name, anchor, px, py, pz, rx, ry, rz,
        new EquipmentSetDefinition.Cube(x, y, z, w, h, d, inflate, u, v));
  }
}
