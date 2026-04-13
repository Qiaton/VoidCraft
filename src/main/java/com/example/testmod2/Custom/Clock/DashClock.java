package com.example.testmod2.Custom.Clock;

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
    public static Map<UUID,Integer> DASH_TICKS = new HashMap<>();   //记录所有玩家剩余冲刺事件的表
    public static Map<UUID, Vec3> DASH_DIRECTION = new HashMap<>(); //记录所有玩家使用流型时记录的方向的表
    @SubscribeEvent
    public static void DASH_TICK(PlayerTickEvent.Post event){ //event可以获取很多玩家的信息
        Player player = event.getEntity();
        if(player.level().isClientSide()){ //如果在客户端时
            return;
        }
        Integer tick = DASH_TICKS.get(player.getUUID());
        if(tick!=null && tick>0){
            DASH_TICKS.put(player.getUUID(),tick-1);
        }
    }

}
