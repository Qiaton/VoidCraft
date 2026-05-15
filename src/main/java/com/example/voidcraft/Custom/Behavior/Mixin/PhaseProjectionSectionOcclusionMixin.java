package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.World.projection.PhaseProjectionClient;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionOcclusionGraph.class)
public class PhaseProjectionSectionOcclusionMixin {
    @Redirect(
            method = "runUpdates(Lnet/minecraft/client/renderer/SectionOcclusionGraph$GraphStorage;Lnet/minecraft/world/phys/Vec3;Ljava/util/Queue;ZLjava/util/function/Consumer;Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;contains(J)Z"
            )
    )
    private boolean voidcraft$isEmptySection(LongOpenHashSet sections, long sectionPos) {
        return sections.contains(sectionPos) && !PhaseProjectionClient.hasDrawSection(sectionPos);
    }
}
