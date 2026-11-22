package de.resourcebuilder.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererPostAccess {
  @Invoker("setPostEffect")
  void rb$setPostEffect(Identifier id);
}
