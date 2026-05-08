package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.world.PhasePlayerStateHandler;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PhasePlayerTraversalMixin {
    @Inject(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void voidcraft$keepPhaseTraversalNoPhysics(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (PhasePlayerStateHandler.canPhaseThrough(player)) {
            // 原版每 tick 会按旁观者状态重置 noPhysics，这里把相位维度的穿墙状态补回去。
            player.noPhysics = true;
            player.setOnGround(false);
            player.resetFallDistance();
        }
    }
}
