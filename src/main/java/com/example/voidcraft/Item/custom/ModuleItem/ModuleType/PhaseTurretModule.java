package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.CHANNEL;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

public class PhaseTurretModule extends ModuleItem {

    // 手动炮台的基础数值集中在这里；后续接入配置文件时优先替换这些常量或对应的 getter。
    private static final double RANGE = 128.0D;
    private static final double AIM_ASSIST_RADIUS = 0.5D;
    private static final float SHOT_DAMAGE = 1.0F;
    private static final int FIRE_INTERVAL_TICKS = 1;
    private static final long CHANNEL_ENERGY_COST = 1L;

    // 服务端按玩家和模块槽位保存开火状态，客户端只同步视觉和输入请求。
    private static final Map<UUID, Map<Integer, FireState>> FIRE_STATES = new HashMap<>();

    public static final class VisualColors {
        // 炮台球颜色。
        public static final int ORB_CORE = 0xB8FFD2;
        public static final int ORB_RIM = 0x38FF72;

        // 射击光束颜色。
        public static final int SHOT_BEAM_GLOW = 0x38FF72;
        public static final int SHOT_BEAM_CORE = 0xB8FFD2;

        // 球发射瞬间的小相位白光颜色。
        public static final int MUZZLE_FLASH = 0xFFFFFF;

        // 命中点的小相位白光颜色。
        public static final int HIT_FLASH = 0xFFFFFF;

        private VisualColors() {
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
            .coreAlpha(0.94F)
            .glowAlpha(0.50F)
            .lineAlpha(0.78F)
            .glowWidthScale(1.34F)
            .glowHeightScale(1.34F)
            .shaderGlowWidthScale(1.50F)
            .shaderGlowHeightScale(1.50F)
            .shaderCompatOuterGlowGain(1.45F)
            .shaderCompatCoreGain(1.24F)
            .shaderCompatLineGain(1.34F)
            .shaderCompatBloomGain(1.34F)
            .shaderCompatBloomAlphaScale(0.54F)
            .distortionAlpha(0.68F)
            .distortionThickness(1.75F)
            .distortionAmplitude(2.85F)
            .distortionWidthScale(1.28F)
            .distortionHeightScale(1.28F)
            .noiseFrequency(8.6F)
            .noiseScrollSpeed(6.2F)
            .occludedByBlocks(false)
            .build();
    private static final VoidBeamInstance.Config SHOT_BEAM = VoidBeamInstance.Config.builder()
            .lifetimeTicks(6)
            .coreRadius(0.045F)
            .glowRadius(0.17F)
            .startRadiusScale(1.0F)
            .endRadiusScale(0.62F)
            .coreAlpha(0.94F)
            .glowAlpha(0.42F)
            .crossAlphaScale(0.40F)
            .fadeInRatio(0.04F)
            .fadeOutRatio(0.72F)
            .shaderCompatCoreGain(1.20F)
            .shaderCompatGlowGain(1.36F)
            .shaderCompatBloomAlphaScale(0.78F)
            .shaderCompatBloomWidthScale(1.46F)
            .coreColor(VisualColors.SHOT_BEAM_CORE)
            .glowColor(VisualColors.SHOT_BEAM_GLOW)
            .build();

    public static VoidRingInstance.Preset getMuzzleFlashPreset() {
        return MUZZLE_FLASH;
    }

    public static VoidRingInstance.Preset getHitFlashPreset() {
        return HIT_FLASH;
    }

    public PhaseTurretModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() != CHANNEL) {
            return;
        }

        if (ModuleSkillClock.getChannel(player, slot)) {
            ModuleSkillClock.stopChannel(player, slot);
            return;
        }

        long offEnergy = stats.channelEnergyCost();
        if (!ModuleSkillClock.tryUseEnergy(player, offEnergy)) {
            return;
        }

        stopOtherTurretChannels(player, slot);
        AssistPhaseTurretModule.stopOtherBurstTurrets(player, slot);
        // 炮台开关接入通用 channel：持续耗能由 ModuleSkillClock 统一扣。
        ModuleSkillClock.startChannel(player, slot, 0);
        FIRE_STATES
                .computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                .put(slot, new FireState());
        ModNetworking.sendTurretState(player, true);
    }

