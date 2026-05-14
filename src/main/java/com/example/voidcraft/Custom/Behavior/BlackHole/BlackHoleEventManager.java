package com.example.voidcraft.Custom.Behavior.BlackHole;

import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@EventBusSubscriber
public class BlackHoleEventManager {
    public static Map<UUID,BlackHoleEventInstance> EVENTS = new HashMap<>();
    public static void addChannel(Player player, ServerLevel level, Vec3 center, float radius, float strength, int coreColor, int color){
        UUID uuid = UUID.randomUUID();
        UUID owner = player.getUUID();
        BlackHoleEventInstance blackHole = new BlackHoleEventInstance(owner,uuid,level,center,radius,strength,coreColor,color,getViewYaw(player));
        EVENTS.put(blackHole.uuid,blackHole);
    }
    public static void addBurst(Entity entity, ServerLevel level, Vec3 center, float radius, float strength, int duration, int coreColor, int color){
        UUID uuid = UUID.randomUUID();
        UUID owner = entity.getUUID();
        BlackHoleEventInstance blackHole = new BlackHoleEventInstance(owner,uuid,level,center,radius,strength,duration,coreColor,color,getViewYaw(entity));
        ModNetworking.sendBlackHoleAt(level,center,1,blackHole.getConfig());
        EVENTS.put(blackHole.uuid,blackHole);
    }
    public void  remove(UUID uuid){
        EVENTS.remove(uuid);
    }

    private static float getViewYaw(Entity entity) {
        return entity == null ? 0.0F : (float) Math.toRadians(-entity.getYRot());
    }

    @SubscribeEvent
    public static void age(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, BlackHoleEventInstance>> iterator = EVENTS.entrySet().iterator();
        while (iterator.hasNext()) {
            BlackHoleEventInstance instance = iterator.next().getValue();
            try {
                if (instance.duration > 0) {
                    instance.duration--;
                } else {
                    iterator.remove();
                }
            } catch (Exception e) {
                Logger.getLogger("Minecraft").severe("结算黑洞事件寿命错误");
            }
        }
    }
}
