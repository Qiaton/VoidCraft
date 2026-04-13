package com.example.testmod2.Custom.Clock;

import com.example.testmod2.ModAttachments;
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
    @SubscribeEvent
    public static void VOID_TICK_SERVER(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.level().isClientSide()){
            return;
        }
        UUID uuid = player.getUUID();
        Integer ticks = VOID_TICKS.get(uuid);
        if(ticks != null && ticks > 0){
            VOID_TICKS.put(uuid,ticks-1);
        }
        else{
            player.setData(ModAttachments.IN_VOID.get(),false);//修改玩家身上的IN_VOID为false
            VOID_TICKS.remove(uuid);//删除数据表 优化性能
        }
    }

}
