package com.example.voidcraft.Custom.Behavior;

import com.example.voidcraft.Custom.Behavior.Mixin.AbstractArrowAccessor;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Effect.VoidTrailInstance;
import com.example.voidcraft.ModDamageTypes;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber
public class VoidArcher {
    // 飞行调参：箭本体只当作碰撞载体，视觉上当成高速能量束。
    private static final float SPEED_MULTIPLIER_PER_LEVEL = 2.0F;
    private static final int MAX_ARROW_LIFETIME_TICKS = 80;
    private static final int HIT_TRAIL_FIX_TICKS = 1;

    // 命中调参：有直击伤害就按直击伤害扩散，没有直击伤害就用基础伤害乘附魔等级兜底。
    private static final float AOE_DAMAGE_MULTIPLIER = 0.9F;
    private static final double AOE_BASE_RADIUS = 2.5D;
    private static final double AOE_RADIUS_PER_LEVEL = 1.5D;
    private static final int MAX_AOE_TARGETS = 48;
    private static final int FALLBACK_WAIT_TICKS = 1;

    // 运行时状态只保留两类：箭的发射状态，以及等待直击伤害结算的命中。
    private static final Map<UUID, ArrowState> ARROWS = new HashMap<>();
    private static final Map<UUID, PendingHit> PENDING_HITS = new HashMap<>();

    // 视觉预设入口：以后调光束和命中裂隙，只改这两个方法。
    private static final VoidTrailInstance.Preset RAY = makeRay();
    private static final VoidRingInstance.Preset LIGHT = makeLight();

