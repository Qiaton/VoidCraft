package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Sound.ModSound;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.CHANNEL;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;

@EventBusSubscriber(modid = VoidCraft.MODID)
public class AssistPhaseTurretModule extends ModuleItem {
    // 辅助炮台的基础数值集中在这里；以后要做配置文件或 UI 调参时，优先替换这些常量/getter。
    private static  double RANGE = 32.0D;
    private static double RANGE_SQR = RANGE * RANGE;
    private static final double SAFE_DISTANCE = 4.0D;
    private static final double SAFE_DISTANCE_SQR = SAFE_DISTANCE * SAFE_DISTANCE;
    private static final int BASE_MODULE_LEVEL = 1;
    private static final int BASE_EMITTER_COUNT = 1;
    private static final float SHOT_DAMAGE = 1.0F;
    private static final float SHOT_DAMAGE_PER_LEVEL = 0.5F;
    private static final int FIRE_INTERVAL_TICKS = 5;
    private static final float SPEED_BOOST_PER_LEVEL = 1F;
    private static final int TARGET_LOCK_TICKS = 20;
    private static final int RECENT_ATTACK_TARGET_TICKS = 100;
    private static final int BURST_ACTIVE_TICKS = 5*20;
    private static final double LOW_HEALTH_TIE_DISTANCE_SQR = 4.0D;
    private static final long CHANNEL_ENERGY_COST = 10L;
    private static final long BURST_ENERGY_COST = 800L;
    private static final long BURST_COOLDOWN_TICKS = 45*20L;
    private static final float BURST_ACTIVE_DURATION_PER_LEVEL = 0.50F;
    private static final float BURST_COOLDOWN_REDUCTION_PER_LEVEL = 0.10F;
    private static final double FIRE_TICK_EPSILON = 1.0E-6D;
    private static final int MAX_DUE_SHOTS_PER_TICK = 20;

    // 按玩家和模块槽位保存自动炮台运行状态，避免不同槽位的锁定目标和发射顺序互相污染。
    private static final Map<UUID, Map<Integer, FireState>> FIRE_STATES = new HashMap<>();

    // 玩家最近主动攻击的目标单独记忆，用于“打错非怪物也反击”的优先级。
    private static final Map<UUID, RecentAttackTarget> RECENT_ATTACK_TARGETS = new HashMap<>();

    private static final VoidBeamInstance.Config SHOT_BEAM = makeShotBeam(
            PhaseTurretModule.VisualColors.SHOT_BEAM_CORE,
            PhaseTurretModule.VisualColors.SHOT_BEAM_GLOW
    );

    private static VoidBeamInstance.Config makeShotBeam(int coreColor, int glowColor) {
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

    public AssistPhaseTurretModule(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    public static void rememberAttack(AttackEntityEvent event) {
        // 这里只记目标，不直接开火；真正是否可打由后面的距离、视线和锁定规则判断。
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target) || target == player) {
            return;
        }

        RECENT_ATTACK_TARGETS.put(
                player.getUUID(),
                new RecentAttackTarget(target.getUUID(), player.tickCount + RECENT_ATTACK_TARGET_TICKS)
        );
    }

