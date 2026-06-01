package com.example.voidcraft.Custom.Behavior.Mixin;

import com.example.voidcraft.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightTexture.class)
public class PhaseNightVisionMixin {
    @ModifyArg(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 3
            ),
            index = 0
    )
    private float setPhaseVisionLight(float original) {
        return hasLocalPhaseVision() ? 1.0F : original;
    }

    private static boolean hasLocalPhaseVision() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }

        return player.getData(ModAttachments.IN_PHASE.get())
                || player.getData(ModAttachments.IN_VOID.get());
    }
}
