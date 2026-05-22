package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Config;
import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.CHANNEL;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

public class PhaseTurretModule extends ModuleItem {

    // 手动炮台的基础数值集中在这里；后续接入配置文件时优先替换这些常量或对应的 getter。
    private static final double RANGE = 512.0D;
    private static final double AIM_ASSIST_RADIUS = 0.5D;
    private static final int BASE_MODULE_LEVEL = 1;
    private static final int BASE_EMITTER_COUNT = 1;
    private static final float SHOT_DAMAGE = 2.5F;
    private static final float SHOT_DAMAGE_PER_LEVEL = 0.5F;
    private static final int FIRE_INTERVAL_TICKS = 5;
    private static final long CHANNEL_ENERGY_COST = 10L;
    private static final long BURST_COOLDOWN_TICKS = 45*20L;
    private static final int BURST_ACTIVE_TICKS = 5*20 ;
    private static final long BURST_ENERGY_COST = 800L;
    private static final float BURST_ACTIVE_DURATION_PER_LEVEL = 0.30F;
    private static final float BURST_COOLDOWN_REDUCTION_PER_LEVEL = 0.10F;
    private static final float VOLLEY_DAMAGE_MULTIPLIER = 0.25F;
    private static final double VOLLEY_SCATTER_SCREEN_RATIO = 0.20D;
    private static final float HIT_FLASH_ALPHA_SCALE = 0.60F;
    private static final double FIRE_TICK_EPSILON = 1.0E-6D;
    private static final int MAX_DUE_SHOTS_PER_TICK = 20;

    // 服务端按玩家和模块槽位保存开火状态，客户端只同步视觉和输入请求。
    private static final Map<UUID, Map<Integer, FireState>> FIRE_STATES = new HashMap<>();

    public static final class VisualColors {
        // 炮台球颜色。
        public static final int ORB_CORE = 0xB8FFD2;
        public static final int ORB_RIM = 0x38FF72;
        public static final int ORB_LEVEL_2_CORE = 0xFFD7D7;
        public static final int ORB_LEVEL_2_RIM = 0xFF7A7A;
        public static final int ORB_LEVEL_3_CORE = 0xFFF4C2;
        public static final int ORB_LEVEL_3_RIM = 0xFFD75E;
        public static final int ORB_LEVEL_4_CORE = 0xFFD6EE;
        public static final int ORB_LEVEL_4_RIM = 0xFF78C8;
        public static final int ORB_LEVEL_5_CORE = 0xD7ECFF;
        public static final int ORB_LEVEL_5_RIM = 0x68B8FF;
        public static final int ORB_VOID_CORE = 0xC4DAF2;
        public static final int ORB_VOID_RIM = 0x6688C8;
        public static final int HEALTH_ORB_CORE = 0xFFD8EA;
        public static final int HEALTH_ORB_RIM = 0xFF79B6;
        public static final int HEALTH_ORB_LEVEL_2_CORE = 0xFFE0EF;
        public static final int HEALTH_ORB_LEVEL_2_RIM = 0xFF86C4;
        public static final int HEALTH_ORB_LEVEL_3_CORE = 0xFFE8F4;
        public static final int HEALTH_ORB_LEVEL_3_RIM = 0xFF94D0;
        public static final int HEALTH_ORB_LEVEL_4_CORE = 0xFFF0F8;
        public static final int HEALTH_ORB_LEVEL_4_RIM = 0xFFA2DA;
        public static final int HEALTH_ORB_LEVEL_5_CORE = 0xFFF6FC;
        public static final int HEALTH_ORB_LEVEL_5_RIM = 0xFFB0E4;
        public static final int HEALTH_ORB_VOID_CORE = 0xFFE6F8;
        public static final int HEALTH_ORB_VOID_RIM = 0xEFA8FF;

        // 射击光束颜色。
        public static final int SHOT_BEAM_GLOW = 0x38FF72;
        public static final int SHOT_BEAM_CORE = 0xB8FFD2;
        public static final int SHOT_BEAM_LEVEL_2_GLOW = 0xFF5E5E;
        public static final int SHOT_BEAM_LEVEL_2_CORE = 0xFFEAEA;
        public static final int SHOT_BEAM_LEVEL_3_GLOW = 0xFFD047;
        public static final int SHOT_BEAM_LEVEL_3_CORE = 0xFFF7D8;
        public static final int SHOT_BEAM_LEVEL_4_GLOW = 0xFF68B8;
        public static final int SHOT_BEAM_LEVEL_4_CORE = 0xFFE4F4;
        public static final int SHOT_BEAM_LEVEL_5_GLOW = 0x5FB8FF;
        public static final int SHOT_BEAM_LEVEL_5_CORE = 0xE2F4FF;
        public static final int SHOT_BEAM_VOID_GLOW = 0x7D94FF;
        public static final int SHOT_BEAM_VOID_CORE = 0xE0EAFF;

        // 球发射瞬间的小相位白光颜色。
        public static final int MUZZLE_FLASH = 0xFFFFFF;

        // 命中点的小相位白光颜色。
        public static final int HIT_FLASH = 0xFFFFFF;