    public static void tryFireActiveTurret(ServerPlayer player) {
        ActiveTurret activeTurret = findActiveTurret(player);
        if (activeTurret == null) {
            if (!AssistPhaseTurretModule.hasAnyActive(player)) {
                ModNetworking.sendTurretState(player, false);
            }
            return;
        }

        Map<Integer, FireState> playerStates =
                FIRE_STATES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        FireState state = playerStates.computeIfAbsent(activeTurret.slot(), slot -> new FireState());

        if (player.tickCount < state.nextFireTick) {
            return;
        }

        // 射击顺序只在服务端推进，客户端只负责按住左键发“请求开火”。
        int emitterIndex = state.nextEmitterIndex;
        state.nextEmitterIndex = (state.nextEmitterIndex + 1) % PhaseEmitterSlot.FIRE_ORDER.length;
        state.nextFireTick = player.tickCount + activeTurret.module().getFireIntervalTicks(activeTurret.moduleStack());

        ShotResult result = activeTurret.module().fire(
                player,
                activeTurret.moduleStack(),
                emitterIndex
        );

        if (result != null) {
            ModSound.playPhaseTurretShot(player.level(), player, emitterIndex);
            ModNetworking.sendTurretShotFx(player, emitterIndex, result.targetPos(), result.beamConfig());
        }
    }

    public static void setShooting(ServerPlayer player, boolean shooting) {
        ActiveTurret activeTurret = findActiveTurret(player);
        if (activeTurret == null) {
            if (!AssistPhaseTurretModule.hasAnyActive(player)) {
                ModNetworking.sendTurretState(player, false);
            }
            return;
        }

        Map<Integer, FireState> playerStates =
                FIRE_STATES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        FireState state = playerStates.computeIfAbsent(activeTurret.slot(), slot -> new FireState());
        state.shooting = shooting;
    }

    public static void tickAutoFire(ServerPlayer player) {
        ActiveTurret activeTurret = findActiveTurret(player);
        if (activeTurret == null) {
            return;
        }

        Map<Integer, FireState> playerStates = FIRE_STATES.get(player.getUUID());
        if (playerStates == null) {
            return;
        }

        FireState state = playerStates.get(activeTurret.slot());
        if (state == null || !state.shooting) {
            return;
        }

        tryFireActiveTurret(player);
    }

    public static void onChannelStopped(ServerPlayer player, int slot) {
        boolean hadFireState = removeFireState(player, slot);
        if ((hadFireState || isTurretSlot(player, slot)) && !hasActiveTurret(player, slot)) {
            ModNetworking.sendTurretState(player, false);
        }
    }

    public static void clearPlayerState(ServerPlayer player) {
        if (player == null) {
            return;
        }

        // 登出或强制清理时不再依赖手表槽位存在，只按 UUID 移除运行时状态。
        FIRE_STATES.remove(player.getUUID());
    }

