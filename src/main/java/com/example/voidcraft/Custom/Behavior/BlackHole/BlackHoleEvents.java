package com.example.voidcraft.Custom.Behavior.BlackHole;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.entity.Entity;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.voidcraft.Custom.Behavior.BlackHole.BlackHoleEventManager.EVENTS;

@EventBusSubscriber
public class BlackHoleEvents {
@SubscribeEvent
public static void pullEntities(ServerTickEvent.Post event){
    for (Map.Entry<UUID, BlackHoleEventInstance> uuidBlackHoleEventInstanceEntry : EVENTS.entrySet()) {
        BlackHoleEventInstance instance = uuidBlackHoleEventInstanceEntry.getValue();
        float pullRadius = instance.pullRadius;
        float pullStrength = instance.pullStrength;
        Vec3 center = instance.center;
        ServerLevel level = instance.level;
        AABB box = new AABB(center, center).inflate(pullRadius * pullStrength * 20);
        List<Entity> entities = level.getEntities(
                (Entity) null,
                box,
                entity -> !(entity instanceof Player)

        );
        for (Entity entity : entities) {
            Vec3 direction = center.subtract(entity.getX(), entity.getY(), entity.getZ());
            float coefficient = 0;
            if (direction.length() < pullRadius * pullStrength * 3) {
                entity.setDeltaMovement(direction.scale(0.2));
                entity.hurtMarked = true;
                continue;
            } else if (direction.length() < pullRadius * pullStrength * 4) {
                coefficient = 0.8F;
            } else if (direction.length() < pullRadius * pullStrength * 8) {
                coefficient = 1F;
            } else if (direction.length() < pullRadius * pullStrength * 10) {
                coefficient = 0.6F;
            } else if (direction.length() < pullRadius * pullStrength * 20) {
                coefficient = 0.2F;
            }


            Vec3 pull = direction.normalize().scale(coefficient * pullStrength);
            Vec3 entitySpeed = entity.getDeltaMovement();
            entity.setDeltaMovement(entitySpeed.add(pull));
            entity.hurtMarked = true;
        }
    }
}
}
