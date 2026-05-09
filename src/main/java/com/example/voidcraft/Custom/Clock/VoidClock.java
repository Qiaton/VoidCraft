package com.example.voidcraft.Custom.Clock;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class VoidClock {
    public static Map<UUID,Integer> VOID_TICKS = new HashMap<>();
    public static Map<UUID,Integer> VOID_PLAYER_TICKS = new HashMap<>();
    public static Map<UUID,Integer> VOID_PLAYER_TOTAL_TICKS = new HashMap<>();
    public static final int DEFAULT_VOID_PLAYER_FLASH_TOTAL = 1;
    @SubscribeEvent
    public static void tickVoidServer(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (entity.level().isClientSide()) {
            return;
        }

        UUID uuid = entity.getUUID();
        Integer ticks = VOID_TICKS.get(uuid);

        // 虚空状态由计时器驱动；没有计时器的实体不写附件，避免每 tick 给普通生物刷默认值。
        if (ticks == null) {
            return;
        }

        if (ticks > 0) {
            entity.setData(ModAttachments.IN_VOID.get(), true);
            VOID_TICKS.put(uuid, ticks - 1);
            if (ticks == 1) {
                ModSound.playOutVoid(entity.level(), entity);
                ModNetworking.sendPhaseTear(entity, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
            }
        } else {
            entity.setData(ModAttachments.IN_VOID.get(), false);//计时结束时退出虚空状态
            VOID_TICKS.remove(uuid);//删除数据表 优化性能
        }
    }
    @SubscribeEvent
    public static void tickVoidClient(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (!entity.level().isClientSide()) {
            return;
        }
        UUID uuid = entity.getUUID();
        Integer ticks = VOID_PLAYER_TICKS.get(uuid);
        if (ticks != null && ticks > 0) {
            VOID_PLAYER_TICKS.put(uuid, ticks - 1);

        } else {
            VOID_PLAYER_TICKS.remove(uuid);//删除数据表 优化性能
            VOID_PLAYER_TOTAL_TICKS.remove(uuid);
        }
    }
    @SubscribeEvent
    public static void clearRemovedEntity(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        UUID uuid = entity.getUUID();
        VOID_TICKS.remove(uuid);
        VOID_PLAYER_TICKS.remove(uuid);
        VOID_PLAYER_TOTAL_TICKS.remove(uuid);
    }
    public static void flashVoidPlayer(Player player) {
        flashVoidEntity(player, DEFAULT_VOID_PLAYER_FLASH_TOTAL);
    }
    public static void flashVoidPlayer(Player player, int totalTicks) {
        flashVoidEntity(player, totalTicks);
    }
    public static void flashVoidEntity(LivingEntity entity) {
        flashVoidEntity(entity, DEFAULT_VOID_PLAYER_FLASH_TOTAL);
    }
    public static void flashVoidEntity(LivingEntity entity, int totalTicks) {
        int clampedTicks = Math.max(1, totalTicks);
        VOID_PLAYER_TICKS.put(entity.getUUID(), clampedTicks);
        VOID_PLAYER_TOTAL_TICKS.put(entity.getUUID(), clampedTicks);
    }
    public static float getVoidFlashAlpha(LivingEntity entity) {
        int left = VOID_PLAYER_TICKS.getOrDefault(entity.getUUID(), 0);
        int total = VOID_PLAYER_TOTAL_TICKS.getOrDefault(
                entity.getUUID(),
                DEFAULT_VOID_PLAYER_FLASH_TOTAL
        );
        float progress = 1.0F - (float) left / Math.max(1, total);
        return 1.0F - progress * progress;
    }
    public static boolean hasVoidFlash(LivingEntity entity) {
        return VOID_PLAYER_TICKS.containsKey(entity.getUUID());
    }
    public static void setVoidTicks(LivingEntity entity, Integer ticks) {
        VOID_TICKS.put(entity.getUUID(), ticks);
    }
    public static void stopVoid(LivingEntity entity) {
        ModSound.playOutVoid(entity.level(), entity);
        ModNetworking.sendPhaseTear(entity, VoidRingInstance.Preset.DEFAULT);
        entity.setData(ModAttachments.IN_VOID.get(), false);
        VOID_TICKS.remove(entity.getUUID());
    }
}