    private static void stopOtherTurretChannels(ServerPlayer player, int activeSlot) {
        // 同一时间只保留一个炮台类 channel，避免多个模块同时争用同一套炮台球视觉。
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (slot == activeSlot || !ModuleSkillClock.getChannel(player, slot) || !isTurretSlot(player, slot)) {
                continue;
            }

            ModuleSkillClock.stopChannel(player, slot);
        }
    }

    private static boolean hasActiveTurret(ServerPlayer player, int ignoredSlot) {
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (slot == ignoredSlot || !isTurretSlot(player, slot)) {
                continue;
            }

            if (ModuleSkillClock.getChannel(player, slot) || AssistPhaseTurretModule.hasActiveBurst(player, slot)) {
                return true;
            }
        }

        return false;
    }

    private static ActiveTurret findActiveTurret(ServerPlayer player) {
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
            if (!ModuleSkillClock.getChannel(player, slot)) {
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

    private static boolean removeFireState(ServerPlayer player, int slot) {
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

        float energyEfficiency = 1.0F;
        List<ModuleModifierData> modifiers = data.modifiers();

        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == null) {
                continue;
            }
            if (modifierType == COOLDOWN_REDUCTION) {
                energyEfficiency += 0.15F * modifier.level();
            }
            if (modifierType == ACTIVE_DURATION) {
                energyEfficiency += 0.12F * modifier.level();
            }
        }

        return new Stats(data.moduleMode(), energyEfficiency);
    }

    public ShotResult fire(
            ServerPlayer player,
            ItemStack moduleStack,
            int emitterIndex
    ) {
        PhaseEmitterSlot emitterSlot = PhaseEmitterSlot.byFireIndex(emitterIndex);

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();

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

        EntityHitResult entityHit = rayCastEntity(
                player,
                start,
                blockHitPos,
                searchBox
        );

        if (entityHit != null) {
            Entity hitEntity = entityHit.getEntity();
            if (hitEntity instanceof LivingEntity target) {
                hurtTarget(player, target, getShotDamage(moduleStack, target));
            }

            return new ShotResult(
                    emitterSlot,
                    entityHit.getLocation(),
                    hitEntity.getId(),
                    getShotBeamConfig(moduleStack, emitterSlot, entityHit.getLocation(), hitEntity.getId())
            );
        } else {

            return new ShotResult(
                    emitterSlot,
                    blockHitPos,
                    -1,
                    getShotBeamConfig(moduleStack, emitterSlot, blockHitPos, -1)
            );
        }
    }

    protected VoidBeamInstance.Config getShotBeamConfig(
            ItemStack moduleStack,
            PhaseEmitterSlot emitterSlot,
            Vec3 targetPos,
            int targetEntityId
    ) {
        // 射击光束的细分配置放在模块侧，后续可按修饰词、命中类型或发射球位置分支。
        return SHOT_BEAM;
    }

    protected int getFireIntervalTicks(ItemStack moduleStack) {
        // 射速先沿用基础常量；以后模块词条或配置可以只改这个入口。
        return Math.max(1, FIRE_INTERVAL_TICKS);
    }

    protected float getShotDamage(ItemStack moduleStack, LivingEntity target) {
        // 伤害先沿用基础常量；以后按目标类型、模块词条或配置扩展时从这里分支。
        return SHOT_DAMAGE;
    }

    private static void hurtTarget(ServerPlayer player, LivingEntity target, float damage) {
        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(buildShotDamageSource(player), damage);
        // 炮台伤害自己绕过受击冷却，但不额外延长目标对其他来源的无敌帧。
        target.invulnerableTime = Math.min(target.invulnerableTime, previousInvulnerableTime);
    }

    private static DamageSource buildShotDamageSource(ServerPlayer player) {
        return player.damageSources().source(getShotDamageType(player), player, player);
    }

    private static ResourceKey<DamageType> getShotDamageType(ServerPlayer player) {
        return player.getRandom().nextBoolean()
                ? ModDamageTypes.PHASE_TURRET_SHRED
                : ModDamageTypes.PHASE_TURRET_DISPERSE;
    }

    private static EntityHitResult rayCastEntity(
            ServerPlayer player,
            Vec3 start,
            Vec3 end,
            AABB searchBox
    ) {
        Entity closestEntity = null;
        Vec3 closestHitPos = null;
        double closestDistanceSqr = start.distanceToSqr(end);

        for (Entity entity : player.level().getEntities(player, searchBox, target ->
                target instanceof LivingEntity
                        && target.isAlive()
                        && target.isPickable()
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

    @Override
    public ModuleInputMode getInputMode() {
        // 炮台的 CHANNEL 是点按开关，不走长按释放链。
        return ModuleInputMode.CLICK;
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == CHANNEL;
    }

    public record Stats(ModuleMode mode, float energyEfficiency) {
        // 能量和冷却仍由 ModuleSkillClock 统一处理，模块只暴露计算后的基础消耗。
        public long channelEnergyCost() {
            return Math.max(1L, (long) (CHANNEL_ENERGY_COST / energyEfficiency));
        }
    }

    private static final class FireState {
        // nextEmitterIndex 决定四个炮台球轮流开火；shooting 来自客户端按键同步。
        private int nextEmitterIndex;
        private int nextFireTick;
        private boolean shooting;
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
