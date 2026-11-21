package de.resourcebuilder;

import com.google.common.collect.MapMaker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public final class RemoteKillTracker {
    // Entity.equals uses network IDs. Weak identity keys distinguish respawns and world changes.
    private final Map<LivingEntity, Integer> localHits = new MapMaker().weakKeys().makeMap();

    public void onDamage(
            ClientLevel level, int localPlayerId, ClientboundDamageEventPacket packet) {
        if (level.getEntity(packet.entityId()) instanceof LivingEntity victim) {
            if (victim.getId() != localPlayerId && packet.sourceCauseId() == localPlayerId)
                localHits.put(victim, victim.tickCount);
            else localHits.remove(victim);
        }
    }

    public LivingEntity onEntityEvent(ClientLevel level, ClientboundEntityEventPacket packet) {
        byte event = packet.getEventId();
        if (event != EntityEvent.DEATH && event != EntityEvent.PROTECTED_FROM_DEATH) return null;
        if (!(packet.getEntity(level) instanceof LivingEntity victim)) return null;
        Integer hit = localHits.remove(victim);
        // Ordinary killing blows send damage before death. Allow one tick boundary in transit.
        if (event == EntityEvent.DEATH && hit != null && victim.tickCount - hit <= 1) return victim;
        return null;
    }
}
