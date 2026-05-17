package com.example.voidcraft.Custom.Clock;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@EventBusSubscriber
public class DashClock {
    private static final Map<UUID, Integer> DASH_TICKS = new HashMap<>();   //记录所有玩家剩余冲刺事件的表
    private static final Map<UUID, Vec3> DASH_DIRECTION = new HashMap<>(); //记录所有玩家使用流型时记录的方向的表
    private static final Map<UUID, Float> DASH_STRENGTH = new HashMap<>();

    @SubscribeEvent
    public static void tickDash(PlayerTickEvent.Post event){ //event可以获取很多玩家的信息
        Player player = event.getEntity();
        if(player.level().isClientSide()){ //如果在客户端时
            return;
        }

        UUID playerId = player.getUUID();
        Integer tick = DASH_TICKS.get(playerId);

        if(tick == null){
            return;
        }
        if(tick <= 0){
            clearDash(player);
            return;
        }

        applyDash(player, playerId, tick);

        int nextTick = tick - 1;
        if(nextTick > 0){
            DASH_TICKS.put(playerId, nextTick);
        }
        else{
            clearDash(player);
        }
    }

    private static void applyDash(Player player, UUID playerId, int tick){
        Vec3 direction = DASH_DIRECTION.get(playerId);
        Float strength = DASH_STRENGTH.get(playerId);

        if(direction == null || strength == null || strength <= 0F){
            return;
        }

        if(tick > 1){
            player.setDeltaMovement(direction.x * strength, 0, direction.z * strength);
        }
        else{
            player.setDeltaMovement(0, 0, 0);
        }

        player.hurtMarked = true;
    }

    public static void setDash(Player player, int tick, float strength) {
        if(tick <= 0){
            clearDash(player);
            return;
        }

        UUID playerId = player.getUUID();
        DASH_TICKS.put(playerId, tick);
        DASH_STRENGTH.put(playerId, strength);
        DASH_DIRECTION.put(playerId, getDashDirection(player));
    }

    public static void keepDash(Player player, int tick, float strength) {
        if(tick <= 0){
            clearDash(player);
            return;
        }

        UUID playerId = player.getUUID();
        DASH_TICKS.put(playerId, tick);
        DASH_STRENGTH.put(playerId, strength);
        DASH_DIRECTION.computeIfAbsent(playerId, unused -> getDashDirection(player));
    }


    public static float getDashPower(Player player){
        Float strength = DASH_STRENGTH.get(player.getUUID());
        return strength == null ? 0.0F : strength;
    }

    public static void clearDash(Player player){
        UUID playerId = player.getUUID();
        player.setDeltaMovement(0, 0, 0);
        DASH_TICKS.remove(playerId);
        DASH_DIRECTION.remove(playerId);
        DASH_STRENGTH.remove(playerId);
    }


    private static Vec3 getDashDirection(Player player){
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0, look.z);

        if(direction.lengthSqr() > 1.0E-6){
            return direction.normalize();
        }

        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)).normalize();
    }
}
