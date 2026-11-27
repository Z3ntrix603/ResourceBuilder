package verify;

import de.resourcebuilder.StudioSettings;
import de.resourcebuilder.equipment.EquipmentSetManager;

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
    } finally {
      StudioSettings.S.equipmentSet = previous;
    }
  }
}
