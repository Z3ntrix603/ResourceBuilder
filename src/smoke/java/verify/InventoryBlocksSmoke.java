package verify;

import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.render.InventoryHighlight;
import net.minecraft.world.item.Items;

final class InventoryBlocksSmoke {
  static void run() {
    if (!PvPItem.INVENTORY.getPrimaryTexturePath().equals("gui/container/inventory"))
      throw new AssertionError("Inventory texture group is missing");
    String[] blocks = de.resourcebuilder.pack.StyleAssets.CATALOG.get("BLOCKS");
    if (blocks == null
        || !java.util.List.of(blocks).contains("block/stone")
        || !java.util.List.of(blocks).contains("block/deepslate")
        || !java.util.List.of(blocks).contains("block/grass_block_top"))
      throw new AssertionError("Important natural blocks are incomplete");
    if (!InventoryHighlight.isImportant(Items.TOTEM_OF_UNDYING.getDefaultInstance())
        || InventoryHighlight.isImportant(Items.COBBLESTONE.getDefaultInstance()))
      throw new AssertionError("Inventory highlight item filter failed");
  }
}
