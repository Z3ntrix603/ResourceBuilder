package verify;

import de.resourcebuilder.StudioSettings;
import de.resourcebuilder.equipment.EquipmentSetManager;
import de.resourcebuilder.render.EquipmentSetModel;
import de.resourcebuilder.gui.EquipmentSetsScreen;

public final class EquipmentSetSmoke {
  public static void run() {
    var sets = EquipmentSetManager.builtins();
    if (sets.size() != 3) throw new AssertionError("Expected three 3D sets");
    if (!sets.stream().map(s -> s.id()).toList()
        .equals(java.util.List.of("void_warden", "frost_seraph", "crimson_revenant")))
      throw new AssertionError("Wrong set IDs");
    for (var set : sets) {
      if (set.pieces().size() < 10 || set.spearModel().isBlank())
        throw new AssertionError("Incomplete set: " + set.id());
      for (var piece : set.pieces()) {
        var cube = piece.cube();
        if (cube.width() <= 0 || cube.height() <= 0 || cube.depth() <= 0
            || cube.width() > 20 || cube.height() > 20 || cube.depth() > 20)
          throw new AssertionError("Unsafe cube bounds");
      }
    }
    String previous = StudioSettings.S.equipmentSet;
    try {
      StudioSettings.S.equipmentSet = "void_warden";
      if (!EquipmentSetManager.active().id().equals("void_warden"))
        throw new AssertionError("Selection failed");
      StudioSettings.S.equipmentSet = "missing";
      if (!EquipmentSetManager.active().isOff()) throw new AssertionError("Invalid ID did not fall back");
      StudioSettings.S.equipmentSet = "off";
      if (!EquipmentSetManager.active().isOff()) throw new AssertionError("Off failed");
      if (EquipmentSetManager.resolve("void_warden") != EquipmentSetManager.resolve("void_warden"))
        throw new AssertionError("Definitions are not cached");
      var definition = EquipmentSetManager.resolve("void_warden");
      if (EquipmentSetModel.forDefinition(definition) != EquipmentSetModel.forDefinition(definition))
        throw new AssertionError("Baked model was rebuilt between frames");
      if (!EquipmentSetsScreen.SET_NAMES.equals(
          java.util.List.of("Void Warden", "Frost Seraph", "Crimson Revenant")))
        throw new AssertionError("3D Sets screen is incomplete");
      try (var model = EquipmentSetSmoke.class.getResourceAsStream(
          "/assets/minecraft/models/item/netherite_spear_in_hand.json")) {
        if (model == null || !new String(model.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
            .contains("\"elements\"")) throw new AssertionError("3D Spear model missing");
      } catch (java.io.IOException error) {
        throw new AssertionError("Could not read 3D Spear model", error);
      }
    } finally {
      StudioSettings.S.equipmentSet = previous;
    }
  }
}
