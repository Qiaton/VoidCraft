package com.example.testmod2.Custom.Behavior.Mixin;

import com.example.testmod2.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class VoidEntity {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void VOID_COLLIDE(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (player.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void VOID_PUSH(Entity other, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (player.getData(ModAttachments.IN_VOID.get())) {
            ci.cancel();
        }
    }
}