    @SubscribeEvent
    public static void onArrowJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            runArrow(arrow);
        }
    }

    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            runArrow(arrow);
        }
    }

    @SubscribeEvent
    public static void onArrowLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            ARROWS.remove(arrow.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            runFallbackHits(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            clearLevelHits(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onArrowShootSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getSource() != SoundSource.PLAYERS || event.getSound() == null) {
            return;
        }

        if (event.getSound().value() != SoundEvents.ARROW_SHOOT) {
            return;
        }

        if (hasVoidArcherPlayer(event.getLevel(), event.getPosition())) {
            ModSound.playVoidArcherShoot(event.getLevel(), event.getPosition());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int level = getArcherLevel(arrow);
        if (level <= 0) {
            return;
        }

        HitResult hitResult = event.getRayTraceResult();
        if (isSelfHit(arrow, hitResult)) {
            event.setCanceled(true);
            return;
        }

        ArrowState state = getArrowState(arrow, level);
        Vec3 hitPos = hitResult.getLocation();
        sendHitFx(serverLevel, arrow, hitPos, level);

        Entity directHit = getHitEntity(hitResult);
        if (directHit instanceof LivingEntity living) {
            waitDirectDamage(serverLevel, arrow, state, living, hitPos, level);
            return;
        }

        // 方块或非活体实体没有直击伤害可等，直接用兜底伤害并取消原版箭命中。
        hitArea(serverLevel, arrow, hitPos, level, getFallbackDamage(state, level), null);
        finishArrow(arrow);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        PendingHit hit = PENDING_HITS.remove(arrow.getUUID());
        if (hit == null) {
            return;
        }

        float directDamage = event.getNewDamage();
        float areaDamage = directDamage > 0.0F
                ? directDamage * AOE_DAMAGE_MULTIPLIER
                : hit.fallbackDamage();
        hitArea(hit.serverLevel(), arrow, hit.position(), hit.level(), areaDamage, hit.directTargetId());
        finishArrow(arrow);
    }

    private static void runArrow(AbstractArrow arrow) {
        int level = getArcherLevel(arrow);
        if (level <= 0) {
            return;
        }

        hideArrow(arrow);

        if (arrow.level().isClientSide()) {
            return;
        }

        if (arrow.tickCount > MAX_ARROW_LIFETIME_TICKS) {
            finishArrow(arrow);
            return;
        }

        ArrowState state = getArrowState(arrow, level);
        sendNormalTrail(arrow, state);
    }

    private static ArrowState getArrowState(AbstractArrow arrow, int level) {
        UUID arrowId = arrow.getUUID();
        ArrowState state = ARROWS.get(arrowId);
        if (state != null) {
            return state;
        }

        state = makeArrowState(arrow, level);
        ARROWS.put(arrowId, state);
        return state;
    }

    private static ArrowState makeArrowState(AbstractArrow arrow, int level) {
        Vec3 oldMove = getOldMove(arrow);
        Vec3 aimDir = getAimDir(arrow.getOwner(), oldMove);
        double oldSpeed = Math.max(0.05D, oldMove.length());
        Vec3 damageMove = aimDir.scale(oldSpeed);
        float speedMultiplier = getSpeedMult(level);
        double baseDamage = ((AbstractArrowAccessor) arrow).voidcraft$getBaseDamage();

        // 加速只改变飞行手感；基础伤害先除回来，避免高速导致原版直击伤害暴涨。
        arrow.setDeltaMovement(damageMove.scale(speedMultiplier));
        arrow.setBaseDamage(baseDamage / speedMultiplier);
        return new ArrowState(damageMove, baseDamage, false);
    }

    private static void sendNormalTrail(AbstractArrow arrow, ArrowState state) {
        // 快速箭只走命中补偿流光；普通流光等箭活过补偿窗口后再发。
        if (arrow.tickCount <= HIT_TRAIL_FIX_TICKS || state.normalTrailSent()) {
            return;
        }

        ModNetworking.sendEntityTrail(arrow, RAY, 1.0F, -1.0D);
        ARROWS.put(arrow.getUUID(), state.withNormalTrailSent());
    }

    private static void waitDirectDamage(
            ServerLevel serverLevel,
            AbstractArrow arrow,
            ArrowState state,
            LivingEntity directTarget,
            Vec3 hitPos,
            int level
    ) {
        // 直击结算前把箭还原成“未加速的伤害状态”，让原版和其他模组弓正常算伤害。
        arrow.setDeltaMovement(state.damageMove());
        arrow.setBaseDamage(state.baseDamage());
        PENDING_HITS.put(
                arrow.getUUID(),
                new PendingHit(
                        arrow,
                        serverLevel,
                        hitPos,
                        level,
                        getFallbackDamage(state, level),
                        directTarget.getUUID(),
                        serverLevel.getGameTime()
                )
        );
    }

    private static void runFallbackHits(ServerLevel serverLevel) {
        long now = serverLevel.getGameTime();
        Iterator<Map.Entry<UUID, PendingHit>> iterator = PENDING_HITS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingHit> entry = iterator.next();
            PendingHit hit = entry.getValue();
            if (hit.serverLevel() != serverLevel) {
                continue;
            }

            if (now <= hit.tick() + FALLBACK_WAIT_TICKS) {
                continue;
            }

            AbstractArrow arrow = hit.arrow();
            iterator.remove();
            hitArea(serverLevel, arrow, hit.position(), hit.level(), hit.fallbackDamage(), hit.directTargetId());
            ARROWS.remove(entry.getKey());
            if (!arrow.isRemoved()) {
                arrow.discard();
            }
        }
    }

    private static void sendHitFx(ServerLevel serverLevel, AbstractArrow arrow, Vec3 hitPos, int level) {
        // 只有极短飞行时才补整段光束，正常飞行交给实体拖尾自己显示。
        if (arrow.tickCount <= HIT_TRAIL_FIX_TICKS) {
            ModNetworking.sendTrailSegment(serverLevel, arrow.getId(), getStartPos(arrow), hitPos, 1.0F, RAY);
        }
        ModNetworking.sendPhaseTearAt(serverLevel, hitPos, getHitScale(level), LIGHT);
        ModSound.playVoidArcherHit(serverLevel, hitPos);
    }

    private static void hitArea(
            ServerLevel serverLevel,
            AbstractArrow arrow,
            Vec3 hitPos,
            int level,
            float damage,
            UUID directTargetId
    ) {
        if (damage <= 0.0F) {
            return;
        }

        Entity owner = arrow.getOwner();
        double radius = getHitRadius(level);
        AABB box = new AABB(
                hitPos.x - radius,
                hitPos.y - radius,
                hitPos.z - radius,
                hitPos.x + radius,
                hitPos.y + radius,
                hitPos.z + radius
        );

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        targets.stream()
                .filter(target -> canAreaHit(target, owner, directTargetId, hitPos, radius))
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(hitPos)))
                .limit(MAX_AOE_TARGETS)
                .forEach(target -> target.hurtServer(serverLevel, makeDamageSource(arrow), damage));
    }

    private static boolean canAreaHit(LivingEntity target, Entity owner, UUID directTargetId, Vec3 hitPos, double radius) {
        if (owner != null && target.is(owner)) {
            return false;
        }

        if (directTargetId != null && target.getUUID().equals(directTargetId)) {
            return false;
        }

        return target.getBoundingBox().getCenter().distanceToSqr(hitPos) <= radius * radius;
    }

    private static void hideArrow(AbstractArrow arrow) {
        arrow.setInvisible(true);
        arrow.setNoGravity(true);
        arrow.setCritArrow(false);
        arrow.setSilent(true);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    private static void finishArrow(AbstractArrow arrow) {
        clearArrow(arrow.getUUID());
        if (!arrow.isRemoved()) {
            arrow.discard();
        }
    }

    private static void clearArrow(UUID arrowId) {
        ARROWS.remove(arrowId);
        PENDING_HITS.remove(arrowId);
    }

    private static void clearLevelHits(ServerLevel serverLevel) {
        PENDING_HITS.entrySet().removeIf(entry -> entry.getValue().serverLevel() == serverLevel);
    }

    private static Entity getHitEntity(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity();
        }
        return null;
    }

    private static boolean isSelfHit(AbstractArrow arrow, HitResult hitResult) {
        Entity owner = arrow.getOwner();
        Entity directHit = getHitEntity(hitResult);
        return owner != null && directHit != null && directHit.is(owner);
    }

    private static boolean hasVoidArcherPlayer(net.minecraft.world.level.Level level, Vec3 position) {
        AABB box = new AABB(
                position.x - 1.0D,
                position.y - 1.0D,
                position.z - 1.0D,
                position.x + 1.0D,
                position.y + 1.0D,
                position.z + 1.0D
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        for (Player player : players) {
            if (getEnchantLevel(level, player.getMainHandItem()) > 0
                    || getEnchantLevel(level, player.getOffhandItem()) > 0
                    || getEnchantLevel(level, player.getUseItem()) > 0) {
                return true;
            }
        }
        return false;
    }

    private static float getFallbackDamage(ArrowState state, int level) {
        return (float) state.baseDamage() * level;
    }

    private static double getHitRadius(int level) {
        return Math.max(1.0D, AOE_BASE_RADIUS + AOE_RADIUS_PER_LEVEL * Math.max(0, level - 1));
    }

    private static float getHitScale(int level) {
        return (float) (getHitRadius(level) / LIGHT.peakHalfWidth());
    }

    private static float getSpeedMult(int level) {
        return SPEED_MULTIPLIER_PER_LEVEL * level;
    }

    private static Vec3 getStartPos(AbstractArrow arrow) {
        Entity owner = arrow.getOwner();
        if (owner instanceof LivingEntity living) {
            return living.getEyePosition().add(living.getLookAngle().scale(0.5D));
        }
        return arrow.position();
    }

    private static Vec3 getOldMove(AbstractArrow arrow) {
        Vec3 move = arrow.getDeltaMovement();
        Entity owner = arrow.getOwner();
        if (owner != null) {
            Vec3 ownMove = owner.getDeltaMovement();
            Vec3 flightMove = move.subtract(ownMove);
            if (flightMove.lengthSqr() > 1.0E-8D) {
                return flightMove;
            }
        }
        return move;
    }

    private static Vec3 getAimDir(Entity owner, Vec3 fallbackMove) {
        if (owner != null && owner.getLookAngle().lengthSqr() > 1.0E-8D) {
            return owner.getLookAngle().normalize();
        }

        if (fallbackMove.lengthSqr() > 1.0E-8D) {
            return fallbackMove.normalize();
        }

        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static DamageSource makeDamageSource(AbstractArrow arrow) {
        Entity owner = arrow.getOwner();
        return arrow.damageSources().source(getDamageType(arrow), arrow, owner == null ? arrow : owner);
    }

    private static ResourceKey<DamageType> getDamageType(AbstractArrow arrow) {
        return arrow.getRandom().nextBoolean() ? ModDamageTypes.VOID_ARCHER_PHASE : ModDamageTypes.VOID_ARCHER_ENTER_VOID;
    }

    private static int getArcherLevel(Projectile projectile) {
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

    private static int getEnchantLevel(net.minecraft.world.level.Level level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        HolderLookup<Enchantment> enchantments = level.holderLookup(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> voidArcher = enchantments.get(
                com.example.voidcraft.ModEnchantment.VoidArcher.VOID_ARCHER
        );
        if (voidArcher.isEmpty()) {
            return 0;
        }
        return stack.getEnchantmentLevel(voidArcher.get());
    }

    private static int getEnchantLevel(ItemStack stack, Holder.Reference<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return stack.getEnchantmentLevel(enchantment);
    }

    private static VoidTrailInstance.Preset makeRay() {
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

    private static VoidRingInstance.Preset makeLight() {
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

    private record ArrowState(Vec3 damageMove, double baseDamage, boolean normalTrailSent) {
        private ArrowState withNormalTrailSent() {
            return new ArrowState(this.damageMove, this.baseDamage, true);
        }
    }

    private record PendingHit(
            AbstractArrow arrow,
            ServerLevel serverLevel,
            Vec3 position,
            int level,
            float fallbackDamage,
            UUID directTargetId,
            long tick
    ) {
    }
}
