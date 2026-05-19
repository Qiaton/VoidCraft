package com.example.voidcraft.Sound;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSound {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, VoidCraft.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ENTER_VOID = SOUND_EVENTS.register(
            "enter_void",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT_PORTAL_ENTER = SOUND_EVENTS.register(
            "teleport_portal_enter",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT_DEPLOY_START = SOUND_EVENTS.register(
            "teleport_deploy_start",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT_DEPLOY_END = SOUND_EVENTS.register(
            "teleport_deploy_end",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> OUT_VOID = SOUND_EVENTS.register(
            "out_void",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> IN_VOID_LONG = SOUND_EVENTS.register(
            "in_void_long",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> PHASE_TURRET_SHOT = SOUND_EVENTS.register(
            "phase_turret_shot",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> HEALTH_PHASE_TURRET_SHOT = SOUND_EVENTS.register(
            "health_phase_turret_shot",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAL = SOUND_EVENTS.register(
            "heal",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_RELEASE = SOUND_EVENTS.register(
            "black_hole_release",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_PULL = SOUND_EVENTS.register(
            "black_hole_pull",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_ARCHER_HIT = SOUND_EVENTS.register(
            "void_archer_hit",
            SoundEvent::createVariableRangeEvent
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_ARCHER_SHOOT = SOUND_EVENTS.register(
            "void_archer_shoot",
            SoundEvent::createVariableRangeEvent
    );

    // 虚空音效统一配置入口
    public static final SoundSource VOID_SOUND_SOURCE = SoundSource.PLAYERS;
    public static final float ENTER_VOID_VOLUME = 6.0F;
    public static final float ENTER_VOID_PITCH = 1.0F;
    public static final float TELEPORT_PORTAL_ENTER_VOLUME = 6.0F;
    public static final float TELEPORT_PORTAL_ENTER_PITCH = 1.0F;
    public static final float TELEPORT_DEPLOY_START_VOLUME = 6.0F;
    public static final float TELEPORT_DEPLOY_START_PITCH = 1.0F;
    public static final float TELEPORT_DEPLOY_END_VOLUME = 6.0F;
    public static final float TELEPORT_DEPLOY_END_PITCH = 1.0F;
    public static final float OUT_VOID_VOLUME = 6.0F;
    public static final float OUT_VOID_PITCH = 1.0F;
    public static final float LOOP_VOID_VOLUME = 6.0F;
    public static final float LOOP_VOID_PITCH = 1.0F;
    public static final int LOOP_VOID_START_DELAY_TICKS = 4;
    public static final int LOOP_VOID_FADE_IN_TICKS = 8;
    public static final int LOOP_VOID_FADE_OUT_TICKS = -3;
    public static final float PHASE_TURRET_SHOT_VOLUME = 1.0F;
    public static final float HEALTH_PHASE_TURRET_SHOT_VOLUME = 1.0F;
    public static final float HEAL_VOLUME = 1.0F;
    public static final float HEAL_MIN_PITCH = 0.75F;
    public static final float HEAL_MAX_PITCH = 1.35F;
    public static final float BLACK_HOLE_RELEASE_VOLUME = 6.0F;
    public static final float BLACK_HOLE_RELEASE_PITCH = 1.0F;
    public static final float BLACK_HOLE_PULL_VOLUME = 4.0F;
    public static final float BLACK_HOLE_PULL_PITCH = 1.0F;
    public static final float VOID_ARCHER_HIT_VOLUME = 6.0F;
    public static final float VOID_ARCHER_HIT_PITCH = 1.0F;
    public static final float VOID_ARCHER_SHOOT_VOLUME = 6.0F;
    public static final float VOID_ARCHER_SHOOT_PITCH = 1.0F;

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }

    public static void playEnterVoid(Level level, Player player) {
        playAtPlayer(level, player, ENTER_VOID.get(), ENTER_VOID_VOLUME, ENTER_VOID_PITCH);
    }

    public static void playTeleportPortalEnter(Level level, Vec3 position) {
        playAtPos(
                level,
                position,
                TELEPORT_PORTAL_ENTER.get(),
                TELEPORT_PORTAL_ENTER_VOLUME,
                TELEPORT_PORTAL_ENTER_PITCH
        );
    }

    public static void playTeleportDeployStart(Level level, Player player) {
        playAtPlayer(
                level,
                player,
                TELEPORT_DEPLOY_START.get(),
                TELEPORT_DEPLOY_START_VOLUME,
                TELEPORT_DEPLOY_START_PITCH
        );
    }

    public static void playTeleportDeployEnd(Level level, Player player) {
        playAtPlayer(
                level,
                player,
                TELEPORT_DEPLOY_END.get(),
                TELEPORT_DEPLOY_END_VOLUME,
                TELEPORT_DEPLOY_END_PITCH
        );
    }

    public static void playOutVoid(Level level, Player player) {
        playAtPlayer(level, player, OUT_VOID.get(), OUT_VOID_VOLUME, OUT_VOID_PITCH);
    }

    public static void playOutVoid(Level level, LivingEntity entity) {
        playAtEntity(level, entity, OUT_VOID.get(), OUT_VOID_VOLUME, OUT_VOID_PITCH);
    }

    public static void playPhaseTurretShot(Level level, Player player, int emitterIndex) {
        float pitch = switch (Math.floorMod(emitterIndex, 4)) {
            case 0 -> 0.96F;
            case 1 -> 1.03F;
            case 2 -> 0.99F;
            default -> 1.06F;
        };
        playAtPlayer(level, player, PHASE_TURRET_SHOT.get(), PHASE_TURRET_SHOT_VOLUME, pitch);
    }

    public static void playHealthPhaseTurretShot(Level level, Player player, int emitterIndex) {
        float pitch = switch (Math.floorMod(emitterIndex, 4)) {
            case 0 -> 0.96F;
            case 1 -> 1.03F;
            case 2 -> 0.99F;
            default -> 1.06F;
        };
        playAtPlayer(level, player, HEALTH_PHASE_TURRET_SHOT.get(), HEALTH_PHASE_TURRET_SHOT_VOLUME, pitch);
    }

    public static void playHeal(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || player.level().isClientSide()) {
            return;
        }

        player.connection.send(new ClientboundSoundPacket(
                HEAL,
                VOID_SOUND_SOURCE,
                player.getX(), player.getY(), player.getZ(),
                HEAL_VOLUME,
                getHealPitch(target),
                player.getRandom().nextLong()
        ));
    }

    public static void playBlackHoleRelease(Level level, Vec3 center) {
        playAtPos(level, center, BLACK_HOLE_RELEASE.get(), BLACK_HOLE_RELEASE_VOLUME, BLACK_HOLE_RELEASE_PITCH);
    }

    public static void playVoidArcherHit(Level level, Vec3 position) {
        playAtPos(level, position, VOID_ARCHER_HIT.get(), VOID_ARCHER_HIT_VOLUME, VOID_ARCHER_HIT_PITCH);
    }

    public static void playVoidArcherShoot(Level level, Vec3 position) {
        playAtPos(level, position, VOID_ARCHER_SHOOT.get(), VOID_ARCHER_SHOOT_VOLUME, VOID_ARCHER_SHOOT_PITCH);
    }

    private static void playAtPlayer(Level level, Player player, SoundEvent event, float volume, float pitch) {
        playAtEntity(level, player, event, volume, pitch);
    }

    private static void playAtEntity(Level level, LivingEntity entity, SoundEvent event, float volume, float pitch) {
        if (level.isClientSide()) {
            return;
        }

        level.playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                event,
                VOID_SOUND_SOURCE,
                volume,
                pitch
        );
    }

    private static float getHealPitch(LivingEntity target) {
        float maxHealth = target.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return HEAL_MIN_PITCH;
        }

        float health = Math.max(0.0F, Math.min(target.getHealth() / maxHealth, 1.0F));
        return HEAL_MIN_PITCH + (HEAL_MAX_PITCH - HEAL_MIN_PITCH) * health;
    }

    private static void playAtPos(Level level, Vec3 position, SoundEvent event, float volume, float pitch) {
        if (level.isClientSide()) {
            return;
        }

        level.playSound(
                null,
                position.x, position.y, position.z,
                event,
                VOID_SOUND_SOURCE,
                volume,
                pitch
        );
    }
}
