package com.example.voidcraft.Custom.Behavior; // 这个类属于虚空射手附魔的主要逻辑入口。

import com.example.voidcraft.Custom.Behavior.Mixin.AbstractArrowAccessor; // 用来读取和回写箭的基础伤害。
import com.example.voidcraft.Effect.VoidBlackHoleInstance; // 黑洞特效实例配置类型。
import com.example.voidcraft.Effect.VoidRingInstance; // 白光特效预设类型。
import com.example.voidcraft.Effect.VoidTrailInstance; // 拉丝特效预设类型。
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.network.ModNetworking; // 发送白光和拉丝网络包的工具类。
import net.minecraft.core.Holder; // 附魔 Holder 类型。
import net.minecraft.core.HolderLookup; // 注册表查询器类型。
import net.minecraft.core.registries.Registries; // 原版注册表常量。
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel; // 服务端世界类型。
import net.minecraft.util.Mth; // 数学工具类。
import net.minecraft.world.damagesource.DamageSource; // 伤害来源类型。
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity; // 实体基类。
import net.minecraft.world.entity.LivingEntity; // 生物实体基类。
import net.minecraft.world.entity.projectile.Projectile; // 投射物基类。
import net.minecraft.world.entity.projectile.arrow.AbstractArrow; // 箭实体基类。
import net.minecraft.world.item.ItemStack; // 物品堆类型。
import net.minecraft.world.item.enchantment.Enchantment; // 附魔类型。
import net.minecraft.world.item.enchantment.EnchantmentHelper; // 复用原版弓类伤害附魔结算。
import net.minecraft.world.phys.AABB; // 范围盒类型。
import net.minecraft.world.phys.EntityHitResult; // 实体命中结果类型。
import net.minecraft.world.phys.HitResult; // 通用命中结果类型。
import net.minecraft.world.phys.Vec3; // 三维向量类型。
import net.neoforged.bus.api.SubscribeEvent; // 事件订阅注解。
import net.neoforged.fml.common.EventBusSubscriber; // 自动注册事件监听的注解。
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent; // 实体进入世界事件。
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent; // 实体离开世界事件。
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent; // 投射物命中事件。
import net.neoforged.neoforge.event.tick.EntityTickEvent; // 实体 tick 事件。
import net.neoforged.neoforge.event.tick.LevelTickEvent; // 世界 tick 事件。
import net.neoforged.neoforge.event.level.LevelEvent; // 世界卸载时清理挂起任务。

import java.util.ArrayDeque; // 命中后的 AOE 任务分批排队。
import java.util.ArrayList; // 候选目标列表。
import java.util.Comparator; // 大量目标时按距离限制 AOE 处理数量。
import java.util.Deque; // 每个世界对应一个待处理 AOE 队列。
import java.util.HashMap; // 用来保存箭被改速前的原始基础伤害。
import java.util.HashSet; // 用来记录已经处理过的箭。
import java.util.List; // 实体列表类型。
import java.util.Map; // 箭 UUID 到原始基础伤害的映射。
import java.util.Optional; // 用来安全处理附魔查询结果。
import java.util.Set; // 集合接口。
import java.util.UUID; // 箭的唯一标识。

@EventBusSubscriber // 告诉 NeoForge 自动把这个类里的事件方法注册出去。
public class VoidArcher { // 虚空射手附魔逻辑主类。

    // ===== 调参入口：飞行 =====
    private static final float SPEED_MULTIPLIER_PER_LEVEL = 2.0F; // 每级附魔给箭带来的速度倍率。
    private static final int TRAIL_PRIME_TICKS = 10; // 前几 tick 重复补发拉丝包，避免高速箭来不及接上。
    private static final double TRAIL_SEED_LENGTH = 0.5D; // 初始补点只回退一小段，避免高速箭从玩家身后拉出光带。
    private static final int MAX_ARROW_LIFETIME_TICKS = 80; // 虚空箭最长飞行多久，超时后直接自清。

