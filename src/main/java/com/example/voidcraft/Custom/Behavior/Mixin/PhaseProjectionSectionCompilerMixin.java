package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.World.projection.PhaseProjectionClient;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SectionCompiler.class)
public class PhaseProjectionSectionCompilerMixin {
    @Redirect(
            method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState voidcraft$getDrawState(
            RenderChunkRegion region,
            BlockPos pos,
            SectionPos sectionPos,
            RenderChunkRegion originalRegion,
            com.mojang.blaze3d.vertex.VertexSorting vertexSorting,
            SectionBufferBuilderPack sectionBufferBuilderPack,
            List<net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers
    ) {
        BlockState state = region.getBlockState(pos);
        // 只替换当前正在编译的方块；邻居查询保持相位世界原状态，避免原版遮挡判断吃掉投影表面。
        return PhaseProjectionClient.getDrawState(pos, state);
    }
}
