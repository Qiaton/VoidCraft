package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.Effect.VoidTrailManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowRenderer.class)
public class VoidArrowRenderer {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;F)V",
            at = @At("TAIL")
    )
    private void markHiddenArrow(AbstractArrow arrow, ArrowRenderState state, float partialTick, CallbackInfo ci) {
        if (arrow.isInvisible() || VoidTrailManager.isTrackedEntity(arrow.getId())) {
            state.isInvisible = true;
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipHiddenArrow(
            ArrowRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (state.isInvisible) {
            ci.cancel();
        }
    }
}
