package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.ModAttachments;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class VoidPlayer {
    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true) //虚空内不会被弹射物命中
    private void noArrowHit(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;
        if (inPhase(player)) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void noFluid(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;
        if (inPhase(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void noSwim(CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (inPhase(player)) {
            player.setSwimming(false);
            ci.cancel();
        }
    }
    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)              //虚空内陆地上固定移速
    private void setVoidSpeed(CallbackInfoReturnable<Float> cir) {
        Player player = (Player)(Object)this;

        if (inPhase(player)) {
            cir.setReturnValue(player.getData(ModAttachments.VOID_SPEED)); // 虚空内移速
        }
    }

    private static boolean inPhase(Player player) {
        return player.getData(ModAttachments.IN_PHASE.get())
                || player.getData(ModAttachments.IN_VOID.get());
    }
}
