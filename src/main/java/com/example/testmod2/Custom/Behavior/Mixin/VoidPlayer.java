package com.example.testmod2.Custom.Behavior.Mixin;

import com.example.testmod2.ModAttachments;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class VoidPlayer {
    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true) //虚空内不会被弹射物命中
    private void NO_HIT(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;
        if (player.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)       //还没完成实现
    private void NO_FLUID(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;
        if (player.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)              //虚空内陆地上固定移速
    private void SPEED(CallbackInfoReturnable<Float> cir) {
        Player player = (Player)(Object)this;

        if (player.getData(ModAttachments.IN_VOID.get())) {
            cir.setReturnValue(0.93F); // 虚空内移速
        }
    }
}
