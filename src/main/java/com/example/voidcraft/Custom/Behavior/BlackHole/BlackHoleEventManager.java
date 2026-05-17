package com.example.voidcraft.Custom.Behavior.BlackHole;

import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
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
    private static final int VISUAL_SYNC_INTERVAL_TICKS = 20;
    public static void addChannel(Player player, ServerLevel level, Vec3 center, float radius, float strength, int coreColor, int color){
        UUID uuid = UUID.randomUUID();
        UUID owner = player.getUUID();
        BlackHoleEventInstance blackHole = new BlackHoleEventInstance(owner,uuid,level,center,radius,strength,coreColor,color,getViewYaw(player));
        EVENTS.put(blackHole.uuid,blackHole);
        startSound(blackHole);
    }
    public static void addBurst(Entity entity, ServerLevel level, Vec3 center, float radius, float strength, int duration, int coreColor, int color){
        addBurst(entity, level, center, radius, strength, duration, coreColor, color, 0.0F, false, false, ModDamageTypes.RIFT_TEAR);
    }
    public static void addBurst(Entity entity, ServerLevel level, Vec3 center, float radius, float strength, int duration, int coreColor, int color, float coreDamage, boolean hurtPlayers, boolean pullPlayers, ResourceKey<DamageType> damageType){
        UUID uuid = UUID.randomUUID();
        UUID owner = entity.getUUID();
        BlackHoleEventInstance blackHole = new BlackHoleEventInstance(owner,uuid,level,center,radius,strength,duration,coreColor,color,coreDamage,hurtPlayers,pullPlayers,damageType,getViewYaw(entity));
        ModNetworking.sendBlackHoleAt(level, center, 1, blackHole.uuid, blackHole.getAgeTicks(), -1, blackHole.getConfig());
        EVENTS.put(blackHole.uuid,blackHole);
        startSound(blackHole);
    }
    public void  remove(UUID uuid){
        BlackHoleEventInstance instance = EVENTS.remove(uuid);
        stopSound(instance);
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
                    if (instance.duration > 0 && instance.duration % VISUAL_SYNC_INTERVAL_TICKS == 0) {
                        syncVisual(instance);
                    }
                } else {
                    stopSound(instance);
                    iterator.remove();
                }
            } catch (Exception e) {
                Logger.getLogger("Minecraft").severe("结算黑洞事件寿命错误");
            }
        }
    }

    private static void startSound(BlackHoleEventInstance blackHole) {
        if (blackHole == null) {
            return;
        }

        ModSound.playBlackHoleRelease(blackHole.level, blackHole.center);
        ModNetworking.sendLoopSoundStart(
                blackHole.level,
                blackHole.uuid,
                ModSound.BLACK_HOLE_PULL.getId(),
                blackHole.center,
                ModSound.BLACK_HOLE_PULL_VOLUME,
                ModSound.BLACK_HOLE_PULL_PITCH,
                blackHole.duration
        );
    }

    private static void syncVisual(BlackHoleEventInstance blackHole) {
        if (blackHole == null) {
            return;
        }

        ModNetworking.sendBlackHoleAt(
                blackHole.level,
                blackHole.center,
                1,
                blackHole.uuid,
                blackHole.getAgeTicks(),
                -1,
                blackHole.getConfig()
        );
    }

    private static void stopSound(BlackHoleEventInstance blackHole) {
        if (blackHole == null) {
            return;
        }

        ModNetworking.sendLoopSoundStop(
                blackHole.level,
                blackHole.uuid,
                ModSound.BLACK_HOLE_PULL.getId(),
                blackHole.center
        );
    }
}
