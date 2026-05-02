package com.example.voidcraft.Custom.Clock;

import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.AssistPhaseTurretModule;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final int ENERGY_HUD_SYNC_INTERVAL_TICKS = 5;
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

        if (cleanupInvalidChannels(player, data)) {
            data = CHANNEL_ENERGY.get(playerId);
            if (data == null || data.isEmpty()) {
                return;
            }
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
            return;
        }

        // 手动炮台的持续开火跟随 channel 能量 tick；辅助炮台有自己的服务端 tick。
        PhaseTurretModule.tickAutoFire(player);
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
    public static void ENERGY_HUD_SYNC(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        tickActionbarLock(player.getUUID());
        syncEnergyHud(player);
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
        return cooldownTicks <= 0;
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
            return false;
        }

        setEnergy(player, currentEnergy - requiredEnergy);
        return true;
    }

    private static void syncEnergyHud(ServerPlayer player){
        if(player.tickCount % ENERGY_HUD_SYNC_INTERVAL_TICKS != 0){
            return;
        }
        if(!(player.getOffhandItem().getItem() instanceof PhaseWatch)){
            ModNetworking.sendEnergyHud(player, 0, false);
            return;
        }

        ModNetworking.sendEnergyHud(player, getEnergyPercent(player), true);
    }

    private static int getEnergyPercent(ServerPlayer player) {
        long energy = getEnergy(player);
        return (int) Math.max(0L, Math.min(100L, Math.round(energy * 100.0D / MAX_ENERGY)));
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
        Map<Integer, Long> channel = CHANNEL_ENERGY.remove(player.getUUID());
        if (channel == null) {
            return;
        }

        for (Integer slot : channel.keySet()) {
            // channel 停止时通知两种炮台模块分别清自己的状态。
            PhaseTurretModule.onChannelStopped(player, slot);
            AssistPhaseTurretModule.onChannelStopped(player, slot);
        }
    }
    public static void stopChannel(ServerPlayer player, int slot) {
        Map<Integer, Long> channel = CHANNEL_ENERGY.get(player.getUUID());

        if (channel == null) {
            return;
        }

        if (channel.remove(slot) != null) {
            // 单槽停止也走同一条清理入口，避免炮台球或锁定目标残留。
            PhaseTurretModule.onChannelStopped(player, slot);
            AssistPhaseTurretModule.onChannelStopped(player, slot);
        }

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

    @SubscribeEvent
    public static void PLAYER_LOGOUT(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerState(player);
        }
    }

    public static void clearPlayerState(ServerPlayer player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        CHANNEL_ENERGY.remove(playerId);
        COOLDOWN_TICKS.remove(playerId);
        MODULE_ENERGY.remove(playerId);
        ACTIONBAR_LOCK_TICKS.remove(playerId);
        // 登出时只清服务器运行时状态，避免 UUID 表一直挂到进程结束。
        PhaseTurretModule.clearPlayerState(player);
        AssistPhaseTurretModule.clearPlayerState(player);
    }

    private static boolean cleanupInvalidChannels(ServerPlayer player, Map<Integer, Long> channel) {
        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            stopChannel(player);
            return true;
        }

        ItemContainerContents contents = watchStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        NonNullList<ItemStack> items = NonNullList.withSize(
                PhaseWatch.WATCH_MODULE_SLOT_COUNT,
                ItemStack.EMPTY
        );
        contents.copyInto(items);

        List<Integer> invalidSlots = new ArrayList<>();
        for (Integer slot : channel.keySet()) {
            if (slot == null
                    || slot < 0
                    || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT
                    || !(items.get(slot).getItem() instanceof ModuleItem)) {
                invalidSlots.add(slot);
            }
        }

        for (Integer slot : invalidSlots) {
            if (slot == null) {
                Map<Integer, Long> playerChannels = CHANNEL_ENERGY.get(player.getUUID());
                if (playerChannels != null) {
                    playerChannels.remove(null);
                    if (playerChannels.isEmpty()) {
                        CHANNEL_ENERGY.remove(player.getUUID());
                    }
                }
                continue;
            }
            // 模块被移走时走正常停止入口，让各模块有机会清自己的运行时状态。
            stopChannel(player, slot);
        }

        return !invalidSlots.isEmpty();
    }
}