        // 炮台开启/结束时，每个炮台球位置同时闪出的白光。
        public static final int TOGGLE_FLASH = 0xFFFFFF;

        private VisualColors() {
        }

        public static int orbCoreForLevel(int level) {
            return switch (level) {
                case 2 -> ORB_LEVEL_2_CORE;
                case 3 -> ORB_LEVEL_3_CORE;
                case 4 -> ORB_LEVEL_4_CORE;
                case 5 -> ORB_LEVEL_5_CORE;
                default -> level >= 6 ? ORB_VOID_CORE : ORB_CORE;
            };
        }

        public static int orbRimForLevel(int level) {
            return switch (level) {
                case 2 -> ORB_LEVEL_2_RIM;
                case 3 -> ORB_LEVEL_3_RIM;
                case 4 -> ORB_LEVEL_4_RIM;
                case 5 -> ORB_LEVEL_5_RIM;
                default -> level >= 6 ? ORB_VOID_RIM : ORB_RIM;
            };
        }

        public static int healthOrbCoreForLevel(int level) {
            return switch (level) {
                case 2 -> HEALTH_ORB_LEVEL_2_CORE;
                case 3 -> HEALTH_ORB_LEVEL_3_CORE;
                case 4 -> HEALTH_ORB_LEVEL_4_CORE;
                case 5 -> HEALTH_ORB_LEVEL_5_CORE;
                default -> level >= 6 ? HEALTH_ORB_VOID_CORE : HEALTH_ORB_CORE;
            };
        }

        public static int healthOrbRimForLevel(int level) {
            return switch (level) {
                case 2 -> HEALTH_ORB_LEVEL_2_RIM;
                case 3 -> HEALTH_ORB_LEVEL_3_RIM;
                case 4 -> HEALTH_ORB_LEVEL_4_RIM;
                case 5 -> HEALTH_ORB_LEVEL_5_RIM;
                default -> level >= 6 ? HEALTH_ORB_VOID_RIM : HEALTH_ORB_RIM;
            };
        }

        public static int shotBeamCoreForLevel(int level) {
            return switch (level) {
                case 2 -> SHOT_BEAM_LEVEL_2_CORE;
                case 3 -> SHOT_BEAM_LEVEL_3_CORE;
                case 4 -> SHOT_BEAM_LEVEL_4_CORE;
                case 5 -> SHOT_BEAM_LEVEL_5_CORE;
                default -> level >= 6 ? SHOT_BEAM_VOID_CORE : SHOT_BEAM_CORE;
            };
        }

        public static int shotBeamGlowForLevel(int level) {
            return switch (level) {
                case 2 -> SHOT_BEAM_LEVEL_2_GLOW;
                case 3 -> SHOT_BEAM_LEVEL_3_GLOW;
                case 4 -> SHOT_BEAM_LEVEL_4_GLOW;
                case 5 -> SHOT_BEAM_LEVEL_5_GLOW;
                default -> level >= 6 ? SHOT_BEAM_VOID_GLOW : SHOT_BEAM_GLOW;
            };
        }
    }

    public static final class VisualSizes {
        // 炮台球稳定态峰值半径；开关白光按它做倍率，避免以后调球大小时两边脱节。
        public static final float ORB_PEAK_HALF_SIZE = 0.35F;
        public static final float TOGGLE_FLASH_SIZE_SCALE = 1.20F;
        public static final float TOGGLE_FLASH_DISTORTION_SCALE = 2.00F;
        public static final float TOGGLE_FLASH_PEAK_HALF_SIZE =
                ORB_PEAK_HALF_SIZE * TOGGLE_FLASH_SIZE_SCALE;

        private VisualSizes() {
        }
    }

