package com.example.voidcraft.Custom.Clock;

import com.example.voidcraft.Item.custom.PhaseWatch;
import net.minecraft.network.chat.Component;
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
    public static Map<UUID, Map<Integer, Long>> CHANNEL_ENERGY = new HashMap<>();
    private static final Map<UUID,Integer> ACTIONBAR_LOCK_TICKS = new HashMap<>();
    private static final long MAX_ENERGY = 1000L;
    private static final long TICKS_PER_SECOND = 20L;
    private static final int ENERGY_ACTIONBAR_INTERVAL_TICKS = 5;
    private static final int FEEDBACK_ACTIONBAR_LOCK_TICKS = 40;
    @SubscribeEvent
    public static void CHANNEL_CLOCK(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        Map<Integer, Long> data = CHANNEL_ENERGY.get(playerId);

        if (data == null || data.isEmpty()) {
            return;
        }

        long offEnergy = 0L;

        for (Map.Entry<Integer, Long> entry : data.entrySet()) {
            offEnergy += entry.getValue();
        }

        long energy = getEnergy(player);

        if (energy >= offEnergy) {
            setEnergy(player, energy - offEnergy);
        } else {
            stopChannel(player);
        }
    }
    @SubscribeEvent
    public static void ENERGY_RECHARGE(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        Map<Integer, Long> channelData = CHANNEL_ENERGY.get(playerId);

        if (channelData != null && !channelData.isEmpty()) {
            return;
        }

        MODULE_ENERGY.putIfAbsent(playerId, MAX_ENERGY);

        long energy = MODULE_ENERGY.get(playerId);

        if (energy < MAX_ENERGY) {
            MODULE_ENERGY.put(playerId, Math.min(energy + 10, MAX_ENERGY));
        }
    }
    @SubscribeEvent
    public static void ENERGY_ACTIONBAR(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        tickActionbarLock(player.getUUID());
        showEnergyActionbar(player);
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
        Map<Integer, Long> playerCooldowns = COOLDOWN_TICKS.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCooldowns.put(slot, cooldownTicks);
    }
    public static long getCooldown(Player player, int slot){
        UUID playerId = player.getUUID();
        Map<Integer, Long> playerCooldowns = COOLDOWN_TICKS.get(playerId);
        if(playerCooldowns == null){
            return 0;
        }
        return playerCooldowns.getOrDefault(slot,0L);
    }

    public static boolean checkCooldown(ServerPlayer player, int slot) {
        long cooldownTicks = getCooldown(player, slot);
        if(cooldownTicks <= 0){
            return true;
        }

        long cooldownSeconds = (cooldownTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        showFeedbackActionbar(player, Component.literal("技能槽 " + (slot + 1) + " 冷却中：" + cooldownSeconds + " 秒"));
        return false;
    }

    public static long getEnergy(ServerPlayer player){
        return MODULE_ENERGY.computeIfAbsent(player.getUUID(), uuid -> MAX_ENERGY);
    }

    public static void setEnergy(ServerPlayer player, long energy){
        MODULE_ENERGY.put(player.getUUID(),Math.max(0L, Math.min(energy, MAX_ENERGY)));
    }

    public static boolean tryUseEnergy(ServerPlayer player, long requiredEnergy){
        long currentEnergy = getEnergy(player);
        if(currentEnergy < requiredEnergy){
            showFeedbackActionbar(player, Component.literal("能量不足：" + currentEnergy + "/" + requiredEnergy));
            return false;
        }

        setEnergy(player, currentEnergy - requiredEnergy);
        return true;
    }

    private static void showEnergyActionbar(ServerPlayer player){
        if(player.tickCount % ENERGY_ACTIONBAR_INTERVAL_TICKS != 0){
            return;
        }
        if(ACTIONBAR_LOCK_TICKS.containsKey(player.getUUID())){
            return;
        }
        if(!(player.getOffhandItem().getItem() instanceof PhaseWatch)){
            return;
        }

        player.displayClientMessage(Component.literal("能量：" + getEnergy(player) + "/" + MAX_ENERGY), true);
    }

    private static void showFeedbackActionbar(ServerPlayer player, Component message){
        player.displayClientMessage(message, true);
        ACTIONBAR_LOCK_TICKS.put(player.getUUID(), FEEDBACK_ACTIONBAR_LOCK_TICKS);
    }

    private static void tickActionbarLock(UUID playerId){
        Integer ticks = ACTIONBAR_LOCK_TICKS.get(playerId);
        if(ticks == null){
            return;
        }
        if(ticks <= 1){
            ACTIONBAR_LOCK_TICKS.remove(playerId);
            return;
        }
        ACTIONBAR_LOCK_TICKS.put(playerId, ticks - 1);
    }

    public static void stopChannel(ServerPlayer player) {
        CHANNEL_ENERGY.remove(player.getUUID());
    }
    public static void stopChannel(ServerPlayer player, int slot) {
        Map<Integer, Long> channel = CHANNEL_ENERGY.get(player.getUUID());

        if (channel == null) {
            return;
        }

        channel.remove(slot);

        if (channel.isEmpty()) {
            CHANNEL_ENERGY.remove(player.getUUID());
        }
    }
    public static void startChannel(ServerPlayer player, int slot, long offEnergy) {
        Map<Integer, Long> channel =
                CHANNEL_ENERGY.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        channel.put(slot, offEnergy);
    }
    public static boolean getChannel(ServerPlayer player, int slot) {
        Map<Integer, Long> channel = CHANNEL_ENERGY.get(player.getUUID());

        if (channel == null) {
            return false;
        }

        return channel.containsKey(slot);
    }
}
