package de.resourcebuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class MaceEffects {
    private static final Material GLOW =
            new Material(TextureAtlas.LOCATION_PARTICLES, Identifier.withDefaultNamespace("glow"));
    private static final Set<String> MODES = Set.of("Shockwave", "Fracture", "Impact");

    private MaceEffects() {}

    public static void onDamage(Minecraft client, ClientboundDamageEventPacket packet) {
        if (client.level == null
                || client.player == null
                || packet.sourceCauseId() != client.player.getId()
                || !client.player.getMainHandItem().is(Items.MACE)) return;
        var victim = client.level.getEntity(packet.entityId());
        if (victim != null) display(client, victim.position());
    }

    public static void display(Minecraft client, Vec3 position) {
        String mode = StudioSettings.S.maceEffect;
        if (client.level == null || !MODES.contains(mode)) return;

        String sound =
                switch (mode) {
                    case "Fracture" -> "block.deepslate.break";
                    case "Impact" -> "entity.generic.explode";
                    default -> "item.mace.smash_ground";
                };
        if (StudioSettings.S.maceSounds)
            client.level.playLocalSound(
                    position.x,
                    position.y,
                    position.z,
                    SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(sound)),
                    SoundSource.PLAYERS,
                    .7f,
                    mode.equals("Impact") ? .8f : 1f,
                    false);

        TextureAtlasSprite sprite = client.getAtlasManager().get(GLOW);
        Vec3 base = position.add(0, .08, 0);
        switch (mode) {
            case "Shockwave" -> ring(client, sprite, base);
            case "Fracture" -> fracture(client, sprite, base);
            case "Impact" -> impact(client, sprite, base.add(0, .7, 0));
        }
    }

    private static void ring(Minecraft client, TextureAtlasSprite sprite, Vec3 base) {
        for (int i = 0; i < 72; i++) {
            double angle = i * Math.PI * 2 / 72;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            Spark spark = new Spark(client.level, base.add(.2 * dx, 0, .2 * dz), sprite, .1f, 18);
            spark.setColor(.35f, .9f, 1f);
            spark.setParticleSpeed(.18 * dx, .006, .18 * dz);
            client.particleEngine.add(spark);
        }
    }

    private static void fracture(Minecraft client, TextureAtlasSprite sprite, Vec3 base) {
        for (int ray = 0; ray < 8; ray++) {
            double angle = ray * Math.PI / 4 + (ray % 2) * .12;
            for (int step = 1; step <= 8; step++) {
                double distance = step * .18;
                double bend = Math.sin(step * 2.7 + ray) * .045;
                Vec3 point =
                        base.add(
                                Math.cos(angle) * distance - Math.sin(angle) * bend,
                                .006,
                                Math.sin(angle) * distance + Math.cos(angle) * bend);
                Spark crack = new Spark(client.level, point, sprite, .07f, 24);
                crack.setColor(.82f, .52f, .18f);
                client.particleEngine.add(crack);
            }
        }
    }

    private static void impact(Minecraft client, TextureAtlasSprite sprite, Vec3 base) {
        for (int i = 0; i < 80; i++) {
            double vertical = 1 - 2 * (i + .5) / 80;
            double radius = Math.sqrt(1 - vertical * vertical);
            double angle = i * 2.399963229728653;
            Spark spark = new Spark(client.level, base, sprite, .12f, 20);
            spark.setColor(1f, .78f, .32f);
            spark.setParticleSpeed(
                    .17 * radius * Math.cos(angle),
                    .17 * vertical,
                    .17 * radius * Math.sin(angle));
            client.particleEngine.add(spark);
        }
    }

    private static final class Spark extends SingleQuadParticle {
        Spark(ClientLevel level, Vec3 point, TextureAtlasSprite sprite, float size, int ticks) {
            super(level, point.x, point.y, point.z, sprite);
            quadSize = size;
            hasPhysics = false;
            friction = 1;
            lifetime = ticks;
        }

        @Override
        public void tick() {
            super.tick();
            setAlpha(Math.max(0, 1f - (float) age / lifetime));
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
