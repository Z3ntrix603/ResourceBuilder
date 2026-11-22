package de.resourcebuilder.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleQuadParticle.class)
public interface ParticleVisualAccess {
  @Accessor("quadSize")
  float rb$getQuadSize();

  @Accessor("quadSize")
  void rb$setQuadSize(float size);
}
