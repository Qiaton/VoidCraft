package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class HealthPhaseTurretModule extends PhaseTurretModule {
    private static final int BASE_MODULE_LEVEL = 1;
    private static final float DAMAGE_SCALE = 0.65F;
    private static final float SELF_HEAL_PER_LEVEL = 0.10F;
    private static final float FRIEND_HEAL = 0.20F;
    private static final float FRIEND_HEAL_PER_LEVEL = 0.12F;

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

    public HealthPhaseTurretModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void shootRight(ServerPlayer player, ItemStack moduleStack, FireState state) {
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
                    true
            );

            sendHealShot(player, emitterIndex, result);
        }
    }

    @Override
    protected float getDamage(ItemStack moduleStack, LivingEntity target) {
        return super.getDamage(moduleStack, target) * DAMAGE_SCALE;
    }

    @Override
    protected float getDamage(ItemStack moduleStack, Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            return getDamage(moduleStack, livingTarget);
        }

        return super.getDamage(moduleStack, target) * DAMAGE_SCALE;
    }

    @Override
    protected boolean hitTarget(
            ServerPlayer player,
            ItemStack moduleStack,
            Entity hitEntity,
            Entity target,
            float amount,
            boolean right
    ) {
        if (right) {
            if (!(target instanceof LivingEntity livingTarget)) {
                return false;
            }

            boolean healed = healTarget(livingTarget, amount * getFriendHeal(moduleStack));
            if (healed) {
                playHeal(player, livingTarget);
            }
            return healed;
        }

        boolean hit = super.hitTarget(player, moduleStack, hitEntity, target, amount, false);
        if (hit) {
            player.heal(amount * getSelfHeal(moduleStack));
        }
        return hit;
    }

    @Override
    protected VoidBeamInstance.Config getBeam(
            ItemStack moduleStack,
            PhaseEmitterSlot emitterSlot,
            Vec3 targetPos,
            int targetEntityId,
            boolean right
    ) {
        int level = getModuleLevel(moduleStack);
        return makeShotBeam(
                getBeamCore(level, right),
                getBeamGlow(level, right)
        );
    }

    @Override
    public boolean isHealthVisual() {
        return true;
    }

    private static void sendHealShot(ServerPlayer player, int emitterIndex, ShotResult result) {
        if (result == null) {
            return;
        }

        ModSound.playHealthPhaseTurretShot(player.level(), player, emitterIndex);
        ModNetworking.sendTurretShotFx(player, emitterIndex, result.targetPos(), result.beamConfig());
    }

    private static boolean healTarget(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F) {
            return false;
        }

        float oldHealth = target.getHealth();
        target.heal(amount);
        return target.getHealth() > oldHealth;
    }

    private static void playHeal(ServerPlayer player, LivingEntity target) {
        ModSound.playHeal(player, target);
        if (target instanceof ServerPlayer targetPlayer && targetPlayer != player) {
            ModSound.playHeal(targetPlayer, target);
        }
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

    private static int getBeamCore(int level, boolean right) {
        if (right) {
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

    private static int getBeamGlow(int level, boolean right) {
        if (right) {
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
