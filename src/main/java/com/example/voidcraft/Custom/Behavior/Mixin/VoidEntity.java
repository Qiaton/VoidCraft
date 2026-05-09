package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class VoidEntity {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void noCollide(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (!(self instanceof LivingEntity livingEntity)) {
            return;
        }

        if (livingEntity.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void noPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (!(self instanceof LivingEntity livingEntity)) {
            return;
        }

        if (livingEntity.getData(ModAttachments.IN_VOID.get())) {
            ci.cancel();
        }
    }

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void noProjectileHit(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (self instanceof LivingEntity livingEntity
                && livingEntity.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void noFluidPush(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (self instanceof LivingEntity livingEntity
                && livingEntity.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(false);
        }
    }
}
