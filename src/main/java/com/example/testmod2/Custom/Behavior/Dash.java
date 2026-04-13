package com.example.testmod2.Custom.Behavior;

import com.example.testmod2.Custom.Clock.DashClock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class Dash {
    @SubscribeEvent
    public static void clientTick(PlayerTickEvent.Post   event) {       //冲刺事件本体
        Player player = event.getEntity();
        Integer tick = DashClock.DASH_TICKS.getOrDefault(player.getUUID(),0);
        Vec3 direction = DashClock.DASH_DIRECTION.getOrDefault(player.getUUID(),null);
        if(tick>1) {        //根据剩余时间修改冲刺效果
            float strength = 2;
            player.setDeltaMovement(direction.x * strength,0, direction.z * strength);
            player.hurtMarked = true;
        }
        else if(tick==1) {
            player.setDeltaMovement(0,0, 0);
            player.hurtMarked = true;
        }
    }

}