    // ===== 调参入口：命中 =====
    private static final float LIGHT_SIZE_MULTIPLIER_PER_LEVEL = 3.5F; // 白光每升一级的尺寸倍率。
    private static final float AOE_DAMAGE_MULTIPLIER = 0.9F; // 普通范围目标吃到的 AOE 伤害倍率。
    private static final int MAX_AOE_TARGETS = 48; // 防止怪堆里一箭触发过多伤害结算。
    private static final int TRAIL_PRIME_RESEND_INTERVAL_TICKS = 4; // 高速箭补发拉丝包的最小间隔。
    private static final int AOE_TARGETS_PER_BATCH = 12; // 单次 AOE 最多处理多少个目标，剩下的分批做。
    private static final int AOE_TARGETS_PER_LEVEL_TICK = 24; // 一个世界每 tick 最多补多少个排队的 AOE 目标。

    // ===== 运行时状态 =====
    private static final Set<UUID> MODIFIED_ARROWS = new HashSet<>(); // 记录已经完成“加速/伤害回调”的箭。
    private static final Set<UUID> TRAIL_SENT_ARROWS = new HashSet<>(); // 记录已经注册过客户端拉丝跟踪的箭。
    private static final Map<UUID, Double> ORIGINAL_BASE_DAMAGES = new HashMap<>(); // 记录加速前的箭伤，用于命中时按原速重新结算。
    private static final Map<UUID, Vec3> LOCKED_DIRECTIONS = new HashMap<>(); // 记录箭发射瞬间锁定的飞行方向，后续 tick 不再跟准心转向。
    private static final Map<UUID, Float> SPEED_MULTIPLIERS = new HashMap<>(); // 记录每支箭实际套用的速度倍率，给原版直击结算还原用。
    private static final Set<UUID> IMPACTED_ARROWS = new HashSet<>(); // 记录已经触发过白光/AOE 的箭，避免穿透或多目标路径重复爆炸。
    private static final Map<ServerLevel, Deque<PendingAreaDamage>> PENDING_AREA_DAMAGE = new HashMap<>(); // 超大范围 AOE 拆到后续 tick 继续结算。

    // ===== 视觉预设入口 =====
    private static final VoidTrailInstance.Preset RAY = makeRay(); // 箭飞行时用的拉丝预设。
    private static final VoidRingInstance.Preset LIGHT = makeBaseLight(); // 箭命中时用的基础白光预设。
    private static final VoidBlackHoleInstance.Config BLACK_HOLE_CONFIG = VoidBlackHoleInstance.Config.DEFAULT; // 箭命中时播放的黑洞球体配置。