    private static final VoidRingInstance.Preset MUZZLE_FLASH = VoidRingInstance.Preset.builder()
            .renderStyle(VoidRingInstance.Preset.RenderStyle.FLASH)
            .durationTicks(4)
            .peakHoldTicks(0)
            .centerYOffset(0.0F)
            .color(VisualColors.MUZZLE_FLASH)
            .followCameraPitch(true)
            .distortionFollowCameraPitch(true)
            .startHalfHeight(0.08F)
            .peakHalfHeight(0.38F)
            .endHalfHeight(0.10F)
            .startHalfWidth(0.08F)
            .peakHalfWidth(0.38F)
            .endHalfWidth(0.02F)
            .coreAlpha(0.92F)
            .glowAlpha(0.42F)
            .lineAlpha(0.70F)
            .glowWidthScale(1.28F)
            .glowHeightScale(1.28F)
            .shaderGlowWidthScale(1.38F)
            .shaderGlowHeightScale(1.38F)
            .shaderCompatOuterGlowGain(1.35F)
            .shaderCompatCoreGain(1.18F)
            .shaderCompatLineGain(1.20F)
            .shaderCompatBloomGain(1.24F)
            .shaderCompatBloomAlphaScale(0.46F)
            .distortionAlpha(0.42F)
            .distortionThickness(1.18F)
            .distortionAmplitude(1.90F)
            .distortionWidthScale(1.18F)
            .distortionHeightScale(1.18F)
            .noiseFrequency(9.0F)
            .noiseScrollSpeed(5.8F)
            .occludedByBlocks(false)
            .build();
    private static final VoidRingInstance.Preset HIT_FLASH = VoidRingInstance.Preset.builder()
            .renderStyle(VoidRingInstance.Preset.RenderStyle.FLASH)
            .durationTicks(5)
            .peakHoldTicks(1)
            .centerYOffset(0.0F)
            .color(VisualColors.HIT_FLASH)
            .followCameraPitch(true)
            .distortionFollowCameraPitch(true)
            .startHalfHeight(0.16F)
            .peakHalfHeight(0.72F)
            .endHalfHeight(0.20F)
            .startHalfWidth(0.16F)
            .peakHalfWidth(0.72F)
            .endHalfWidth(0.03F)
            .coreAlpha(0.94F * HIT_FLASH_ALPHA_SCALE)
            .glowAlpha(0.50F * HIT_FLASH_ALPHA_SCALE)
            .lineAlpha(0.78F * HIT_FLASH_ALPHA_SCALE)
            .glowWidthScale(1.34F)
            .glowHeightScale(1.34F)
            .shaderGlowWidthScale(1.50F)
            .shaderGlowHeightScale(1.50F)
            .shaderCompatOuterGlowGain(1.45F)
            .shaderCompatCoreGain(1.24F)
            .shaderCompatLineGain(1.34F)
            .shaderCompatBloomGain(1.34F)
            .shaderCompatBloomAlphaScale(0.54F * HIT_FLASH_ALPHA_SCALE)
            .distortionAlpha(0.68F * HIT_FLASH_ALPHA_SCALE)
            .distortionThickness(1.75F)
            .distortionAmplitude(2.85F)
            .distortionWidthScale(1.28F)
            .distortionHeightScale(1.28F)
            .noiseFrequency(8.6F)
            .noiseScrollSpeed(6.2F)
            .occludedByBlocks(false)
            .build();
    private static final VoidRingInstance.Preset TOGGLE_FLASH = VoidRingInstance.Preset.builder()
            .renderStyle(VoidRingInstance.Preset.RenderStyle.FULL)
            .durationTicks(5)
            .peakHoldTicks(1)
            .centerYOffset(0.0F)
            .color(VisualColors.TOGGLE_FLASH)
            .followCameraPitch(true)
            .distortionFollowCameraPitch(true)
            .startHalfHeight(0.12F)
            .peakHalfHeight(VisualSizes.TOGGLE_FLASH_PEAK_HALF_SIZE*2)
            .endHalfHeight(0.08F)
            .startHalfWidth(0.12F)
            .peakHalfWidth(VisualSizes.TOGGLE_FLASH_PEAK_HALF_SIZE*2)
            .endHalfWidth(0.018F)
            .coreAlpha(1.00F)
            .glowAlpha(0.58F)
            .lineAlpha(0.90F)
            .glowWidthScale(1.18F)
            .glowHeightScale(1.18F)
            .shaderGlowWidthScale(1.34F)
            .shaderGlowHeightScale(1.34F)
            .shaderCompatOuterGlowGain(1.42F)
            .shaderCompatCoreGain(1.24F)
            .shaderCompatLineGain(1.30F)
            .shaderCompatBloomGain(1.30F)
            .shaderCompatBloomAlphaScale(0.56F)
            .distortionAlpha(0.96F)
            .distortionThickness(2.40F)
            .distortionAmplitude(4.20F)
            .distortionWidthScale(VisualSizes.TOGGLE_FLASH_DISTORTION_SCALE)
            .distortionHeightScale(VisualSizes.TOGGLE_FLASH_DISTORTION_SCALE)
            .noiseFrequency(8.4F)
            .noiseScrollSpeed(6.0F)
            .occludedByBlocks(false)
            .build();
    private static final VoidBeamInstance.Config SHOT_BEAM = makeShotBeam(
            VisualColors.SHOT_BEAM_CORE,
            VisualColors.SHOT_BEAM_GLOW
    );

    protected static VoidBeamInstance.Config makeShotBeam(int coreColor, int glowColor) {
        return VoidBeamInstance.Config.builder()
            .lifetimeTicks(6)
            .coreRadius(0.045F)
            .glowRadius(0.17F)
            .startRadiusScale(1.0F)
            .endRadiusScale(0.62F)
            .coreAlpha(0.94F)
            .glowAlpha(0.24F)
            .crossAlphaScale(0.40F)
            .fadeInRatio(0.04F)
            .fadeOutRatio(0.72F)
            .shaderCompatCoreGain(0.78F)
            .shaderCompatGlowGain(0.42F)
            .shaderCompatBloomAlphaScale(0.12F)
            .shaderCompatBloomWidthScale(0.82F)
            .coreColor(coreColor)
            .glowColor(glowColor)
            .build();
    }

    public static VoidRingInstance.Preset getMuzzleFlash() {
        return MUZZLE_FLASH;
    }

    public static VoidRingInstance.Preset getHitFlash() {
        return HIT_FLASH;
    }

