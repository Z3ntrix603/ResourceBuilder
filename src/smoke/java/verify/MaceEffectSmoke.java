package verify;

import de.resourcebuilder.MaceEffects;
import de.resourcebuilder.StudioSettings;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;

final class MaceEffectSmoke {
  static void run(Minecraft client) {
    String previous = StudioSettings.S.maceEffect;
    boolean previousSounds = StudioSettings.S.maceSounds;
    StudioSettings.S.maceSounds = false;
    try {
      verify(client, "Off", 0);
      verify(client, "Shockwave", 72);
      verify(client, "Fracture", 64);
      verify(client, "Impact", 80);
    } finally {
      StudioSettings.S.maceEffect = previous;
      StudioSettings.S.maceSounds = previousSounds;
    }
  }

  private static void verify(Minecraft client, String mode, int expected) {
    StudioSettings.S.maceEffect = mode;
    Set<Particle> before = particles(client.particleEngine);
    MaceEffects.display(client, client.player.position().add(2, 0, 0));
    Set<Particle> emitted = particles(client.particleEngine);
    emitted.removeAll(before);
    if (emitted.size() != expected)
      throw new AssertionError(mode + " mace particles: " + emitted.size());
    if (emitted.isEmpty()) return;
    Particle first = emitted.iterator().next();
    var start = first.getBoundingBox().getCenter();
    float alpha = (float) field(SingleQuadParticle.class, first, "alpha");
    first.tick();
    if ((float) field(SingleQuadParticle.class, first, "alpha") >= alpha)
      throw new AssertionError(mode + " particles did not fade");
    if (mode.equals("Shockwave")
        && first.getBoundingBox().getCenter().subtract(start).horizontalDistance() <= .1)
      throw new AssertionError("Shockwave did not expand");
  }

  private static Set<Particle> particles(ParticleEngine engine) {
    Set<Particle> result = new HashSet<>();
    collect(result, field(ParticleEngine.class, engine, "particlesToAdd"));
    var groups = (Map<?, ?>) field(ParticleEngine.class, engine, "particles");
    for (var group : groups.values())
      collect(result, field(ParticleGroup.class, group, "particles"));
    return result;
  }

  private static void collect(Set<Particle> out, Object queue) {
    for (Object particle : (Collection<?>) queue)
      if (particle.getClass().getEnclosingClass() == MaceEffects.class)
        out.add((Particle) particle);
  }

  private static Object field(Class<?> owner, Object instance, String name) {
    try {
      var field = owner.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(instance);
    } catch (ReflectiveOperationException error) {
      throw new AssertionError(error);
    }
  }
}