    @SubscribeEvent // 实体进世界时，尽快尝试给箭挂上虚空射手效果。
    public static void onArrowJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            runArrow(arrow);
        }
    }

    @SubscribeEvent // 每 tick 再补一次，避免刚进世界那一刻还没拿到附魔信息。
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            runArrow(arrow);
        }
    }

    @SubscribeEvent // 箭离开世界时，把对应的运行时记录一起清掉。
    public static void onArrowLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            UUID arrowId = arrow.getUUID();
            MODIFIED_ARROWS.remove(arrowId);
            TRAIL_SENT_ARROWS.remove(arrowId);
            ORIGINAL_BASE_DAMAGES.remove(arrowId);
            LOCKED_DIRECTIONS.remove(arrowId);
            SPEED_MULTIPLIERS.remove(arrowId);
            IMPACTED_ARROWS.remove(arrowId);
        }
    }

    @SubscribeEvent // 世界 tick 后处理一小批挂起的 AOE，避免命中瞬间把所有目标都压在同一 tick。
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            tickAreaDamage(serverLevel);
        }
    }

    @SubscribeEvent // 世界卸载时把这个世界残留的 AOE 队列清掉。
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PENDING_AREA_DAMAGE.remove(serverLevel);
        }
    }

    @SubscribeEvent // 箭命中时，统一处理白光、AOE 和清箭。
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        int level = getArcherLevel(projectile);
        if (level <= 0) {
            return;
        }

        if (projectile instanceof AbstractArrow arrow && projectile.level() instanceof ServerLevel serverLevel) {
            if (isSelfHit(arrow, event.getRayTraceResult())) {
                event.setCanceled(true);
                return;
            }

            if (!IMPACTED_ARROWS.add(arrow.getUUID())) {
                return;
            }

            if (hitArrow(serverLevel, arrow, event.getRayTraceResult(), level)) {
                event.setCanceled(true);
            }
        }
    }

    public static boolean isVoidArcherProjectile(Projectile projectile) { // 给别的类一个清晰入口，判断某支箭是否带虚空射手逻辑。
        return getArcherLevel(projectile) > 0;
    }

    private static void runArrow(AbstractArrow arrow) { // 飞行阶段总入口。
        int level = getArcherLevel(arrow);
        if (level <= 0) {
            return;
        }

        hideArrow(arrow); // 视觉状态每 tick 都刷新一次，避免同步抖动。

        if (arrow.level().isClientSide()) { // 真正改伤害和发网络包只放在服务端。
            return;
        }

        if (arrow.tickCount > MAX_ARROW_LIFETIME_TICKS) { // 高速无重力箭不让它在世界里一直飞下去。
            arrow.discard();
            return;
        }

        boostArrowOnce(arrow, level); // 只在第一次真正强化箭的速度和伤害参数。
        lockArrowDir(arrow); // 后续飞行只沿发射时锁住的方向走，不再跟准心变化。
        sendTrail(arrow); // 拉丝按需要补发。
    }

    private static boolean hitArrow(ServerLevel serverLevel, AbstractArrow arrow, HitResult hitResult, int level) { // 命中阶段总入口。
        Vec3 impactPosition = hitResult.getLocation();
        float impactScale = getHitScale(arrow);
        VoidRingInstance.Preset lightPreset = makeLight(level);
        VoidBlackHoleInstance.Config blackHoleConfig = makeBlackHole(level);

        sendHitTrail(arrow); // 命中这一帧再补一次带种子段的拉丝，专门兜住“高速箭刚生成就命中”的情况。
        addAreaDamage(serverLevel, arrow, hitResult, impactPosition, impactScale, lightPreset, level); // 先结算 AOE 伤害。
        // ModNetworking.sendPhaseTearAt(serverLevel, impactPosition, impactScale, lightPreset); // 原虚空射手命中白光效果。
        ModNetworking.sendBlackHoleAt(serverLevel, impactPosition, impactScale, blackHoleConfig); // 再在落点播放新黑洞实例。

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return false; // 实体直击交回原版处理，这样直击伤害、火矢、击退、清箭都只跑一次。
        }

        if (!arrow.isRemoved()) { // 方块命中没有直击结算，这里主动清箭并取消原版后续。
            arrow.discard();
        }
        return true;
    }

    private static void hideArrow(AbstractArrow arrow) { // 每 tick 都刷一遍箭的视觉状态。
        arrow.setInvisible(true); // 箭本体隐藏。
        arrow.setNoGravity(true); // 箭无下坠。
        arrow.setCritArrow(false); // 关闭原版满弓暴击粒子尾迹。
    }

    private static void boostArrowOnce(AbstractArrow arrow, int level) { // 只在第一次进入服务端逻辑时修改箭的飞行参数。
        UUID arrowId = arrow.getUUID();
        if (MODIFIED_ARROWS.contains(arrowId)) {
            return;
        }

        float speedMultiplier = getSpeedMult(level);
        double baseDamage = ((AbstractArrowAccessor) arrow).voidcraft$getBaseDamage();
        Vec3 currentMotion = arrow.getDeltaMovement();
        Vec3 ownerMotion = arrow.getOwner() == null ? Vec3.ZERO : arrow.getOwner().getDeltaMovement();

        ORIGINAL_BASE_DAMAGES.put(arrowId, baseDamage);
        SPEED_MULTIPLIERS.put(arrowId, speedMultiplier);

        Vec3 flightMotion = currentMotion.subtract(ownerMotion); // 先剥掉射手自己的移动速度，避免一加速就把横移一起放大。
        if (flightMotion.lengthSqr() < 1.0E-8D) {
            flightMotion = currentMotion;
        }

        double flightSpeed = flightMotion.length();
        Vec3 aimDirection = getAimDir(arrow.getOwner(), flightMotion);
        Vec3 newMotion = aimDirection.scale(flightSpeed * speedMultiplier);

        LOCKED_DIRECTIONS.put(arrowId, aimDirection);
        arrow.setDeltaMovement(newMotion); // 速度改成对准准星后的高速直线。
        arrow.setBaseDamage(baseDamage / speedMultiplier); // 基础伤害除回来，避免速度提升时伤害一起爆炸。
        MODIFIED_ARROWS.add(arrowId);
    }

    private static void lockArrowDir(AbstractArrow arrow) { // 保持箭沿首次锁定的方向飞行，避免中途再被别的逻辑带偏。
        Vec3 lockedDirection = LOCKED_DIRECTIONS.get(arrow.getUUID());
        if (lockedDirection == null) {
            return;
        }

        Vec3 currentMotion = arrow.getDeltaMovement();
        double currentSpeed = currentMotion.length();
        if (currentSpeed < 1.0E-8D) {
            return;
        }

        Vec3 lockedMotion = lockedDirection.scale(currentSpeed);
        if (currentMotion.distanceToSqr(lockedMotion) > 1.0E-8D) {
            arrow.setDeltaMovement(lockedMotion);
        }
    }

    private static void sendTrail(AbstractArrow arrow) { // 根据当前状态决定是否需要再补一次拉丝包。
        UUID arrowId = arrow.getUUID();
        boolean firstSend = TRAIL_SENT_ARROWS.add(arrowId);
        boolean primeResend = arrow.tickCount <= TRAIL_PRIME_TICKS
                && arrow.tickCount % TRAIL_PRIME_RESEND_INTERVAL_TICKS == 0;
        if (firstSend || primeResend) {
            ModNetworking.sendEntityTrail(arrow, RAY, 1.0F, TRAIL_SEED_LENGTH);
        }
    }

    private static void sendHitTrail(AbstractArrow arrow) {
        ModNetworking.sendEntityTrail(arrow, RAY, 1.0F, TRAIL_SEED_LENGTH);
    }

    private static VoidTrailInstance.Preset makeRay() { // 以后要调箭尾拉丝，优先改这里。
        return VoidTrailInstance.Preset.builder()
                .lifetimeTicks(20)
                .centerYOffset(0.0F)
                .minMoveDistance(0.02F)
                .pointSpacing(0.06F)
                .maxInterpolationSteps(24)
                .width(0.1F)
                .height(0.1F)
                .ribbonFadeSegments(5)
                .glowHeightMultiplier(1.2F)
                .glowWidthMultiplier(1.2F)
                .build();
    }

    private static VoidRingInstance.Preset makeBaseLight() { // 以后要调命中白光，优先改这里。
        return VoidRingInstance.Preset.builder()
                .durationTicks(5)
                .peakHoldTicks(1)
                .centerYOffset(0.3F)
                .followCameraPitch(true)
                .startHalfHeight(0.5F)
                .peakHalfHeight(1.52F)
                .endHalfHeight(0.52F)
                .startHalfWidth(0.5F)
                .peakHalfWidth(1.52F)
                .endHalfWidth(0.5F)
                .coreAlpha(0.96F)
                .distortionAlpha(3.92F)
                .lineAlpha(0.85F)
                .distortionThickness(2.56F)
                .distortionAmplitude(10.78F)
                .distortionWidthScale(1.06F)
                .distortionHeightScale(1.04F)
                .noiseFrequency(8.6F)
                .noiseScrollSpeed(6.68F)
                .build();
    }
