package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Effect.VoidBeamInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

public class HealthAssistPhaseTurretModule extends AssistPhaseTurretModule {
    private static final float DAMAGE_SCALE = 0.65F;
    private static final float SELF_HEAL_PER_LEVEL = 0.10F;
    private static final float FRIEND_HEAL = 0.20F;
    private static final float FRIEND_HEAL_PER_LEVEL = 0.15F;
    private static final float HEAL_SCALE = 0.25F;
    private static final long CHANNEL_ENERGY_ADD = 2L;
    private static final long BURST_ENERGY_ADD = 80L;
    private static final double FRIEND_RANGE = 4.0D;
    private static final double FRIEND_RANGE_SQR = FRIEND_RANGE * FRIEND_RANGE;
    private static final float DYING_HEALTH_RATE = 0.20F;
    private static final int DYING_HEAL_TICKS = 10;

    public HealthAssistPhaseTurretModule(Properties properties) {
        super(properties);
    }

    @Override
    protected LivingEntity pickTarget(ServerPlayer player, FireState state) {
        LivingEntity dyingPlayer = getDyingPlayer(player, state);
        if (dyingPlayer != null) {
            return dyingPlayer;
        }

        LivingEntity lockedFriend = getLockedFriend(player, state);
        if (lockedFriend != null) {
            return lockedFriend;
        }

        LivingEntity safetyThreat = findCloseThreat(player);
        if (safetyThreat != null) {
            lockTarget(player, state, safetyThreat);
            return safetyThreat;
        }

        LivingEntity friend = findHurtFriend(player);
        if (friend != null) {
            lockTarget(player, state, friend);
            return friend;
        }

        LivingEntity target = super.pickTarget(player, state);
        if (target != null) {
            return target;
        }

        return needsHeal(player) ? player : null;
    }

    @Override
    protected boolean hitTarget(ServerPlayer player, ItemStack moduleStack, Stats stats, LivingEntity target, float damage) {
        if (isHealTarget(player, target)) {
            float healAmount = damage * getFriendHeal(moduleStack) * HEAL_SCALE;
            if (target == player) {
                healAmount *= HEAL_SCALE;
            }
            return healTarget(target, healAmount);
        }

        boolean hurt = super.hitTarget(player, moduleStack, stats, target, damage);
        if (hurt) {
            player.heal(damage * getSelfHeal(moduleStack) * HEAL_SCALE);
        }
        return hurt;
    }

    @Override
    protected VoidBeamInstance.Config getBeam(ServerPlayer player, ItemStack moduleStack, Stats stats, LivingEntity target) {
        return HealthTurretHelper.getBeam(moduleStack, isHealTarget(player, target));
    }

    @Override
    protected float getDamageScale() {
        return DAMAGE_SCALE;
    }

    @Override
    protected long getChannelEnergyAdd() {
        return CHANNEL_ENERGY_ADD;
    }

    @Override
    protected long getBurstEnergyAdd() {
        return BURST_ENERGY_ADD;
    }

    @Override
    public boolean isHealthVisual() {
        return true;
    }

    private static LivingEntity getLockedFriend(ServerPlayer player, FireState state) {
        if (state.lockedTargetId == null || player.tickCount >= state.lockUntilTick) {
            return null;
        }

        Entity entity = getEntity(player, state.lockedTargetId);
        if (!(entity instanceof LivingEntity target)) {
            clearLock(state);
            return null;
        }

        if (!isFriend(player, target)) {
            return null;
        }

        if (!isGoodFriend(player, target, false)) {
            clearLock(state);
            return null;
        }

        return target;
    }

    private static LivingEntity findHurtFriend(ServerPlayer player) {
        LivingEntity best = null;
        double bestDistanceSqr = Double.MAX_VALUE;

        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(FRIEND_RANGE),
                target -> isGoodFriend(player, target, true)
        )) {
            double distanceSqr = player.distanceToSqr(target);
            if (distanceSqr < bestDistanceSqr) {
                best = target;
                bestDistanceSqr = distanceSqr;
            }
        }

        return best;
    }

    private static boolean isGoodFriend(ServerPlayer player, LivingEntity target, boolean checkSafeRange) {
        if (!isGoodTarget(player, target, true) || !isFriend(player, target) || !needsHeal(target)) {
            return false;
        }

        return !checkSafeRange || player.distanceToSqr(target) <= FRIEND_RANGE_SQR;
    }

    private static boolean isFriend(ServerPlayer player, LivingEntity target) {
        if (target == null || target == player) {
            return false;
        }

        if (player.isAlliedTo(target)) {
            return true;
        }

        return target instanceof TamableAnimal animal && animal.isOwnedBy(player);
    }

    private static boolean isHealTarget(ServerPlayer player, LivingEntity target) {
        return target == player || isFriend(player, target);
    }

    private static boolean needsHeal(LivingEntity target) {
        return HealthTurretHelper.needsHeal(target);
    }

    private static LivingEntity getDyingPlayer(ServerPlayer player, FireState state) {
        if (!isDying(player)) {
            clearDyingHeal(state);
            return null;
        }

        if (player.tickCount >= state.dyingHealUntilTick) {
            state.dyingHealUntilTick = player.tickCount + DYING_HEAL_TICKS;
        }
        return player;
    }

    private static void clearDyingHeal(FireState state) {
        state.dyingHealUntilTick = 0;
    }

    private static boolean isDying(ServerPlayer player) {
        return needsHeal(player) && player.getHealth() < player.getMaxHealth() * DYING_HEALTH_RATE;
    }

    private static boolean healTarget(LivingEntity target, float amount) {
        return HealthTurretHelper.heal(target, amount);
    }

    private static float getSelfHeal(ItemStack moduleStack) {
        return HealthTurretHelper.getSelfHeal(moduleStack, SELF_HEAL_PER_LEVEL);
    }

    private static float getFriendHeal(ItemStack moduleStack) {
        return HealthTurretHelper.getFriendHeal(moduleStack, FRIEND_HEAL, FRIEND_HEAL_PER_LEVEL);
    }
}