    @SubscribeEvent
    public static void tickAssistTurret(PlayerTickEvent.Post event) {
        // 辅助炮台不依赖玩家按住左键，所以使用独立服务端 tick 驱动自动射击。
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            tickFire(player);
        }
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() == null) {
            return;
        }

        ModuleMode mode = stats.mode();
        if (mode == CHANNEL) {
            // CHANNEL 是开关型自动炮台：启动后由 ModuleSkillClock 负责持续耗能。
            if (ModuleSkillClock.hasChannel(player, slot)) {
                ModuleSkillClock.stopChannel(player, slot);
                return;
            }

            long energyCost = stats.channelEnergyCost();
            if (!ModuleSkillClock.tryUseEnergy(player, energyCost)) {
                return;
            }

            stopOtherTurrets(player, slot);
            stopOtherBursts(player, slot);
            ModuleSkillClock.startChannel(player, slot, energyCost);
            getFire(player, slot);
            ModNetworking.sendAssistTurretState(player, true, moduleStack);
            fire(player, moduleStack, slot, stats);
            return;
        }
        if (mode == BURST) {
            if (ModuleSkillClock.hasChannel(player, slot)) {
                ModuleSkillClock.stopChannel(player, slot);
                return;
            }

            boolean cooldownReady = ModuleSkillClock.canUseNow(player, slot);
            if (cooldownReady) {
                ModuleSkillClock.setCooldown(player, slot, stats.burstCooldownTicks());
            } else if (!ModuleSkillClock.tryUseEnergy(player, stats.burstEnergyCost())) {
                return;
            }

            startBurst(player, moduleStack, slot, stats);
        }
    }

    public static void tickFire(ServerPlayer player) {
        // 每 tick 重新读取手表里的模块，模块被移走或手表不在副手时会自然停止。
        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            if (clearFire(player)) {
                ModNetworking.sendAssistTurretState(player, false);
            }
            return;
        }

        NonNullList<ItemStack> items = getWatchModules(watchStack);
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            ItemStack moduleStack = items.get(slot);
            FireState state = findFire(player, slot);
            if (!(moduleStack.getItem() instanceof AssistPhaseTurretModule module)) {
                if (state != null && !ModuleSkillClock.hasChannel(player, slot)) {
                    removeFire(player, slot);
                }
                continue;
            }

            Stats stats = getStats(moduleStack);
            if (stats == null) {
                continue;
            }

            boolean channelActive = ModuleSkillClock.hasChannel(player, slot);
            boolean burstActive = isBurstOn(player, state);
            if (state != null && state.burstUntilTick > 0 && !burstActive) {
                // BURST 到期后只清 burst 状态；如果同槽 CHANNEL 还开着，继续保留 FireState。
                state.burstUntilTick = 0;
                if (!channelActive) {
                    removeFire(player, slot);
                    if (!hasTurret(player, slot)) {
                        ModNetworking.sendAssistTurretState(player, false);
                    }
                    continue;
                }
            }

            if (!channelActive && !burstActive) {
                continue;
            }

            module.fire(player, moduleStack, slot, stats);
        }
    }

    public static boolean hasBurst(ServerPlayer player, int slot) {
        return isBurstOn(player, findFire(player, slot));
    }

    public static boolean hasAny(ServerPlayer player) {
        return hasTurret(player, -1);
    }

    public static void onChannelStop(ServerPlayer player, int slot) {
        FireState state = findFire(player, slot);
        boolean hadFireState = state != null;
        if (state != null) {
            if (isBurstOn(player, state)) {
                // 同槽 burst 还在跑时，不移除 FireState，只清掉 channel 的锁定目标。
                state.lockedTargetId = null;
                state.lockUntilTick = 0;
            } else {
                removeFire(player, slot);
            }
        }
        if ((hadFireState || isAssistSlot(player, slot)) && !hasTurret(player, slot)) {
            ModNetworking.sendAssistTurretState(player, false);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        // 登出时清掉自动炮台和最近攻击目标，避免 UUID 状态跨在线会话残留。
        FIRE_STATES.remove(playerId);
        RECENT_ATTACK_TARGETS.remove(playerId);
    }

    private void startBurst(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats) {
        stopOtherTurrets(player, slot);
        stopOtherBursts(player, slot);

        FireState state = getFire(player, slot);
        state.burstUntilTick = player.tickCount + stats.burstActiveTicks();
        ModNetworking.sendAssistTurretState(player, true, moduleStack);
        fire(player, moduleStack, slot, stats);
    }

    private boolean fire(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats) {
        // 真正开火的统一入口：模式分支只负责算冷却/扣能量，然后进入这里。
        FireState state = getFire(player, slot);
        if (player.tickCount + FIRE_TICK_EPSILON < state.nextFireTick) {
            return false;
        }

        LivingEntity firstTarget = pickTarget(player, state);
        if (firstTarget == null) {
            skipBadShotDebt(player, state);
            return false;
        }

        int shotCount = getDueShotCount(player, state, getFireTicks(moduleStack, stats));
        if (shotCount <= 0) {
            return false;
        }

        int emitterCount = PhaseTurretModule.getEmitterCount(moduleStack);
        boolean fired = false;
        for (int shotIndex = 0; shotIndex < shotCount; shotIndex++) {
            LivingEntity target = shotIndex == 0 ? firstTarget : pickTarget(player, state);
            if (target == null) {
                skipBadShotDebt(player, state);
                break;
            }

            int emitterIndex = Math.floorMod(state.nextEmitterIndex, emitterCount);
            state.nextEmitterIndex = (emitterIndex + 1) % emitterCount;

            hurtTarget(player, target, getDamage(moduleStack, stats, target));
            Vec3 targetPos = getTargetPos(target);
            ModSound.playPhaseTurretShot(player.level(), player, emitterIndex);
            ModNetworking.sendTurretShotFx(player, emitterIndex, targetPos, getBeam(moduleStack, stats, target));
            fired = true;
        }

        return fired;
    }

    private static int getDueShotCount(ServerPlayer player, FireState state, float fireIntervalTicks) {
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

    private static void skipBadShotDebt(ServerPlayer player, FireState state) {
        if (state.nextFireTick <= player.tickCount + FIRE_TICK_EPSILON) {
            state.nextFireTick = player.tickCount + 1.0D;
        }
    }

    protected float getFireTicks(ItemStack moduleStack, Stats stats) {
        return stats == null ? Math.max(1, FIRE_INTERVAL_TICKS) : stats.fireIntervalTicks();
    }

    protected float getDamage(ItemStack moduleStack, Stats stats, LivingEntity target) {
        return stats == null ? SHOT_DAMAGE : stats.shotDamage();
    }

    protected VoidBeamInstance.Config getBeam(ItemStack moduleStack, Stats stats, LivingEntity target) {
        int level = stats == null ? BASE_MODULE_LEVEL : stats.emitterCount();
        if (level <= BASE_MODULE_LEVEL) {
            return SHOT_BEAM;
        }

        return makeShotBeam(
                PhaseTurretModule.VisualColors.shotBeamCoreForLevel(level),
                PhaseTurretModule.VisualColors.shotBeamGlowForLevel(level)
        );
    }

    private static LivingEntity pickTarget(ServerPlayer player, FireState state) {
        // 优先级：安全距离内的怪物 > 正在攻击玩家的怪物 > 锁定目标 > 玩家最近攻击的目标 > 最近/低血量怪物。
        LivingEntity safetyThreat = findCloseThreat(player);
        if (safetyThreat != null) {
            lockTarget(player, state, safetyThreat);
            return safetyThreat;
        }

        // 玩家正在被怪物攻击时，直接抢占当前锁定目标。
        LivingEntity attacker = findAttacker(player);
        if (attacker != null) {
            lockTarget(player, state, attacker);
            return attacker;
        }

        LivingEntity lockedTarget = getLockTarget(player, state);
        if (lockedTarget != null) {
            return lockedTarget;
        }

        LivingEntity recentAttackTarget = getRecentTarget(player);
        if (isGoodTarget(player, recentAttackTarget, true)) {
            lockTarget(player, state, recentAttackTarget);
            return recentAttackTarget;
        }

        LivingEntity hostile = findHostile(player);
        if (hostile != null) {
            lockTarget(player, state, hostile);
            return hostile;
        }

        return null;
    }

    private static LivingEntity findCloseThreat(ServerPlayer player) {
        LivingEntity best = null;
        double bestDistanceSqr = Double.MAX_VALUE;

        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(SAFE_DISTANCE),
                AssistPhaseTurretModule::isHostile
        )) {
            if (!isGoodTarget(player, mob, false)) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(mob);
            if (distanceSqr > SAFE_DISTANCE_SQR) {
                continue;
            }

            // 安全距离内只看谁离玩家最近，先打掉最可能贴脸的威胁。
            if (distanceSqr < bestDistanceSqr) {
                best = mob;
                bestDistanceSqr = distanceSqr;
            }
        }

        return best;
    }

    private static LivingEntity findAttacker(ServerPlayer player) {
        LivingEntity best = null;
        double bestDistanceSqr = Double.MAX_VALUE;

        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(RANGE),
                mob -> mob.getTarget() == player && isHostile(mob)
        )) {
            if (!isGoodTarget(player, mob, false)) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(mob);
            if (distanceSqr < bestDistanceSqr) {
                best = mob;
                bestDistanceSqr = distanceSqr;
            }
        }

        return best;
    }

    private static LivingEntity findHostile(ServerPlayer player) {
        LivingEntity best = null;
        double bestDistanceSqr = Double.MAX_VALUE;
        float bestHealth = Float.MAX_VALUE;

        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(RANGE)
        )) {
            if (!isGoodTarget(player, mob, false)) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(mob);
            float health = mob.getHealth();
            // 默认优先最近；距离很接近时再偏向低血量，避免炮台频繁横跨战场换目标。
            if (distanceSqr < bestDistanceSqr
                    || (Math.abs(distanceSqr - bestDistanceSqr) <= LOW_HEALTH_TIE_DISTANCE_SQR && health < bestHealth)) {
                best = mob;
                bestDistanceSqr = distanceSqr;
                bestHealth = health;
            }
        }

        return best;
    }

    private static LivingEntity getLockTarget(ServerPlayer player, FireState state) {
        if (state.lockedTargetId == null || player.tickCount >= state.lockUntilTick) {
            clearLock(state);
            return null;
        }

        Entity entity = getEntity(player, state.lockedTargetId);
        if (!(entity instanceof LivingEntity target) || !isGoodTarget(player, target, true)) {
            clearLock(state);
            return null;
        }

        return target;
    }

    private static LivingEntity getRecentTarget(ServerPlayer player) {
        RecentAttackTarget recentTarget = RECENT_ATTACK_TARGETS.get(player.getUUID());
        if (recentTarget == null) {
            return null;
        }

        if (player.tickCount >= recentTarget.expiresAtTick()) {
            RECENT_ATTACK_TARGETS.remove(player.getUUID());
            return null;
        }

        Entity entity = getEntity(player, recentTarget.targetId());
        if (!(entity instanceof LivingEntity target)) {
            RECENT_ATTACK_TARGETS.remove(player.getUUID());
            return null;
        }

        return target;
    }

    private static boolean isGoodTarget(ServerPlayer player, LivingEntity target, boolean allowNonMonster) {
        if (target == null || target == player || target.isRemoved() || !target.isAlive() || !target.isPickable()) {
            return false;
        }
        if (target.level() != player.level()) {
            return false;
        }
        if (!allowNonMonster && !isHostile(target)) {
            return false;
        }
        if (player.distanceToSqr(target) > RANGE_SQR) {
            return false;
        }

        return canSee(player, target);
    }

    private static boolean isHostile(LivingEntity target) {
        // 不用 Monster 类判断，幻翼/恶魂这类 MobCategory.MONSTER 也能被自动索敌覆盖。
        return target.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean canSee(ServerPlayer player, LivingEntity target) {
        Vec3 start = player.getEyePosition();
        Vec3 end = getTargetPos(target);
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (blockHit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return start.distanceToSqr(blockHit.getLocation()) + 0.25D >= start.distanceToSqr(end);
    }

    private static Vec3 getTargetPos(LivingEntity target) {
        return target.getBoundingBox().getCenter();
    }

    private static Entity getEntity(ServerPlayer player, UUID entityId) {
        return ((ServerLevel) player.level()).getEntity(entityId);
    }

    private static void lockTarget(ServerPlayer player, FireState state, LivingEntity target) {
        state.lockedTargetId = target.getUUID();
        state.lockUntilTick = player.tickCount + TARGET_LOCK_TICKS;
    }

    private static void clearLock(FireState state) {
        state.lockedTargetId = null;
        state.lockUntilTick = 0;
    }

    private static void hurtTarget(ServerPlayer player, LivingEntity target, float damage) {
        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(makeDamageSource(player), damage);
        target.invulnerableTime = Math.min(target.invulnerableTime, previousInvulnerableTime);
    }

    private static DamageSource makeDamageSource(ServerPlayer player) {
        return player.damageSources().source(getDamageType(player), player, player);
    }

    private static ResourceKey<DamageType> getDamageType(ServerPlayer player) {
        return player.getRandom().nextBoolean()
                ? ModDamageTypes.PHASE_TURRET_SHRED
                : ModDamageTypes.PHASE_TURRET_DISPERSE;
    }

    private static void stopOtherTurrets(ServerPlayer player, int activeSlot) {
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (slot == activeSlot || !ModuleSkillClock.hasChannel(player, slot) || !isTurretSlot(player, slot)) {
                continue;
            }

            ModuleSkillClock.stopChannel(player, slot);
        }
    }

    public static void stopOtherBursts(ServerPlayer player, int activeSlot) {
        // 手动炮台或另一个辅助炮台启动时会调用这里，保证炮台球视觉只有一组主状态。
        Map<Integer, FireState> playerStates = FIRE_STATES.get(player.getUUID());
        if (playerStates == null) {
            return;
        }

        playerStates.entrySet().removeIf(entry -> {
            int slot = entry.getKey();
            FireState state = entry.getValue();
            if (slot == activeSlot || !isBurstOn(player, state)) {
                return false;
            }

            state.burstUntilTick = 0;
            return !ModuleSkillClock.hasChannel(player, slot);
        });
    }

    private static boolean hasTurret(ServerPlayer player, int ignoredSlot) {
        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (slot == ignoredSlot || !isTurretSlot(player, slot)) {
                continue;
            }

            if (ModuleSkillClock.hasChannel(player, slot) || hasBurst(player, slot)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAssistSlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return false;
        }

        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return false;
        }

        return getWatchModules(watchStack).get(slot).getItem() instanceof AssistPhaseTurretModule;
    }

    private static boolean isTurretSlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return false;
        }

        ItemStack watchStack = player.getOffhandItem();
        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return false;
        }

        ItemStack moduleStack = getWatchModules(watchStack).get(slot);
        return moduleStack.getItem() instanceof PhaseTurretModule
                || moduleStack.getItem() instanceof AssistPhaseTurretModule;
    }

    private static NonNullList<ItemStack> getWatchModules(ItemStack watchStack) {
        // ItemContainerContents.EMPTY 没有固定槽位，读取前必须复制进手表模块槽大小的列表。
        ItemContainerContents contents = watchStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        NonNullList<ItemStack> items = NonNullList.withSize(
                PhaseWatch.WATCH_MODULE_SLOT_COUNT,
                ItemStack.EMPTY
        );
        contents.copyInto(items);
        return items;
    }

    private static FireState getFire(ServerPlayer player, int slot) {
        return FIRE_STATES
                .computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                .computeIfAbsent(slot, ignored -> new FireState());
    }

    private static FireState findFire(ServerPlayer player, int slot) {
        Map<Integer, FireState> playerStates = FIRE_STATES.get(player.getUUID());
        if (playerStates == null) {
            return null;
        }

        return playerStates.get(slot);
    }

    private static boolean isBurstOn(ServerPlayer player, FireState state) {
        return state != null && state.burstUntilTick > player.tickCount;
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

    private static boolean clearFire(ServerPlayer player) {
        return FIRE_STATES.remove(player.getUUID()) != null;
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
        int emitterCount = PhaseTurretModule.getEmitterCount(moduleStack);
        int cooldownReductionLevel = 0;
        int activeDurationLevel = 0;
        List<ModuleModifierData> modifiers = data.modifiers();

        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == null) {
                continue;
            }
            if (modifierType == COOLDOWN_REDUCTION) {
                energyEfficiency += 0.20F * modifier.level();
                cooldownReductionLevel += modifier.level();
            }
            if (modifierType == ACTIVE_DURATION) {
                energyEfficiency += 0.20F * modifier.level();
                activeDurationLevel += modifier.level();
            }
            if (modifierType == SPEED_BOOST) {
                fireRate += SPEED_BOOST_PER_LEVEL * modifier.level() * data.level();
            }
        }

        float fireIntervalTicks = FIRE_INTERVAL_TICKS / Math.max(0.01F, fireRate);
        float burstCooldownMultiplier = Math.max(
                0.0F,
                1.0F - BURST_COOLDOWN_REDUCTION_PER_LEVEL * Math.max(0, cooldownReductionLevel)
        );
        float burstActiveDuration = 1.0F
                + BURST_ACTIVE_DURATION_PER_LEVEL * Math.max(0, activeDurationLevel);
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

    @Override
    public ModuleInputMode getInputMode() {
        return ModuleInputMode.CLICK;
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == CHANNEL || mode == BURST;
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
        // Stats 是模块数据到运行数值的边界；UI/配置改动尽量只落在这里或上面的基础 getter。
        public long channelEnergyCost() {
            return Math.max(1L, (long) (CHANNEL_ENERGY_COST / energyEfficiency));
        }

        public long burstEnergyCost() {
            return Math.max(0L, Math.round(BURST_ENERGY_COST * burstCooldownMultiplier));
        }

        public long burstCooldownTicks() {
            return Math.max(1L, Math.round(BURST_COOLDOWN_TICKS * burstCooldownMultiplier));
        }

        public int burstActiveTicks() {
            return Math.max(1, Math.round(BURST_ACTIVE_TICKS * burstActiveDuration));
        }
    }

    private static final class FireState {
        // 自动炮台每槽独立记录发射顺序、冷却、锁定目标和 burst 到期时间。
        private int nextEmitterIndex;
        private double nextFireTick;
        private UUID lockedTargetId;
        private int lockUntilTick;
        private int burstUntilTick;
    }

    private record RecentAttackTarget(UUID targetId, int expiresAtTick) {
        // 只保存 UUID，避免长期持有实体对象；过期或实体消失时会自动清理。
    }
}