//private static VoidRingInstance.Preset makeBaseLight() { // 以后要调命中白光，优先改这里。
//    return VoidRingInstance.Preset.builder()
//            .durationTicks(80)
//            .followCameraPitch(true)
//
//            // 尺寸尽量接近圆
//            .startHalfHeight(0.2F)
//            .startHalfWidth(0.2F)
//            .peakHalfHeight(3.6F)
//            .peakHalfWidth(3.6F)
//            .endHalfHeight(3.6F)
//            .endHalfWidth(3.6F)
//
//            // 光不要填太实，否则中心不像黑洞
//            .color(0x000000)
//            .coreAlpha(1.00F)
//            .glowAlpha(0.95F)
//            .lineAlpha(1.0F)
//            .filledFadeStart(0.3F)
//
//            // 黑洞透镜/吸入
//            .distortionAlpha(3.9F)
//            .distortionAmplitude(15.0F)
//            .distortionThickness(5.2F)
//            .distortionWidthScale(1.2F)
//            .distortionHeightScale(1.2F)
//            .noiseFrequency(1.4F)
//            .noiseScrollSpeed(0.01F)
//            .swirlStrength(0.2F)
//            .suctionStrength(3.0F)
//            .occludedByBlocks(true)
//            .build();
//}

    private static VoidRingInstance.Preset makeLight(int level) { // 按附魔等级放大白光尺寸。
        if (level <= 1) {
            return LIGHT;
        }

        float sizeMultiplier = (float) Math.pow(LIGHT_SIZE_MULTIPLIER_PER_LEVEL, level - 1);
        return LIGHT.copy()
                .startHalfHeight(LIGHT.startHalfHeight() * sizeMultiplier)
                .peakHalfHeight(LIGHT.peakHalfHeight() * sizeMultiplier)
                .endHalfHeight(LIGHT.endHalfHeight() * sizeMultiplier)
                .startHalfWidth(LIGHT.startHalfWidth() * sizeMultiplier)
                .peakHalfWidth(LIGHT.peakHalfWidth() * sizeMultiplier)
                .endHalfWidth(LIGHT.endHalfWidth() * sizeMultiplier)
                .build();
    }

    private static VoidBlackHoleInstance.Config makeBlackHole(int level) { // 按附魔等级放大黑洞视觉尺寸。
        if (level <= 1) {
            return BLACK_HOLE_CONFIG;
        }

        float sizeMultiplier = (float) Math.pow(LIGHT_SIZE_MULTIPLIER_PER_LEVEL, level - 1);
        return BLACK_HOLE_CONFIG.copy()
                .startHalfHeight(BLACK_HOLE_CONFIG.startHalfHeight() * sizeMultiplier)
                .peakHalfHeight(BLACK_HOLE_CONFIG.peakHalfHeight() * sizeMultiplier)
                .endHalfHeight(BLACK_HOLE_CONFIG.endHalfHeight() * sizeMultiplier)
                .startHalfWidth(BLACK_HOLE_CONFIG.startHalfWidth() * sizeMultiplier)
                .peakHalfWidth(BLACK_HOLE_CONFIG.peakHalfWidth() * sizeMultiplier)
                .endHalfWidth(BLACK_HOLE_CONFIG.endHalfWidth() * sizeMultiplier)
                .build();
    }

    private static void addAreaDamage( // 命中后，在白光附近打一段 AOE。
            ServerLevel serverLevel,
            AbstractArrow arrow,
            HitResult hitResult,
            Vec3 impactPosition,
            float impactScale,
            VoidRingInstance.Preset lightPreset,
            int level
    ) {
        ImpactAreaProfile areaProfile = makeHitArea(impactPosition, impactScale, lightPreset);
        Entity directHitEntity = getHitEntity(hitResult);
        Set<UUID> excludedEntityIds = getNoHitIds(directHitEntity, arrow.getOwner());
        List<PendingAreaTarget> targets = getHitTargets(serverLevel, areaProfile, excludedEntityIds);
        if (targets.isEmpty()) {
            return;
        }

        ArrowDamageContext damageContext = makeArrowDamage(serverLevel, arrow, level);
        if (damageContext == null) {
            return;
        }

        PendingAreaDamage pendingAreaDamage = new PendingAreaDamage(damageContext, targets);
        hitAreaBatch(pendingAreaDamage, AOE_TARGETS_PER_BATCH);
        if (pendingAreaDamage.hasRemainingTargets()) {
            PENDING_AREA_DAMAGE.computeIfAbsent(serverLevel, ignored -> new ArrayDeque<>()).addLast(pendingAreaDamage);
        }
    }

    private static ImpactAreaProfile makeHitArea(Vec3 impactPosition, float impactScale, VoidRingInstance.Preset lightPreset) { // 把白光的伤害体积预先算好，后面粗筛细筛都复用。
        Vec3 damageCenter = impactPosition.add(0.0D, lightPreset.centerYOffset() * impactScale, 0.0D);
        double horizontalRange = Math.max(0.1D, lightPreset.peakHalfWidth() * impactScale);
        double verticalRange = Math.max(0.1D, lightPreset.peakHalfHeight() * impactScale);
        AABB damageBox = new AABB(
                damageCenter.x - horizontalRange,
                damageCenter.y - verticalRange,
                damageCenter.z - horizontalRange,
                damageCenter.x + horizontalRange,
                damageCenter.y + verticalRange,
                damageCenter.z + horizontalRange
        );
        return new ImpactAreaProfile(damageCenter, horizontalRange, verticalRange, damageBox);
    }

    private static Entity getHitEntity(HitResult hitResult) { // 如果箭这次是直击实体，就把那个实体取出来。
        if (hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity();
        }
        return null;
    }

    private static boolean isSelfHit(AbstractArrow arrow, HitResult hitResult) {
        Entity owner = arrow.getOwner();
        Entity directHitEntity = getHitEntity(hitResult);
        return owner != null && directHitEntity != null && directHitEntity.is(owner);
    }

    private static Set<UUID> getNoHitIds(Entity... entities) {
        Set<UUID> excludedEntityIds = new HashSet<>();
        for (Entity entity : entities) {
            if (entity != null) {
                excludedEntityIds.add(entity.getUUID());
            }
        }
        return excludedEntityIds;
    }

    private static List<PendingAreaTarget> getHitTargets(
            ServerLevel serverLevel,
            ImpactAreaProfile areaProfile,
            Set<UUID> excludedEntityIds
    ) {
        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, areaProfile.damageBox(), LivingEntity::isAlive);
        List<PendingAreaTarget> targets = new ArrayList<>(candidates.size());
        for (LivingEntity target : candidates) {
            // 直击目标会继续交给原版箭伤害处理，射手本人也不吃自己的 AOE。
            if (excludedEntityIds.contains(target.getUUID())) {
                continue;
            }

            if (!inHitArea(target, areaProfile)) {
                continue;
            }

            targets.add(new PendingAreaTarget(target, AOE_DAMAGE_MULTIPLIER, getHitDistSqr(target, areaProfile.center())));
        }

        if (targets.isEmpty()) {
            return targets;
        }

        targets.sort(Comparator.comparingDouble(PendingAreaTarget::distanceSqr));
        if (targets.size() > MAX_AOE_TARGETS) {
            return new ArrayList<>(targets.subList(0, MAX_AOE_TARGETS));
        }
        return targets;
    }

    private static boolean inHitArea(LivingEntity target, ImpactAreaProfile areaProfile) { // 大 AABB 先粗筛，再用椭球细筛把边角上不该命中的目标排掉。
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        double horizontalRadius = areaProfile.horizontalRange() + target.getBbWidth() * 0.5D;
        double verticalRadius = areaProfile.verticalRange() + target.getBbHeight() * 0.5D;
        double dx = targetCenter.x - areaProfile.center().x;
        double dy = targetCenter.y - areaProfile.center().y;
        double dz = targetCenter.z - areaProfile.center().z;
        double horizontalFactor = (dx * dx + dz * dz) / (horizontalRadius * horizontalRadius);
        double verticalFactor = (dy * dy) / (verticalRadius * verticalRadius);
        return horizontalFactor + verticalFactor <= 1.0D;
    }

    private static double getHitDistSqr(LivingEntity target, Vec3 damageCenter) {
        return target.getBoundingBox().getCenter().distanceToSqr(damageCenter);
    }

    private static ArrowDamageContext makeArrowDamage(ServerLevel serverLevel, AbstractArrow arrow, int level) {
        float speedMultiplier = getSpeedMult(level);
        double normalizedImpactSpeed = getHitSpeed(arrow, speedMultiplier);
        float baseDamage = (float) getBaseDamage(arrow, speedMultiplier);
        if (normalizedImpactSpeed <= 0.0D || baseDamage <= 0.0F) {
            return null;
        }

        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null) {
            weapon = ItemStack.EMPTY;
        }

        return new ArrowDamageContext(
                serverLevel,
                arrow,
                makeArrowDamageSource(arrow),
                weapon.copy(),
                normalizedImpactSpeed,
                baseDamage,
                needFixVanillaDamage(arrow),
                getVanillaMove(arrow, arrow.getDeltaMovement())
        );
    }

    private static void tickAreaDamage(ServerLevel serverLevel) {
        Deque<PendingAreaDamage> queuedAreaDamage = PENDING_AREA_DAMAGE.get(serverLevel);
        if (queuedAreaDamage == null || queuedAreaDamage.isEmpty()) {
            PENDING_AREA_DAMAGE.remove(serverLevel);
            return;
        }

        int remainingBudget = AOE_TARGETS_PER_LEVEL_TICK;
        int remainingPasses = queuedAreaDamage.size();
        while (remainingBudget > 0 && remainingPasses > 0 && !queuedAreaDamage.isEmpty()) {
            PendingAreaDamage pendingAreaDamage = queuedAreaDamage.pollFirst();
            int processedTargets = hitAreaBatch(
                    pendingAreaDamage,
                    Math.min(AOE_TARGETS_PER_BATCH, remainingBudget)
            );
            remainingBudget -= processedTargets;
            remainingPasses--;

            if (pendingAreaDamage.hasRemainingTargets()) {
                queuedAreaDamage.addLast(pendingAreaDamage);
            }
        }

        if (queuedAreaDamage.isEmpty()) {
            PENDING_AREA_DAMAGE.remove(serverLevel);
        }
    }

    private static int hitAreaBatch(PendingAreaDamage pendingAreaDamage, int maxTargets) {
        int processedTargets = 0;
        while (processedTargets < maxTargets && pendingAreaDamage.hasRemainingTargets()) {
            PendingAreaTarget targetEntry = pendingAreaDamage.nextTarget();
            processedTargets++;

            LivingEntity target = targetEntry.target();
            if (!target.isAlive() || target.isRemoved() || target.level() != pendingAreaDamage.damageContext().serverLevel()) {
                continue;
            }

            float directDamage = pendingAreaDamage.damageContext().getDamage(target);
            float areaDamage = directDamage * targetEntry.damageMultiplier();
            if (areaDamage > 0.0F) {
                target.hurtServer(pendingAreaDamage.damageContext().serverLevel(), pendingAreaDamage.damageContext().damageSource(), areaDamage);
            }
        }
        return processedTargets;
    }

    private static double getBaseDamage(AbstractArrow arrow, float speedMultiplier) {
        Double originalBaseDamage = ORIGINAL_BASE_DAMAGES.get(arrow.getUUID());
        if (originalBaseDamage != null) {
            return originalBaseDamage;
        }

        double currentBaseDamage = ((AbstractArrowAccessor) arrow).voidcraft$getBaseDamage();
        return MODIFIED_ARROWS.contains(arrow.getUUID()) ? currentBaseDamage * speedMultiplier : currentBaseDamage;
    }

    public static boolean needFixVanillaDamage(AbstractArrow arrow) {
        return MODIFIED_ARROWS.contains(arrow.getUUID());
    }

    public static double getVanillaBaseDamage(AbstractArrow arrow, double fallbackBaseDamage) {
        if (!needFixVanillaDamage(arrow)) {
            return fallbackBaseDamage;
        }

        return getBaseDamage(arrow, getSpeedMult(arrow));
    }

    public static float getVanillaHitSpeed(AbstractArrow arrow, float fallbackSpeed) {
        if (!needFixVanillaDamage(arrow)) {
            return fallbackSpeed;
        }

        return (float) getHitSpeed(arrow, getSpeedMult(arrow));
    }

    public static Vec3 getVanillaMove(AbstractArrow arrow, Vec3 fallbackMovement) {
        if (!needFixVanillaDamage(arrow)) {
            return fallbackMovement;
        }

        float speedMultiplier = getSpeedMult(arrow);
        if (speedMultiplier <= 0.0F) {
            return fallbackMovement;
        }

        return fallbackMovement.scale(1.0D / speedMultiplier);
    }

    private static double getHitSpeed(AbstractArrow arrow, float speedMultiplier) {
        double speed = arrow.getDeltaMovement().length();
        if (speedMultiplier <= 0.0F) {
            return speed;
        }
        return speed / speedMultiplier;
    }

    private static float getSpeedMult(AbstractArrow arrow) {
        Float speedMultiplier = SPEED_MULTIPLIERS.get(arrow.getUUID());
        if (speedMultiplier != null) {
            return speedMultiplier;
        }

        return getSpeedMult(getArcherLevel(arrow));
    }

    public static DamageSource makeArrowDamageSource(AbstractArrow arrow) {
        Entity owner = arrow.getOwner();
        return arrow.damageSources().source(getArrowDamageType(arrow), arrow, owner == null ? arrow : owner);
    }

    private static ResourceKey<DamageType> getArrowDamageType(AbstractArrow arrow) {
        return arrow.getRandom().nextBoolean() ? ModDamageTypes.VOID_ARCHER_PHASE : ModDamageTypes.VOID_ARCHER_ENTER_VOID;
    }

    private static float getHitScale(AbstractArrow arrow) { // 命中特效和范围伤害统一复用这一套缩放计算。
        return arrow.getBbHeight() / 1.8F;
    }

    private static float getSpeedMult(int level) { // 速度倍率统一从这里出，后面好改。
        return SPEED_MULTIPLIER_PER_LEVEL * level;
    }

    private static Vec3 getAimDir(Entity owner, Vec3 fallbackMotion) { // 只取发射瞬间视线方向，后续不再参考准星落点。
        if (owner == null) {
            return fixDir(fallbackMotion);
        }

        Vec3 lookDirection = owner.getViewVector(1.0F);
        if (lookDirection.lengthSqr() < 1.0E-8D) {
            lookDirection = fallbackMotion;
        }

        return fixDir(lookDirection);
    }

    private static Vec3 fixDir(Vec3 direction) { // 安全归一化，避免零向量把速度算坏。
        if (direction == null || direction.lengthSqr() < 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }

    private static int getArcherLevel(Projectile projectile) { // 统一从箭和射手身上读取虚空射手附魔等级。
        HolderLookup<Enchantment> enchantments = projectile.level().holderLookup(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> voidArcher = enchantments.get(
                com.example.voidcraft.ModEnchantment.VoidArcher.VOID_ARCHER
        );
        if (voidArcher.isEmpty()) {
            return 0;
        }

        Holder.Reference<Enchantment> enchantment = voidArcher.get();
        int weaponLevel = getEnchantLevel(projectile.getWeaponItem(), enchantment);
        if (weaponLevel > 0) {
            return weaponLevel;
        }

        Entity owner = projectile.getOwner();
        if (owner instanceof LivingEntity living) {
            int mainHandLevel = getEnchantLevel(living.getMainHandItem(), enchantment);
            if (mainHandLevel > 0) {
                return mainHandLevel;
            }

            int offHandLevel = getEnchantLevel(living.getOffhandItem(), enchantment);
            if (offHandLevel > 0) {
                return offHandLevel;
            }

            return getEnchantLevel(living.getUseItem(), enchantment);
        }

        return 0;
    }

    private static int getEnchantLevel(ItemStack stack, Holder.Reference<Enchantment> enchantment) { // 安全读取物品上的附魔等级。
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return stack.getEnchantmentLevel(enchantment);
    }

    private record ImpactAreaProfile(Vec3 center, double horizontalRange, double verticalRange, AABB damageBox) {
    }

    private record PendingAreaTarget(LivingEntity target, float damageMultiplier, double distanceSqr) {
    }

    private record ArrowDamageContext(
            ServerLevel serverLevel,
            AbstractArrow arrow,
            DamageSource damageSource,
            ItemStack weapon,
            double normalizedImpactSpeed,
            float baseDamage,
            boolean normalizeVanillaHitDamage,
            Vec3 vanillaEquivalentMovement
    ) {
        private float getDamage(LivingEntity target) {
            double enchantedBaseDamage = this.baseDamage;
            if (!this.weapon.isEmpty()) {
                if (!this.normalizeVanillaHitDamage) {
                    enchantedBaseDamage = EnchantmentHelper.modifyDamage(
                            this.serverLevel,
                            this.weapon,
                            target,
                            this.damageSource,
                            this.baseDamage
                    );
                } else {
                    Vec3 currentMovement = this.arrow.getDeltaMovement();
                    this.arrow.setDeltaMovement(this.vanillaEquivalentMovement);
                    try {
                        enchantedBaseDamage = EnchantmentHelper.modifyDamage(
                                this.serverLevel,
                                this.weapon,
                                target,
                                this.damageSource,
                                this.baseDamage
                        );
                    } finally {
                        this.arrow.setDeltaMovement(currentMovement);
                    }
                }
            }

            double rawDamage = this.normalizedImpactSpeed * enchantedBaseDamage;
            return (float) Mth.ceil(Mth.clamp(rawDamage, 0.0D, Integer.MAX_VALUE));
        }
    }

    private static final class PendingAreaDamage {
        private final ArrowDamageContext damageContext;
        private final List<PendingAreaTarget> targets;
        private int nextTargetIndex;

        private PendingAreaDamage(ArrowDamageContext damageContext, List<PendingAreaTarget> targets) {
            this.damageContext = damageContext;
            this.targets = targets;
        }

        private boolean hasRemainingTargets() {
            return this.nextTargetIndex < this.targets.size();
        }

        private PendingAreaTarget nextTarget() {
            return this.targets.get(this.nextTargetIndex++);
        }

        private ArrowDamageContext damageContext() {
            return this.damageContext;
        }
    }
}
