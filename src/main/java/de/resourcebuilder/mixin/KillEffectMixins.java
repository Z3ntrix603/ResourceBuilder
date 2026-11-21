package de.resourcebuilder.mixin;

import de.resourcebuilder.KillEffects;
import de.resourcebuilder.RemoteKillTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class IntegratedKillCreditMixin {
    @Inject(method = "awardKillScore", at = @At("TAIL"))
    private void studioKillCredit(Entity victim, DamageSource source, CallbackInfo ci) {
        KillEffects.onAwardKillScore((ServerPlayer) (Object) this, victim);
    }
}

@Mixin(ClientPacketListener.class)
abstract class RemoteKillEffectMixin {
    @Shadow private ClientLevel level;
    @Unique private final RemoteKillTracker studioKillTracker = new RemoteKillTracker();

    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void studioDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (client.hasSingleplayerServer() || client.level != level || client.player == null)
            return;
        studioKillTracker.onDamage(level, client.player.getId(), packet);
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void studioDeath(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (client.hasSingleplayerServer() || client.level != level || client.player == null)
            return;
        var victim = studioKillTracker.onEntityEvent(level, packet);
        if (victim != null) KillEffects.display(client, victim.position());
    }
}
