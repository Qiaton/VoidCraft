package com.example.voidcraft.Custom.Clock;

import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class VoidClock {
    public static Map<UUID,Integer> VOID_TICKS = new HashMap<>();
    public static Map<UUID,Integer> VOID_PLAYER_TICKS = new HashMap<>();
    public static Map<UUID,Integer> VOID_PLAYER_TOTAL_TICKS = new HashMap<>();
    public static final int DEFAULT_VOID_PLAYER_FLASH_TOTAL = 3;
    @SubscribeEvent
    public static void VOID_TICK_SERVER(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.level().isClientSide()){
            return;
        }
        UUID uuid = player.getUUID();
        Integer ticks = VOID_TICKS.get(uuid);
        if(ticks != null && ticks > 0){
            player.setData(ModAttachments.IN_VOID.get(),true);
            VOID_TICKS.put(uuid,ticks-1);
            if(ticks == 1){
                ModSound.playOutVoid(player.level(), player);
                ModNetworking.sendPhaseTear(player, VoidRingInstance.Preset.DEFAULT); //相位裂缝动画
            }
        }
        else{
            player.setData(ModAttachments.IN_VOID.get(),false);//修改玩家身上的IN_VOID为false
            VOID_TICKS.remove(uuid);//删除数据表 优化性能
        }
    }
 @SubscribeEvent
    public static void VOID_PLAYER_TICK_CLIENT(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(!player.level().isClientSide()){
            return;
        }
        UUID uuid = player.getUUID();
        Integer ticks = VOID_PLAYER_TICKS.get(uuid);
        if(ticks != null && ticks > 0){
            VOID_PLAYER_TICKS.put(uuid,ticks-1);

        }
        else {
            VOID_PLAYER_TICKS.remove(uuid);//删除数据表 优化性能
            VOID_PLAYER_TOTAL_TICKS.remove(uuid);
        }
    }
    public static void VOID_PLAYER_FLASH(Player player){
        VOID_PLAYER_FLASH(player, DEFAULT_VOID_PLAYER_FLASH_TOTAL);
    }
    public static void VOID_PLAYER_FLASH(Player player, int totalTicks){
        int clampedTicks = Math.max(1, totalTicks);
        VOID_PLAYER_TICKS.put(player.getUUID(), clampedTicks);
        VOID_PLAYER_TOTAL_TICKS.put(player.getUUID(), clampedTicks);
    }
}
