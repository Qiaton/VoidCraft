package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Effect.VoidBeamInstance;
import com.example.voidcraft.Network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class HealthPhaseTurretModule extends PhaseTurretModule {
    private static final float DAMAGE_SCALE = 0.65F;
    private static final float SELF_HEAL_PER_LEVEL = 0.07F;
    private static final float FRIEND_HEAL = 0.20F;
    private static final float FRIEND_HEAL_PER_LEVEL = 0.15F;

    public HealthPhaseTurretModule(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canTurnForm() {
        return true;
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

            boolean healed = HealthTurretHelper.heal(livingTarget, amount * getFriendHeal(moduleStack));
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
        return HealthTurretHelper.getBeam(moduleStack, right);
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

    private static void playHeal(ServerPlayer player, LivingEntity target) {
        ModSound.playHeal(player, target);
        if (target instanceof ServerPlayer targetPlayer && targetPlayer != player) {
            ModSound.playHeal(targetPlayer, target);
        }
    }

    private static float getSelfHeal(ItemStack moduleStack) {
        return HealthTurretHelper.getSelfHeal(moduleStack, SELF_HEAL_PER_LEVEL);
    }

    private static float getFriendHeal(ItemStack moduleStack) {
        return HealthTurretHelper.getFriendHeal(moduleStack, FRIEND_HEAL, FRIEND_HEAL_PER_LEVEL);
    }
}
