package de.resourcebuilder.mixin;

import de.resourcebuilder.gui.StudioScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
abstract class StudioBlurMixin {
    @Inject(method = "getMenuBackgroundBlurriness", at = @At("RETURN"), cancellable = true)
    private void studioBlur(CallbackInfoReturnable<Integer> result) {
        var mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.screen instanceof StudioScreen)
            result.setReturnValue(Math.max(8, result.getReturnValueI()));
    }
}