    public static VoidRingInstance.Preset getHitFlash(int color) {
        int actualColor = color & 0xFFFFFF;
        if (actualColor == VisualColors.HIT_FLASH) {
            return HIT_FLASH;
        }

        return HIT_FLASH.toBuilder()
                .color(actualColor)
                .build();
    }

    public static VoidRingInstance.Preset getToggleFlash() {
        return TOGGLE_FLASH;
    }

    public static int getEmitterCount() {
        return PhaseEmitterSlot.normalizeCount(Config.getPhaseTurretEmitterCount());
    }

    public static int getEmitterCount(ItemStack moduleStack) {
        Stats stats = getStats(moduleStack);
        return stats == null ? PhaseEmitterSlot.normalizeCount(BASE_EMITTER_COUNT) : stats.emitterCount();
    }

    public PhaseTurretModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() == null) {
            return;
        }

        if (stats.mode() == BURST) {
            useBurst(player, moduleStack, slot, stats);
            return;
        }

        if (stats.mode() != CHANNEL) {
            return;
        }

        useChannel(player, moduleStack, slot, stats);
    }

    private void useChannel(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats) {
        if (ModuleSkillClock.hasChannel(player, slot)) {
            ModuleSkillClock.stopChannel(player, slot);
            return;
        }

        long offEnergy = stats.channelEnergyCost();
        if (!ModuleSkillClock.tryUseEnergy(player, offEnergy)) {
            return;
        }

        stopOtherTurrets(player, slot);
        AssistPhaseTurretModule.stopOtherBursts(player, slot);
        // 炮台开关接入通用 channel：持续耗能由 ModuleSkillClock 统一扣。
        ModuleSkillClock.startChannel(player, slot, offEnergy);
        FIRE_STATES
                .computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                .put(slot, new FireState());
        ModNetworking.sendTurretState(player, true, moduleStack);
    }

    private void useBurst(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats) {
        if (ModuleSkillClock.hasChannel(player, slot)) {
            ModuleSkillClock.stopChannel(player, slot);
            return;
        }

        boolean cooldownReady = ModuleSkillClock.canUseNow(player, slot);
        if (!cooldownReady && !ModuleSkillClock.tryUseEnergy(player, stats.burstEnergyCost())) {
            return;
        }

        stopOtherTurrets(player, slot);
        AssistPhaseTurretModule.stopOtherBursts(player, slot);
        // BURST 只临时开启手动炮台姿态，不额外走 channel 每 tick 能量消耗。
        ModuleSkillClock.startChannel(player, slot, 0);
        int activeTicks = stats.burstActiveTicks();
        ModuleSkillClock.startRunCooldown(player, slot, activeTicks, cooldownReady ? stats.burstCooldownTicks() : 0L);
        FireState state = new FireState();
        state.burstUntilTick = player.tickCount + activeTicks;
        FIRE_STATES
                .computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                .put(slot, state);
        ModNetworking.sendTurretState(player, true, moduleStack);
    }

    public static void tryFireTurret(ServerPlayer player) {
        ActiveTurret activeTurret = getActiveTurret(player);
        if (activeTurret == null) {
            return;
        }

        activeTurret.module().shootLeft(player, activeTurret.moduleStack(), getFireState(player, activeTurret.slot()));
    }

    private static void tryVolley(ServerPlayer player) {
        ActiveTurret activeTurret = getActiveTurret(player);
        if (activeTurret == null) {
            return;
        }

        activeTurret.module().shootRight(player, activeTurret.moduleStack(), getFireState(player, activeTurret.slot()));
    }

    private static ActiveTurret getActiveTurret(ServerPlayer player) {
        ActiveTurret activeTurret = findTurret(player);
        if (activeTurret == null && !AssistPhaseTurretModule.hasAny(player)) {
            ModNetworking.sendTurretState(player, false);
        }
        return activeTurret;
    }

    private static FireState getFireState(ServerPlayer player, int slot) {
        Map<Integer, FireState> playerStates =
                FIRE_STATES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        return playerStates.computeIfAbsent(slot, ignored -> new FireState());
    }

    protected void shootLeft(ServerPlayer player, ItemStack moduleStack, FireState state) {
        int shotCount = getDueShotCount(player, state, getFireTicks(moduleStack));
        if (shotCount <= 0) {
            return;
        }

        int emitterCount = getEmitterCount(moduleStack);
        for (int shotIndex = 0; shotIndex < shotCount; shotIndex++) {
            int emitterIndex = nextEmitter(state, emitterCount);
            ShotResult result = shoot(
                    player,
                    moduleStack,
                    emitterIndex,
                    player.getLookAngle(),
                    1.0F,
                    false
            );

            sendShotResult(player, emitterIndex, result);
        }
    }

    protected void shootRight(ServerPlayer player, ItemStack moduleStack, FireState state) {
        if (!canShootVolley(player, state, getFireTicks(moduleStack))) {
            return;
        }

        int emitterCount = getEmitterCount(moduleStack);
        // 右键走独立齐射分支：每次只打一轮，不补旧欠账，避免卡顿后把整串齐射一次性补回来。
        for (int emitterIndex = 0; emitterIndex < emitterCount; emitterIndex++) {
            ShotResult result = shoot(
                    player,
                    moduleStack,
                    emitterIndex,
                    getVolleyLook(player),
                    VOLLEY_DAMAGE_MULTIPLIER,
                    true
            );

            sendShotResult(player, emitterIndex, result);
        }
    }

    protected static int getDueShotCount(ServerPlayer player, FireState state, float fireIntervalTicks) {
        double interval = Math.max(0.05D, fireIntervalTicks);
        if (state.nextFireTick <= 0.0D || state.nextFireTick < player.tickCount - 1.0D) {
            state.nextFireTick = player.tickCount;
        }

        int shotCount = 0;
        while (player.tickCount + FIRE_TICK_EPSILON >= state.nextFireTick
                && shotCount < MAX_DUE_SHOTS_PER_TICK) {
            state.nextFireTick += interval;
            shotCount++;
        }

        if (shotCount >= MAX_DUE_SHOTS_PER_TICK
                && player.tickCount + FIRE_TICK_EPSILON >= state.nextFireTick) {
            state.nextFireTick = player.tickCount + interval;
        }

        return shotCount;
    }

    private static boolean canShootVolley(ServerPlayer player, FireState state, float fireIntervalTicks) {
        double interval = Math.max(0.05D, fireIntervalTicks);
        if (state.nextVolleyTick <= 0.0D || state.nextVolleyTick < player.tickCount - 1.0D) {
            state.nextVolleyTick = player.tickCount;
        }

        if (player.tickCount + FIRE_TICK_EPSILON < state.nextVolleyTick) {
            return false;
        }

        state.nextVolleyTick = player.tickCount + interval;
        return true;
    }

    protected static int nextEmitter(FireState state, int emitterCount) {
        int emitterIndex = Math.floorMod(state.nextEmitterIndex, emitterCount);
        state.nextEmitterIndex = (emitterIndex + 1) % emitterCount;
        return emitterIndex;
    }

    public static void setInputState(ServerPlayer player, boolean shooting, boolean volleyShooting) {
        ActiveTurret activeTurret = findTurret(player);
        if (activeTurret == null) {
            if (!AssistPhaseTurretModule.hasAny(player)) {
                ModNetworking.sendTurretState(player, false);
            }
            return;
        }

        Map<Integer, FireState> playerStates =
                FIRE_STATES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        FireState state = playerStates.computeIfAbsent(activeTurret.slot(), slot -> new FireState());
        state.shooting = shooting;
        state.volleyShooting = volleyShooting;
    }

    public static void setShooting(ServerPlayer player, boolean shooting) {
        setInputState(player, shooting, false);
    }

    public static void tickFire(ServerPlayer player) {
        ActiveTurret activeTurret = findTurret(player);
        if (activeTurret == null) {
            return;
        }

        Map<Integer, FireState> playerStates = FIRE_STATES.get(player.getUUID());
        if (playerStates == null) {
            return;
        }

        FireState state = playerStates.get(activeTurret.slot());
        if (state == null) {
            return;
        }

        if (isBurstDone(player, state)) {
            ModuleSkillClock.stopChannel(player, activeTurret.slot());
            return;
        }

        if (!state.shooting && !state.volleyShooting) {
            return;
        }

        if (state.volleyShooting) {
            tryVolley(player);
            return;
        }

        tryFireTurret(player);
    }

    private static boolean isBurstDone(ServerPlayer player, FireState state) {
        return state.burstUntilTick > 0 && player.tickCount >= state.burstUntilTick;
    }

    public static void onChannelStop(ServerPlayer player, int slot) {
        boolean hadFireState = removeFire(player, slot);
        if ((hadFireState || isTurretSlot(player, slot)) && !hasTurret(player, slot)) {
            ModNetworking.sendTurretState(player, false);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }

        // 登出或强制清理时不再依赖手表槽位存在，只按 UUID 移除运行时状态。
        FIRE_STATES.remove(player.getUUID());
    }

    private static void stopOtherTurrets(ServerPlayer player, int activeSlot) {
        // 同一时间只保留一个炮台类 channel，避免多个模块同时争用同一套炮台球视觉。
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (slot == activeSlot || !ModuleSkillClock.hasChannel(player, slot) || !isTurretSlot(player, slot)) {
                continue;
            }

            ModuleSkillClock.stopChannel(player, slot);
        }
    }

    private static boolean hasTurret(ServerPlayer player, int ignoredSlot) {
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (slot == ignoredSlot || !isTurretSlot(player, slot)) {
                continue;
            }

            if (ModuleSkillClock.hasChannel(player, slot) || AssistPhaseTurretModule.hasBurst(player, slot)) {
                return true;
            }
        }

        return false;
    }

    private static ActiveTurret findTurret(ServerPlayer player) {
        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return null;
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

        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (!ModuleSkillClock.hasChannel(player, slot)) {
                continue;
            }

            ItemStack moduleStack = items.get(slot);
            if (moduleStack.getItem() instanceof PhaseTurretModule module) {
                // 手动炮台只接管自己的模块，辅助炮台由 AssistPhaseTurretModule 自己的 tick 处理。
                return new ActiveTurret(slot, moduleStack, module);
            }
        }

        return null;
    }

    private static boolean isTurretSlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return false;
        }

        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return false;
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

        ItemStack moduleStack = items.get(slot);
        return moduleStack.getItem() instanceof PhaseTurretModule
                || moduleStack.getItem() instanceof AssistPhaseTurretModule;
    }

    private static boolean removeFire(ServerPlayer player, int slot) {
        Map<Integer, FireState> playerStates = FIRE_STATES.get(player.getUUID());
        if (playerStates == null) {
            return false;
        }

        boolean removed = playerStates.remove(slot) != null;
        if (playerStates.isEmpty()) {
            FIRE_STATES.remove(player.getUUID());
        }

        return removed;
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) {
            return null;
        }

        int moduleLevel = Math.max(BASE_MODULE_LEVEL, data.level());
        float energyEfficiency = 1.0F;
        float fireRate = 1.0F;
        float shotDamage = SHOT_DAMAGE + (moduleLevel - BASE_MODULE_LEVEL) * SHOT_DAMAGE_PER_LEVEL;
        int emitterCount = PhaseEmitterSlot.normalizeCount(BASE_EMITTER_COUNT + moduleLevel - BASE_MODULE_LEVEL);
        int cooldownReductionLevel = 0;
        int activeDurationLevel = 0;
        List<ModuleModifierData> modifiers = data.modifiers();

        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == null) {
                continue;
            }
            if (modifierType == COOLDOWN_REDUCTION) {
                energyEfficiency = addLess(energyEfficiency, modifier.level(), 0.20F);
                cooldownReductionLevel += modifier.level();
            }
            if (modifierType == ACTIVE_DURATION) {
                energyEfficiency = addLess(energyEfficiency, modifier.level(), 0.2F);
                activeDurationLevel += modifier.level();
            }
            if (modifierType == SPEED_BOOST) {
                fireRate += fireRate * modifier.level() * 0.3F;
            }
        }

        float fireIntervalTicks = FIRE_INTERVAL_TICKS / Math.max(0.01F, fireRate);
        float burstCooldownMultiplier = 1.0F / addLess(1.0F, Math.max(0, cooldownReductionLevel), BURST_COOLDOWN_REDUCTION_PER_LEVEL);
        float burstActiveDuration = 1.0F + Math.max(0, activeDurationLevel) * BURST_ACTIVE_DURATION_PER_LEVEL;
        return new Stats(
                data.moduleMode(),
                energyEfficiency,
                shotDamage,
                fireIntervalTicks,
                emitterCount,
                burstCooldownMultiplier,
                burstActiveDuration
        );
    }

    public ShotResult fire(
            ServerPlayer player,
            ItemStack moduleStack,
            int emitterIndex
    ) {
        return shoot(player, moduleStack, emitterIndex, player.getLookAngle(), 1.0F, false);
    }

    protected ShotResult shoot(
            ServerPlayer player,
            ItemStack moduleStack,
            int emitterIndex,
            Vec3 shotLook,
            float damageMultiplier,
            boolean right
    ) {
        PhaseEmitterSlot emitterSlot = PhaseEmitterSlot.byFireIndex(emitterIndex, getEmitterCount(moduleStack));

        Vec3 start = player.getEyePosition();
        Vec3 look = fixLook(player, shotLook);

        Vec3 end = start.add(look.scale(RANGE));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 blockHitPos = blockHit.getLocation();

        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(1.0D);

        EntityHitResult entityHit = rayEntity(
                player,
                start,
                blockHitPos,
                searchBox
        );

        if (entityHit != null) {
            Entity hitEntity = entityHit.getEntity();
            Entity damageTarget = getDamageTarget(hitEntity);
            if (damageTarget != null) {
                hitTarget(
                        player,
                        moduleStack,
                        hitEntity,
                        damageTarget,
                        getDamage(moduleStack, damageTarget) * Math.max(0.0F, damageMultiplier),
                        right
                );
            }

            return new ShotResult(
                    emitterSlot,
                    entityHit.getLocation(),
                    hitEntity.getId(),
                    getBeam(moduleStack, emitterSlot, entityHit.getLocation(), hitEntity.getId(), right)
            );
        } else {

            return new ShotResult(
                    emitterSlot,
                    blockHitPos,
                    -1,
                    getBeam(moduleStack, emitterSlot, blockHitPos, -1, right)
            );
        }
    }

    protected static void sendShotResult(ServerPlayer player, int emitterIndex, ShotResult result) {
        if (result == null) {
            return;
        }

        ModSound.playPhaseTurretShot(player.level(), player, emitterIndex);
        ModNetworking.sendTurretShotFx(player, emitterIndex, result.targetPos(), result.beamConfig());
    }

    private static Vec3 getVolleyLook(ServerPlayer player) {
        Vec3 look = fixLook(player, player.getLookAngle());
        Vec3 right = getRight(look);
        Vec3 up = right.cross(look);
        if (up.lengthSqr() < 1.0E-8D) {
            up = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            up = up.normalize();
        }

        RandomSource random = player.getRandom();
        double radius = Math.sqrt(random.nextDouble()) * VOLLEY_SCATTER_SCREEN_RATIO;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double xOffset = Math.cos(angle) * radius;
        double yOffset = Math.sin(angle) * radius;

        return look
                .add(right.scale(xOffset))
                .add(up.scale(yOffset))
                .normalize();
    }

    private static Vec3 fixLook(ServerPlayer player, Vec3 look) {
        Vec3 actualLook = look == null ? player.getLookAngle() : look;
        if (actualLook.lengthSqr() < 1.0E-8D) {
            return player.getLookAngle();
        }

        return actualLook.normalize();
    }

    private static Vec3 getRight(Vec3 look) {
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-8D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }

        return right.normalize();
    }

    protected VoidBeamInstance.Config getBeam(
            ItemStack moduleStack,
            PhaseEmitterSlot emitterSlot,
            Vec3 targetPos,
            int targetEntityId
    ) {
        return getBeam(moduleStack, emitterSlot, targetPos, targetEntityId, false);
    }

    protected VoidBeamInstance.Config getBeam(
            ItemStack moduleStack,
            PhaseEmitterSlot emitterSlot,
            Vec3 targetPos,
            int targetEntityId,
            boolean right
    ) {
        // 手动炮台的光束跟随主等级调色，和同等级炮台球保持一组视觉语言。
        Stats stats = getStats(moduleStack);
        int level = stats == null ? BASE_MODULE_LEVEL : stats.emitterCount();
        if (level <= BASE_MODULE_LEVEL) {
            return SHOT_BEAM;
        }

        return makeShotBeam(
                VisualColors.shotBeamCoreForLevel(level),
                VisualColors.shotBeamGlowForLevel(level)
        );
    }

    protected float getFireTicks(ItemStack moduleStack) {
        Stats stats = getStats(moduleStack);
        return stats == null ? Math.max(1, FIRE_INTERVAL_TICKS) : stats.fireIntervalTicks();
    }

    protected float getDamage(ItemStack moduleStack, LivingEntity target) {
        Stats stats = getStats(moduleStack);
        return stats == null ? SHOT_DAMAGE : stats.shotDamage();
    }

    protected float getDamage(ItemStack moduleStack, Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            return getDamage(moduleStack, livingTarget);
        }

        return SHOT_DAMAGE;
    }

    protected boolean hitTarget(
            ServerPlayer player,
            ItemStack moduleStack,
            Entity hitEntity,
            Entity target,
            float amount,
            boolean right
    ) {
        return hurtTarget(player, hitEntity, amount);
    }

    private static boolean hurtTarget(ServerPlayer player, Entity hitEntity, float damage) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        DamageSource damageSource = makeDamageSource(player);
        if (hitEntity instanceof PartEntity<?> partEntity) {
            // 多部件实体优先让被命中的部件自己处理伤害，保留龙头/身体等部位自己的转发规则。
            if (hurtPart(serverLevel, partEntity, damageSource, damage)) {
                return true;
            }

            return hurtRealTarget(serverLevel, partEntity.getParent(), damageSource, damage);
        }

        return hurtRealTarget(serverLevel, hitEntity, damageSource, damage);
    }

    private static boolean hurtPart(
            ServerLevel serverLevel,
            PartEntity<?> partEntity,
            DamageSource damageSource,
            float damage
    ) {
        Entity parent = partEntity.getParent();
        if (parent instanceof LivingEntity livingParent) {
            int previousInvulnerableTime = livingParent.invulnerableTime;
            livingParent.invulnerableTime = 0;
            boolean hurt = partEntity.hurtServer(serverLevel, damageSource, damage);
            // 部件伤害最终常会转发到父实体；这里清的是父实体无敌帧，末影龙也走这条路。
            livingParent.invulnerableTime = Math.min(livingParent.invulnerableTime, previousInvulnerableTime);
            return hurt;
        }

        return partEntity.hurtServer(serverLevel, damageSource, damage);
    }

    private static boolean hurtRealTarget(
            ServerLevel serverLevel,
            Entity target,
            DamageSource damageSource,
            float damage
    ) {
        if (target instanceof LivingEntity livingTarget) {
            return hurtLiving(serverLevel, livingTarget, damageSource, damage);
        }

        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }

        return target.hurtServer(serverLevel, damageSource, damage);
    }

    private static boolean hurtLiving(
            ServerLevel serverLevel,
            LivingEntity target,
            DamageSource damageSource,
            float damage
    ) {
        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        boolean hurt = target.hurtServer(serverLevel, damageSource, damage);
        // 炮台伤害自己绕过受击冷却，但不额外延长目标对其他来源的无敌帧。
        target.invulnerableTime = Math.min(target.invulnerableTime, previousInvulnerableTime);
        return hurt;
    }

    private static DamageSource makeDamageSource(ServerPlayer player) {
        return player.damageSources().source(getDamageType(player), player, player);
    }

    private static ResourceKey<DamageType> getDamageType(ServerPlayer player) {
        return player.getRandom().nextBoolean()
                ? ModDamageTypes.PHASE_TURRET_SHRED
                : ModDamageTypes.PHASE_TURRET_DISPERSE;
    }

    private static EntityHitResult rayEntity(
            ServerPlayer player,
            Vec3 start,
            Vec3 end,
            AABB searchBox
    ) {
        Entity closestEntity = null;
        Vec3 closestHitPos = null;
        double closestDistanceSqr = start.distanceToSqr(end);

        for (Entity entity : player.level().getEntities(
                player,
                searchBox,
                target -> canRayHit(player, target)
        )) {
            // 炮台光束稍微放宽命中盒，减少擦边空枪，但不做自动锁敌。
            AABB targetBox = entity.getBoundingBox().inflate(entity.getPickRadius() + AIM_ASSIST_RADIUS);

            Optional<Vec3> optionalHit = targetBox.clip(start, end);

            if (targetBox.contains(start)) {
                double distanceSqr = 0.0D;

                if (distanceSqr < closestDistanceSqr) {
                    closestEntity = entity;
                    closestHitPos = start;
                    closestDistanceSqr = distanceSqr;
                }
            } else if (optionalHit.isPresent()) {
                Vec3 hitPos = optionalHit.get();
                double distanceSqr = start.distanceToSqr(hitPos);

                if (distanceSqr < closestDistanceSqr) {
                    closestEntity = entity;
                    closestHitPos = hitPos;
                    closestDistanceSqr = distanceSqr;
                }
            }
        }

        if (closestEntity == null) {
            return null;
        }

        return new EntityHitResult(closestEntity, closestHitPos);
    }

    private static boolean canRayHit(ServerPlayer player, Entity target) {
        if (target == null
                || target == player
                || target.is(player)
                || target.isRemoved()
                || target.level() != player.level()
                || !target.isPickable()) {
            return false;
        }

        if (target instanceof LivingEntity livingTarget) {
            return livingTarget.isAlive();
        }

        if (target instanceof PartEntity<?> partEntity) {
            return isGoodPartParent(player, partEntity.getParent());
        }

        return false;
    }

    private static Entity getDamageTarget(Entity hitEntity) {
        if (hitEntity instanceof PartEntity<?> partEntity && isGoodPartParent(null, partEntity.getParent())) {
            return partEntity.getParent();
        }

        if (hitEntity instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
            return livingTarget;
        }

        return null;
    }

    private static boolean isGoodPartParent(ServerPlayer player, Entity parent) {
        if (parent == null || parent.isRemoved() || !parent.isAlive()) {
            return false;
        }

        return player == null || (parent != player && parent.level() == player.level());
    }

    @Override
    public ModuleInputMode getInputMode() {
        // 炮台的 CHANNEL 是点按开关，不走长按释放链。
        return ModuleInputMode.CLICK;
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == CHANNEL || mode == BURST;
    }

    @Override
    protected Component getModifierDisplayName(ModuleData data, ModuleModifierData modifierData) {
        ModuleModifierType modifierType = modifierData.type();
        if (modifierType == SPEED_BOOST) {
            return Component.translatable("module_modifier_type.void_craft.fire_rate_bonus");
        }
        if (data.moduleMode() == CHANNEL && (modifierType == COOLDOWN_REDUCTION || modifierType == ACTIVE_DURATION)) {
            return Component.translatable("module_modifier_type.void_craft.energy_reduction");
        }
        return super.getModifierDisplayName(data, modifierData);
    }

    public boolean isHealthVisual() {
        return false;
    }

    public static boolean hasHealthVisual(ItemStack moduleStack) {
        return moduleStack.getItem() instanceof PhaseTurretModule module && module.isHealthVisual();
    }

    public record Stats(
            ModuleMode mode,
            float energyEfficiency,
            float shotDamage,
            float fireIntervalTicks,
            int emitterCount,
            float burstCooldownMultiplier,
            float burstActiveDuration
    ) {
        // 能量和冷却仍由 ModuleSkillClock 统一处理，模块只暴露计算后的基础消耗。
        public long channelEnergyCost() {
            return Math.max(1L, (long) (CHANNEL_ENERGY_COST / energyEfficiency));
        }

        public long burstCooldownTicks() {
            return Math.max(1L, Math.round(BURST_COOLDOWN_TICKS * burstCooldownMultiplier));
        }

        public long burstEnergyCost() {
            return Math.max(0L, Math.round(BURST_ENERGY_COST * burstCooldownMultiplier));
        }

        public int burstActiveTicks() {
            return Math.max(1, Math.round(BURST_ACTIVE_TICKS * burstActiveDuration));
        }
    }

    protected static final class FireState {
        // nextFireTick 保留小数，3.33 tick/发会自然累积成 3/3/4 tick 的节奏。
        private int nextEmitterIndex;
        private double nextFireTick;
        private double nextVolleyTick;
        private boolean shooting;
        private boolean volleyShooting;
        private int burstUntilTick;
    }

    private record ActiveTurret(int slot, ItemStack moduleStack, PhaseTurretModule module) {
    }

    public record ShotResult(
            PhaseEmitterSlot emitterSlot,
            Vec3 targetPos,
            int targetEntityId,
            VoidBeamInstance.Config beamConfig
    ) {
    }
}
