package com.example.voidcraft.Custom.Clock;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@EventBusSubscriber
public class ModuleSkillClock {
    public static Map<UUID,Map<Integer,Long>> COOLDOWN_TICKS = new HashMap<>();
    public static Map<UUID,Long> MODULE_ENERGY = new HashMap<>();
    private static final long maxEnergy = 1000L;
    @SubscribeEvent
    public static void ENERGY_RECHARGE(PlayerTickEvent.Post event){
        if(event.getEntity().level().isClientSide()){
            return;
        }
        if(event.getEntity() instanceof Player player){
            MODULE_ENERGY.putIfAbsent(player.getUUID(),maxEnergy);
            long energy = MODULE_ENERGY.get(player.getUUID());
            if(energy < maxEnergy){
                MODULE_ENERGY.put(player.getUUID(),Math.min(energy + 1, maxEnergy));
            }
        }
    }
    @SubscribeEvent
    public static void COOLDOWN(PlayerTickEvent.Post event){
        if(event.getEntity().level().isClientSide()){
            return;
        }
        if(event.getEntity() instanceof Player player){
           Map<Integer,Long> data =  COOLDOWN_TICKS.get(player.getUUID());
           if(data == null){
               return;
           }
            data.entrySet().removeIf(entry -> {
                long next = entry.getValue() - 1;

                if (next <= 0) {
                    return true;
                }

                entry.setValue(next);
                return false;
            });

            if (data.isEmpty()) {
                COOLDOWN_TICKS.remove(player.getUUID());
            }

        }

    }
    public static void setCooldown(ServerPlayer player, int slot, long cooldownTicks) {
        UUID playerId = player.getUUID();
        System.out.println("设置冷却：获取玩家信息");
        Map<Integer, Long> playerCooldowns = COOLDOWN_TICKS.computeIfAbsent(playerId, k -> new HashMap<>());
        System.out.println("设置冷却：查询是否有冷却");
        playerCooldowns.put(slot, cooldownTicks);
        System.out.println("设置冷却：设置冷却");
    }
    public static long getCooldown(ServerPlayer player, int slot){
        UUID playerId = player.getUUID();
        Map<Integer, Long> playerCooldowns = COOLDOWN_TICKS.get(playerId);
        System.out.println("获取冷却中");
        if(playerCooldowns == null){
            System.out.println("空冷却");
            return 0;
        }
        System.out.println("有冷却");
        return playerCooldowns.getOrDefault(slot,0L);
    }
    public static long getEnergy(ServerPlayer player){
        return MODULE_ENERGY.getOrDefault(player.getUUID(),0L);
    }
    public static void setEnergy(ServerPlayer player, long energy){
        MODULE_ENERGY.put(player.getUUID(),energy);
    }

}
