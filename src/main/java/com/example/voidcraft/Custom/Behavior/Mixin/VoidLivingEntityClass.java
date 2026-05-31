package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class VoidLivingEntityClass {
    private static final double PHASE_FLUID_UP_SPEED = 0.3D;
    private static final double PHASE_FLUID_DOWN_SPEED = 0.3D;
    private static final int PHASE_FLUID_BUFFER_TICKS = 4;
    private static final float PHASE_MOVE_FRICTION = 0.6F;
    private static final float PHASE_MOVE_SPEED_SCALE = 0.21600002F / (PHASE_MOVE_FRICTION * PHASE_MOVE_FRICTION * PHASE_MOVE_FRICTION);
    private static final double PHASE_MOVE_DRAG = PHASE_MOVE_FRICTION * 0.91D;
    private static final double PHASE_MOVE_TARGET_SCALE = PHASE_MOVE_SPEED_SCALE / (1.0D - PHASE_MOVE_DRAG);
    private static final double PHASE_AIR_Y_DRAG = 0.98D;
    private static final double PHASE_GROUND_CHECK = 0.08D;

    @Shadow
    protected boolean jumping;

    @Shadow
    protected abstract double getEffectiveGravity();

    @Unique
    private int voidcraft$phaseFluidTime;

    @Unique
    private boolean voidcraft$phaseAirMove;

    @Inject(method = "isPushable",at = @At("HEAD"),cancellable = true)
    public void noPushable(CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity)(Object)this;
        if(inPhase(entity)){   //相位实体不参与活体推挤
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPickable",at = @At("HEAD"),cancellable = true)
    public void noPickable(CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity)(Object)this;
        if(inPhase(entity)){   //相位实体不响应准星选取
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)
    private void setVoidSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (inPhase(entity)) {
            cir.setReturnValue(entity.getData(ModAttachments.VOID_SPEED.get()));
        }
    }

    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isAffectedByFluids()Z",
                    ordinal = 0
            )
    )
    private boolean noWaterTravelFluid(LivingEntity entity) {
        return canFluidTravel(entity);
    }

    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isAffectedByFluids()Z",
                    ordinal = 1
            )
    )
    private boolean noLavaTravelFluid(LivingEntity entity) {
        return canFluidTravel(entity);
    }

    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"
            )
    )
    private float getVoidFriction(BlockState state, LevelReader level, BlockPos pos, Entity target) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (inPhase(entity)) {
            return PHASE_MOVE_FRICTION;
        }
        return state.getFriction(level, pos, target);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void setPhaseMove(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!inPhaseOnly(entity)) {
            voidcraft$phaseFluidTime = 0;
            voidcraft$phaseAirMove = false;
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        boolean hasFluid = hasFluid(entity);
        boolean inFluidMove = false;
        if (hasFluid) {
            voidcraft$phaseFluidTime = PHASE_FLUID_BUFFER_TICKS;
            voidcraft$phaseAirMove = false;
            inFluidMove = true;
        } else if (voidcraft$phaseFluidTime > 0) {
            voidcraft$phaseFluidTime--;
            voidcraft$phaseAirMove = false;
            inFluidMove = true;
        } else {
            if (entity.onGround()) {
                voidcraft$phaseAirMove = false;
                return;
            }

            if (!voidcraft$phaseAirMove && canStartPhaseAirMove(entity, motion)) {
                voidcraft$phaseAirMove = true;
            }

            if (!voidcraft$phaseAirMove) {
                return;
            }
        }

        Vec3 fixedSpeed = getPhaseMoveSpeed(entity, travelVector, motion);
        double ySpeed = inFluidMove ? getPhaseFluidYSpeed(entity) : motion.y;

        entity.setDeltaMovement(fixedSpeed.x, ySpeed, fixedSpeed.z);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 movedSpeed = entity.getDeltaMovement();
        entity.setDeltaMovement(
                movedSpeed.x * PHASE_MOVE_DRAG,
                getNextYSpeed(entity, movedSpeed.y, inFluidMove),
                movedSpeed.z * PHASE_MOVE_DRAG
        );
        entity.resetFallDistance();
        if (entity instanceof Player player) {
            player.hurtMarked = true;
        }
        ci.cancel();
    }

    private static Vec3 getPhaseMoveSpeed(LivingEntity entity, Vec3 travelVector, Vec3 motion) {
        Vec3 input = getPhaseMoveInput(travelVector);
        if (input.lengthSqr() <= 1.0E-6D) {
            return new Vec3(motion.x, 0.0D, motion.z);
        }

        Vec3 oldMotion = entity.getDeltaMovement();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.moveRelative((float)(entity.getData(ModAttachments.VOID_SPEED.get()) * PHASE_MOVE_TARGET_SCALE), input);
        Vec3 fixedSpeed = entity.getDeltaMovement();
        entity.setDeltaMovement(oldMotion);
        return fixedSpeed;
    }

    private static Vec3 getPhaseMoveInput(Vec3 travelVector) {
        Vec3 input = new Vec3(travelVector.x, 0.0D, travelVector.z);
        if (input.lengthSqr() <= 1.0E-6D) {
            return Vec3.ZERO;
        }

        return input.normalize();
    }

    private double getNextYSpeed(LivingEntity entity, double ySpeed, boolean inFluidMove) {
        if (inFluidMove) {
            return ySpeed;
        }

        return (ySpeed - getPhaseGravity(entity)) * PHASE_AIR_Y_DRAG;
    }

    private double getPhaseFluidYSpeed(LivingEntity entity) {
        boolean goingDown = entity.isShiftKeyDown();
        if (jumping && !goingDown) {
            return PHASE_FLUID_UP_SPEED;
        }

        if (goingDown && !jumping) {
            return -PHASE_FLUID_DOWN_SPEED;
        }

        return 0.0D;
    }

    private double getPhaseGravity(LivingEntity entity) {
        if (entity.isNoGravity()) {
            return 0.0D;
        }

        return getEffectiveGravity();
    }

    private static boolean canStartPhaseAirMove(LivingEntity entity, Vec3 motion) {
        return motion.y > 0.0D || !hasBlockBelow(entity);
    }

    private static boolean hasBlockBelow(LivingEntity entity) {
        BlockPos pos = BlockPos.containing(
                entity.getX(),
                entity.getBoundingBox().minY - PHASE_GROUND_CHECK,
                entity.getZ()
        );
        BlockState state = entity.level().getBlockState(pos);
        return !state.getCollisionShape(entity.level(), pos).isEmpty();
    }

    private static boolean canFluidTravel(LivingEntity entity) {
        if (inPhase(entity)) {
            return false;
        }

        if (entity instanceof Player player) {
            return !player.getAbilities().flying;
        }

        return true;
    }

    private static boolean hasFluid(LivingEntity entity) {
        return !entity.getMaxHeightFluidType().isAir();
    }

    private static boolean inPhase(LivingEntity entity) {
        return entity.getData(ModAttachments.IN_PHASE.get())
                || entity.getData(ModAttachments.IN_VOID.get());
    }

    private static boolean inPhaseOnly(LivingEntity entity) {
        return entity.getData(ModAttachments.IN_PHASE.get())
                && !entity.getData(ModAttachments.IN_VOID.get());
    }


}
