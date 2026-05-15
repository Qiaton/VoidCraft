package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.World.projection.PhaseProjectionClient;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public class PhaseProjectionRenderSectionRegionMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void voidcraft$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> info) {
        info.setReturnValue(PhaseProjectionClient.getDrawState(pos, info.getReturnValue()));
    }

    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true)
    private void voidcraft$getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> info) {
        info.setReturnValue(PhaseProjectionClient.getDrawFluidState(pos, info.getReturnValue()));
    }
}
