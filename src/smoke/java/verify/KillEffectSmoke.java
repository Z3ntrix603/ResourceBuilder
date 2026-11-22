package verify;

import de.resourcebuilder.KillEffects;
import de.resourcebuilder.RemoteKillTracker;
import de.resourcebuilder.StudioSettings;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Runs in the disposable smoke world and observes the actual client particle engine. */
public final class KillEffectSmoke {
    public static CompletableFuture<Void> run(Minecraft client) {
        String previous = StudioSettings.S.killEffect;
        verifyRemotePackets(client);
        return exercise(client, "Off", true)
                .thenCompose(v -> exercise(client, "Ring", false))
                .thenCompose(v -> exercise(client, "Ring", true))
                .thenCompose(v -> exercise(client, "Lightning", true))
                .thenCompose(v -> exercise(client, "Soul", true))
                .thenCompose(v -> exercise(client, "Burst", true))
                .orTimeout(30, TimeUnit.SECONDS)
                .whenCompleteAsync((v, failure) -> StudioSettings.S.killEffect = previous, client);
    }

    private static void verifyRemotePackets(Minecraft client) {
        var level = client.level;
        int localId = client.player.getId();
        var victim = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        int testId = Integer.MAX_VALUE - 1;
        while (level.getEntity(testId) != null) testId--;
        victim.setId(testId);
        victim.snapTo(client.player.position());
        level.addEntity(victim);
        var tracker = new RemoteKillTracker();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var hit =
                    new ClientboundDamageEventPacket(
                            victim, level.damageSources().playerAttack(client.player));
            var death = new ClientboundEntityEventPacket(victim, EntityEvent.DEATH);
            ClientboundDamageEventPacket.STREAM_CODEC.encode(buffer, hit);
            ClientboundEntityEventPacket.STREAM_CODEC.encode(buffer, death);
            tracker.onDamage(
                    level, localId, ClientboundDamageEventPacket.STREAM_CODEC.decode(buffer));
            var decodedDeath = ClientboundEntityEventPacket.STREAM_CODEC.decode(buffer);
            if (tracker.onEntityEvent(level, decodedDeath) != victim
                    || tracker.onEntityEvent(level, decodedDeath) != null)
                throw new AssertionError("Wire damage/death or duplicate detection failed");

            tracker.onDamage(level, localId, hit);
            tracker.onDamage(
                    level,
                    localId,
                    new ClientboundDamageEventPacket(
                            victim, level.damageSources().mobAttack(victim)));
            if (tracker.onEntityEvent(level, death) != null)
                throw new AssertionError("Another attacker's finishing blow counted");

            tracker.onDamage(level, localId, hit);
            var totem = new ClientboundEntityEventPacket(victim, EntityEvent.PROTECTED_FROM_DEATH);
            if (tracker.onEntityEvent(level, totem) != null
                    || tracker.onEntityEvent(level, death) != null)
                throw new AssertionError("Totem failed to clear hit");

            tracker.onDamage(level, localId, hit);
            victim.tickCount += 3;
            if (tracker.onEntityEvent(level, death) != null)
                throw new AssertionError("Stale attack counted as a kill");

            tracker.onDamage(level, localId, hit);
            victim.discard();
            var replacement = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            replacement.setId(victim.getId());
            replacement.snapTo(client.player.position());
            level.addEntity(replacement);
            try {
                if (tracker.onEntityEvent(level, death) != null)
                    throw new AssertionError("Recycled entity ID inherited a hit");
            } finally {
                replacement.discard();
            }
        } finally {
            buffer.release();
            victim.discard();
        }
    }

    private static CompletableFuture<Void> exercise(
            Minecraft client, String mode, boolean localKill) {
        StudioSettings.S.killEffect = mode;
        Set<Particle> before = glows(client.particleEngine);
        long lightningBefore = java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
                .filter(net.minecraft.world.entity.LightningBolt.class::isInstance).count();
        var server = client.getSingleplayerServer();
        var playerId = client.player.getUUID();
        return server.submit(
                        () -> {
                            var player = server.getPlayerList().getPlayer(playerId);
                            var level = player.level();
                            var victim =
                                    EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
                            victim.snapTo(player.position().add(2, 0, 0));
                            level.addFreshEntity(victim);
                            var source = level.damageSources().playerAttack(player);
                            if (!localKill) {
                                var attacker =
                                        EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                                attacker.snapTo(player.position());
                                level.addFreshEntity(attacker);
                                try {
                                    victim.hurtServer(
                                            level, level.damageSources().mobAttack(attacker), 100);
                                } finally {
                                    attacker.discard();
                                }
                            } else {
                                victim.hurtServer(level, source, 100);
                            }
                            if (!victim.isDeadOrDying())
                                throw new AssertionError("Smoke target survived");
                        })
                .thenRunAsync(
                        () -> {
                            Set<Particle> emitted = glows(client.particleEngine);
                            emitted.removeAll(before);
                            if (!localKill || mode.equals("Off")) {
                                if (!emitted.isEmpty())
                                    throw new AssertionError("Unexpected kill effect: " + mode);
                            } else if (mode.equals("Ring")) {
                                if (emitted.size() != 64)
                                    throw new AssertionError("Ring particles: " + emitted.size());
                                Particle dot = emitted.iterator().next();
                                var start = dot.getBoundingBox().getCenter();
                                float alpha = (float) field(SingleQuadParticle.class, dot, "alpha");
                                dot.tick();
                                if (dot.getBoundingBox()
                                                        .getCenter()
                                                        .subtract(start)
                                                        .horizontalDistance()
                                                < .1
                                        || (float) field(SingleQuadParticle.class, dot, "alpha")
                                                >= alpha)
                                    throw new AssertionError("Ring failed to expand and fade");
                            } else if (mode.equals("Soul") || mode.equals("Burst")) {
                                if (emitted.size() != (mode.equals("Soul") ? 12 : 72))
                                    throw new AssertionError("Missing " + mode + " particles");
                                Particle dot = emitted.iterator().next();
                                double start = dot.getBoundingBox().minY;
                                dot.tick();
                                if (mode.equals("Soul") && dot.getBoundingBox().minY <= start)
                                    throw new AssertionError("Soul did not ascend");
                            } else {
                                long lightningAfter = java.util.stream.StreamSupport.stream(
                                                client.level.entitiesForRendering().spliterator(), false)
                                        .filter(net.minecraft.world.entity.LightningBolt.class::isInstance).count();
                                if (lightningAfter <= lightningBefore)
                                    throw new AssertionError("Missing vanilla lightning bolt");
                            }
                        },
                        client);
    }

    private static Set<Particle> glows(ParticleEngine engine) {
        Set<Particle> glows = new HashSet<>();
        collect(glows, field(ParticleEngine.class, engine, "particlesToAdd"));
        var groups = (Map<?, ?>) field(ParticleEngine.class, engine, "particles");
        for (var group : groups.values())
            collect(glows, field(ParticleGroup.class, group, "particles"));
        return glows;
    }

    private static void collect(Set<Particle> out, Object queue) {
        for (Object p : (Collection<?>) queue)
            if (p.getClass().getEnclosingClass() == KillEffects.class) out.add((Particle) p);
    }

    private static Object field(Class<?> owner, Object instance, String name) {
        try {
            var field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
