package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

public class HealthAssistPhaseTurretModule extends AssistPhaseTurretModule {
    private static final int BASE_MODULE_LEVEL = 1;
    private static final float DAMAGE_SCALE = 0.65F;
    private static final float SELF_HEAL_PER_LEVEL = 0.10F;
    private static final float FRIEND_HEAL = 0.20F;
    private static final float FRIEND_HEAL_PER_LEVEL = 0.15F;
    private static final long CHANNEL_ENERGY_ADD = 2L;
    private static final long BURST_ENERGY_ADD = 80L;
    private static final double FRIEND_RANGE = 4.0D;
    private static final double FRIEND_RANGE_SQR = FRIEND_RANGE * FRIEND_RANGE;

    private static final int LEFT_BEAM_CORE = 0xF2A4C9;
    private static final int LEFT_BEAM_GLOW = 0xD9327C;
    private static final int LEFT_BEAM_LEVEL_2_CORE = 0xF5AED4;
    private static final int LEFT_BEAM_LEVEL_2_GLOW = 0xE0448E;
    private static final int LEFT_BEAM_LEVEL_3_CORE = 0xFABAE0;
    private static final int LEFT_BEAM_LEVEL_3_GLOW = 0xEA55A0;
    private static final int LEFT_BEAM_LEVEL_4_CORE = 0xFFC6EC;
    private static final int LEFT_BEAM_LEVEL_4_GLOW = 0xF164B1;
    private static final int LEFT_BEAM_LEVEL_5_CORE = 0xFFD2F4;
    private static final int LEFT_BEAM_LEVEL_5_GLOW = 0xFA72C2;
    private static final int LEFT_BEAM_VOID_CORE = 0xF5C4FF;
    private static final int LEFT_BEAM_VOID_GLOW = 0xCB68E8;

    private static final int RIGHT_BEAM_CORE = 0xFFF7FC;
    private static final int RIGHT_BEAM_GLOW = 0xFFB8DC;
    private static final int RIGHT_BEAM_LEVEL_2_CORE = 0xFFF9FD;
    private static final int RIGHT_BEAM_LEVEL_2_GLOW = 0xFFC0E5;
    private static final int RIGHT_BEAM_LEVEL_3_CORE = 0xFFFBFE;
    private static final int RIGHT_BEAM_LEVEL_3_GLOW = 0xFFC8ED;
    private static final int RIGHT_BEAM_LEVEL_4_CORE = 0xFFFFFF;
    private static final int RIGHT_BEAM_LEVEL_4_GLOW = 0xFFD0F4;
    private static final int RIGHT_BEAM_LEVEL_5_CORE = 0xFFFFFF;
    private static final int RIGHT_BEAM_LEVEL_5_GLOW = 0xFFD8FA;
    private static final int RIGHT_BEAM_VOID_CORE = 0xFFFFFF;
    private static final int RIGHT_BEAM_VOID_GLOW = 0xE8C4FF;

    public HealthAssistPhaseTurretModule(Properties properties) {
        super(properties);
    }

    @Override
    protected LivingEntity pickTarget(ServerPlayer player, FireState state) {
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
            return healTarget(target, damage * getFriendHeal(moduleStack));
        }

        boolean hurt = super.hitTarget(player, moduleStack, stats, target, damage);
        if (hurt) {
            player.heal(damage * getSelfHeal(moduleStack));
        }
        return hurt;
    }

    @Override
    protected VoidBeamInstance.Config getBeam(ServerPlayer player, ItemStack moduleStack, Stats stats, LivingEntity target) {
        int level = getModuleLevel(moduleStack);
        boolean heal = isHealTarget(player, target);
        return PhaseTurretModule.makeShotBeam(
                getBeamCore(level, heal),
                getBeamGlow(level, heal)
        );
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
        return target.getHealth() < target.getMaxHealth();
    }

    private static boolean healTarget(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F || !needsHeal(target)) {
            return false;
        }

        float oldHealth = target.getHealth();
        target.heal(amount);
        return target.getHealth() > oldHealth;
    }

    private static float getSelfHeal(ItemStack moduleStack) {
        return getModuleLevel(moduleStack) * SELF_HEAL_PER_LEVEL;
    }

    private static float getFriendHeal(ItemStack moduleStack) {
        return FRIEND_HEAL + getModuleLevel(moduleStack) * FRIEND_HEAL_PER_LEVEL;
    }

    private static int getModuleLevel(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) {
            return BASE_MODULE_LEVEL;
        }

        return Math.max(BASE_MODULE_LEVEL, data.level());
    }

    private static int getBeamCore(int level, boolean heal) {
        if (heal) {
            return switch (level) {
                case 2 -> RIGHT_BEAM_LEVEL_2_CORE;
                case 3 -> RIGHT_BEAM_LEVEL_3_CORE;
                case 4 -> RIGHT_BEAM_LEVEL_4_CORE;
                case 5 -> RIGHT_BEAM_LEVEL_5_CORE;
                default -> level >= 6 ? RIGHT_BEAM_VOID_CORE : RIGHT_BEAM_CORE;
            };
        }

        return switch (level) {
            case 2 -> LEFT_BEAM_LEVEL_2_CORE;
            case 3 -> LEFT_BEAM_LEVEL_3_CORE;
            case 4 -> LEFT_BEAM_LEVEL_4_CORE;
            case 5 -> LEFT_BEAM_LEVEL_5_CORE;
            default -> level >= 6 ? LEFT_BEAM_VOID_CORE : LEFT_BEAM_CORE;
        };
    }

    private static int getBeamGlow(int level, boolean heal) {
        if (heal) {
            return switch (level) {
                case 2 -> RIGHT_BEAM_LEVEL_2_GLOW;
                case 3 -> RIGHT_BEAM_LEVEL_3_GLOW;
                case 4 -> RIGHT_BEAM_LEVEL_4_GLOW;
                case 5 -> RIGHT_BEAM_LEVEL_5_GLOW;
                default -> level >= 6 ? RIGHT_BEAM_VOID_GLOW : RIGHT_BEAM_GLOW;
            };
        }

        return switch (level) {
            case 2 -> LEFT_BEAM_LEVEL_2_GLOW;
            case 3 -> LEFT_BEAM_LEVEL_3_GLOW;
            case 4 -> LEFT_BEAM_LEVEL_4_GLOW;
            case 5 -> LEFT_BEAM_LEVEL_5_GLOW;
            default -> level >= 6 ? LEFT_BEAM_VOID_GLOW : LEFT_BEAM_GLOW;
        };
    }
}
