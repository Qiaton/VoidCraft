package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class VoidEntity {
    private static boolean inPhase(Entity entity) {
        return entity instanceof LivingEntity livingEntity
                && (livingEntity.getData(ModAttachments.IN_PHASE.get())
                || livingEntity.getData(ModAttachments.IN_VOID.get()));
    }

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void noCollide(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void noPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void noProjectileHit(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void noFluidPush(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void noWater(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInLava", at = @At("HEAD"), cancellable = true)
    private void noLava(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInFluidType()Z", at = @At("HEAD"), cancellable = true)
    private void noFluidType(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "updateFluidHeightAndDoFluidPushing(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid(Lnet/neoforged/neoforge/fluids/FluidType;)Z"
            )
    )
    private boolean noFluidMove(Entity entity, FluidType fluidType) {
        if (inPhase(entity)) {
            return false;
        }

        return entity.isPushedByFluid(fluidType);
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void noStuck(BlockState state, Vec3 speed, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "getBlockSpeedFactor", at = @At("HEAD"), cancellable = true)
    private void noBlockSpeed(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBlockJumpFactor", at = @At("HEAD"), cancellable = true)
    private void noBlockJump(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;

        if (inPhase(self)) {
            cir.setReturnValue(1.0F);
        }
    }
}
