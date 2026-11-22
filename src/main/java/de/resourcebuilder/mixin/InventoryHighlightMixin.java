package de.resourcebuilder.mixin;

import de.resourcebuilder.StudioSettings;
import de.resourcebuilder.render.InventoryHighlight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class InventoryHighlightMixin {
  @Shadow protected int leftPos;
  @Shadow protected int topPos;

  @Inject(method = "extractSlot", at = @At("HEAD"))
  private void rb$importantItemBackground(
      GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    var profile = StudioSettings.S.visual;
    if (!profile.inventoryHighlight || !InventoryHighlight.isImportant(slot.getItem())) return;
    int alpha = (int) Math.round(profile.inventoryHighlightOpacity * 255);
    int color = alpha << 24 | profile.inventoryHighlightColor;
    graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, color);
  }
}
