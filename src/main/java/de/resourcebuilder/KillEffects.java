package de.resourcebuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class KillEffects {
    private static final java.util.concurrent.atomic.AtomicInteger CLIENT_ENTITY_ID =
            new java.util.concurrent.atomic.AtomicInteger(-2_000_000_000);
    private static final Material GLOW =
            new Material(TextureAtlas.LOCATION_PARTICLES, Identifier.withDefaultNamespace("glow"));

    private KillEffects() {}

    /** Called on the integrated server thread when vanilla has awarded a kill. */
    public static void onAwardKillScore(ServerPlayer killer, Entity victim) {
        if (victim == killer || !(victim instanceof LivingEntity)) return;
        var server = killer.level().getServer();
        var playerId = killer.getUUID();
        var dimension = victim.level().dimension();
        var position = victim.position();
        var client = Minecraft.getInstance();
        client.execute(
                () -> {
                    if (client.getSingleplayerServer() == server
                            && client.player != null
                            && client.player.getUUID().equals(playerId)
                            && client.level != null
                            && client.level.dimension().equals(dimension)) {
                        display(client, position);
                    }
                });
    }

    public static void display(Minecraft client, Vec3 position) {
        String mode = StudioSettings.S.killEffect;
        if (client.level == null
                || !java.util.Set.of("Ring", "Lightning", "Soul", "Burst").contains(mode)) return;
        String sound =
                switch (mode) {
                    case "Soul" -> "particle.soul_escape";
                    case "Burst" -> "entity.firework_rocket.blast";
                    case "Ring" -> "block.amethyst_block.chime";
                    default -> "entity.lightning_bolt.impact";
                };
        if (StudioSettings.S.finisherSounds)
            client.level.playLocalSound(
                    position.x,
                    position.y,
                    position.z,
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                            Identifier.withDefaultNamespace(sound)),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    .65f,
                    1f,
                    false);
        TextureAtlasSprite sprite = client.getAtlasManager().get(GLOW);
        Vec3 base = position.add(0, .12, 0);
        int rgb = StudioSettings.S.visual.finisherColor;
        float red = (rgb >> 16 & 255) / 255f, green = (rgb >> 8 & 255) / 255f, blue = (rgb & 255) / 255f;
        if ("Ring".equals(mode)) {
            int count = 64;
            for (int point = 0; point < count; point++) {
                double angle = point * 2 * Math.PI / count;
                double dx = Math.cos(angle), dz = Math.sin(angle);
                Glow glow =
                        new Glow(client.level, base.add(.28 * dx, 0, .28 * dz), sprite, .105f, 20);
                glow.setColor(red, green, blue);
                glow.setParticleSpeed(.12 * dx, .008, .12 * dz);
                client.particleEngine.add(glow);
            }
        } else if ("Soul".equals(mode)) {
            var soulSprite =
                    client.getAtlasManager()
                            .get(
                                    new Material(
                                            TextureAtlas.LOCATION_PARTICLES,
                                            Identifier.withDefaultNamespace("soul_0")));
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI * 2 / 12;
                Glow soul =
                        new Glow(
                                client.level,
                                base.add(.3 * Math.cos(angle), .5 + i * .055, .3 * Math.sin(angle)),
                                soulSprite,
                                .2f,
                                36 + i);
                soul.setColor(red, green, blue);
                soul.setParticleSpeed(
                        .012 * Math.cos(angle), .075 + i * .002, .012 * Math.sin(angle));
                client.particleEngine.add(soul);
            }
        } else if ("Burst".equals(mode)) {
            for (int i = 0; i < 72; i++) {
                double vertical = 1 - 2 * (i + .5) / 72;
                double radius = Math.sqrt(1 - vertical * vertical);
                double angle = i * 2.399963229728653;
                Glow spark = new Glow(client.level, base.add(0, .8, 0), sprite, .09f, 22);
                spark.setColor(red, green, blue);
                spark.setParticleSpeed(
                        .15 * radius * Math.cos(angle),
                        .15 * vertical,
                        .15 * radius * Math.sin(angle));
                client.particleEngine.add(spark);
            }
        } else {
            var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(
                    client.level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setVisualOnly(true);
                bolt.snapTo(position.x, position.y, position.z);
                bolt.setId(CLIENT_ENTITY_ID.getAndIncrement());
                client.level.addEntity(bolt);
            }
        }
    }

    private static final class Glow extends SingleQuadParticle {
        Glow(ClientLevel level, Vec3 point, TextureAtlasSprite sprite, float size, int ticks) {
            super(level, point.x, point.y, point.z, sprite);
            quadSize = size;
            hasPhysics = false;
            friction = 1;
            lifetime = ticks;
        }

        @Override
        public void tick() {
            super.tick();
            setAlpha(Math.max(0f, 1f - (float) age / lifetime));
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }
    }
}
