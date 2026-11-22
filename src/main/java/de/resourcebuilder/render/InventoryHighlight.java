package de.resourcebuilder.render;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InventoryHighlight {
  private InventoryHighlight() {}

  public static boolean isImportant(ItemStack stack) {
    return stack.is(Items.TOTEM_OF_UNDYING)
        || stack.is(Items.END_CRYSTAL)
        || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
        || stack.is(Items.MACE);
  }
}
