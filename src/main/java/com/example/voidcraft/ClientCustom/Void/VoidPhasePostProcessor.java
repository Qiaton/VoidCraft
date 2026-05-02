package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class VoidPhasePostProcessor {
    public static final int MAX_EFFECTS = 16;
    public static final int DATA_TEXTURE_WIDTH = 8;
    public static final int DATA_TEXTURE_HEIGHT = MAX_EFFECTS + 1;
    public static final Identifier DATA_TEXTURE_ID =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/phase_tear_data.png");
    public static final Identifier MASK_TEXTURE_ID =
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/phase_tear_mask.png");

    private static DynamicTexture dataTexture;
    private static NativeImage dataPixels;
    private static TextureTarget maskTarget;
    private static RenderTargetTexture maskTexture;

    private VoidPhasePostProcessor() {
    }

    public static void ensureTextureRegistered(Minecraft mc) {
        ensureResources(mc);
    }

    public static void beginFrame(Minecraft mc, float partialTick) {
        ensureResources(mc);
        clearDataPixels();
        clearMaskTarget();
        writeHeader(mc, partialTick);
    }

    public static void finishFrame() {
        if (dataTexture != null) {
            dataTexture.upload();
        }
    }

    public static void resetFrame() {
        if (dataPixels != null && dataTexture != null) {
            clearDataPixels();
            dataTexture.upload();
        }
        clearMaskTarget();
    }

    public static void releaseResources() {
        endMaskWrite();
        if (dataTexture != null) {
            dataTexture.close();
            dataTexture = null;
            dataPixels = null;
        }

        if (maskTexture != null) {
            maskTexture.close();
        } else if (maskTarget != null) {
            maskTarget.destroyBuffers();
            maskTarget = null;
        }
    }

    public static void beginMaskWrite() {
        if (maskTarget == null) {
            return;
        }

        RenderSystem.outputColorTextureOverride = maskTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = null;
    }

    public static void endMaskWrite() {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
    }

    public static boolean shouldRenderRing(Minecraft mc, VoidRingInstance ring, boolean firstPerson) {
        return !(firstPerson && mc.player != null && ring.ownerEntityId == mc.player.getId());
    }

    public static boolean shouldRenderBlackHole(Minecraft mc, VoidBlackHoleInstance blackHole, boolean firstPerson) {
        return !(firstPerson
                && mc.player != null
                && blackHole.config.hideFromOwnerInFirstPerson()
                && blackHole.ownerEntityId == mc.player.getId());
    }

    public static void writeEffectRow(int effectIndex, VoidRingInstance ring, float partialTick) {
        writeEffectRow(effectIndex, ring, partialTick, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F);
    }

    public static void writeEffectRow(int effectIndex, VoidRingInstance ring, float partialTick, float centerDepth) {
        writeEffectRow(effectIndex, ring, partialTick, 0.0F, 0.0F, 0.0F, 0.0F, centerDepth);
    }

    public static void writeEffectRow(
            int effectIndex,
            VoidRingInstance ring,
            float partialTick,
            float centerU,
            float centerV,
            float halfWidthU,
            float halfHeightV,
            float centerDepth
    ) {
        if (dataPixels == null || effectIndex < 0 || effectIndex >= MAX_EFFECTS) {
            return;
        }

        int row = effectIndex + 1;
        float progress = ring.getProgress(partialTick);
        writePackedU16(
                0,
                row,
                progress,
                Mth.clamp(ring.preset.distortionAmplitude() / 12.0F, 0.0F, 1.0F)
        );
        writePackedU16(
                1,
                row,
                Mth.clamp(ring.preset.distortionThickness() / 6.0F, 0.0F, 1.0F),
                Mth.clamp(ring.preset.distortionAlpha() / 2.0F, 0.0F, 1.0F)
        );
        writePackedU16(
                2,
                row,
                Mth.clamp(ring.preset.noiseFrequency() / 18.0F, 0.0F, 1.0F),
                Mth.clamp(ring.preset.noiseScrollSpeed() / 8.0F, 0.0F, 1.0F)
        );
        writePackedU16(
                3,
                row,
                Mth.clamp(centerU, 0.0F, 1.0F),
                Mth.clamp(centerV, 0.0F, 1.0F)
        );
        writePackedU16(
                4,
                row,
                Mth.clamp(halfWidthU, 0.0F, 1.0F),
                Mth.clamp(halfHeightV, 0.0F, 1.0F)
        );
        writePackedU16(
                5,
                row,
                Mth.clamp(ring.preset.swirlStrength(), 0.0F, 1.0F),
                Mth.clamp(ring.preset.suctionStrength(), 0.0F, 1.0F)
        );
        boolean useOcclusionDepth = ring.preset.occludedByBlocks() && centerDepth >= 0.0F && centerDepth <= 1.0F;
        writePackedU16(
                6,
                row,
                useOcclusionDepth ? 1.0F : 0.0F,
                Mth.clamp(centerDepth, 0.0F, 1.0F)
        );
        writePackedU16(
                7,
                row,
                0.0F,
                0.0F
        );
    }

    public static void writeBlackHoleEffectRow(int effectIndex, VoidBlackHoleInstance blackHole, float partialTick) {
        writeBlackHoleEffectRow(effectIndex, blackHole, partialTick, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F);
    }

    public static void writeBlackHoleEffectRow(int effectIndex, VoidBlackHoleInstance blackHole, float partialTick, float centerDepth) {
        writeBlackHoleEffectRow(effectIndex, blackHole, partialTick, 0.0F, 0.0F, 0.0F, 0.0F, centerDepth);
    }

    public static void writeBlackHoleEffectRow(
            int effectIndex,
            VoidBlackHoleInstance blackHole,
            float partialTick,
            float centerU,
            float centerV,
            float halfWidthU,
            float halfHeightV,
            float centerDepth
    ) {
        if (dataPixels == null || effectIndex < 0 || effectIndex >= MAX_EFFECTS) {
            return;
        }

        VoidBlackHoleInstance.Config config = blackHole.config;
        int row = effectIndex + 1;
        float progress = blackHole.getProgress(partialTick);
        writePackedU16(
                0,
                row,
                progress,
                Mth.clamp(config.distortionAmplitude() / 12.0F, 0.0F, 1.0F)
        );
        writePackedU16(
                1,
                row,
                Mth.clamp(config.distortionThickness() / 6.0F, 0.0F, 1.0F),
                Mth.clamp(config.distortionAlpha() / 2.0F, 0.0F, 1.0F)
        );
        writePackedU16(
                2,
                row,
                Mth.clamp(config.noiseFrequency() / 18.0F, 0.0F, 1.0F),
                Mth.clamp(config.noiseScrollSpeed() / 8.0F, 0.0F, 1.0F)
        );
        writePackedU16(
                3,
                row,
                Mth.clamp(centerU, 0.0F, 1.0F),
                Mth.clamp(centerV, 0.0F, 1.0F)
        );
        writePackedU16(
                4,
                row,
                Mth.clamp(halfWidthU, 0.0F, 1.0F),
                Mth.clamp(halfHeightV, 0.0F, 1.0F)
        );
        writePackedU16(
                5,
                row,
                Mth.clamp(config.swirlStrength(), 0.0F, 1.0F),
                Mth.clamp(config.suctionStrength(), 0.0F, 1.0F)
        );
        boolean useOcclusionDepth = config.occludedByBlocks() && centerDepth >= 0.0F && centerDepth <= 1.0F;
        writePackedU16(
                6,
                row,
                useOcclusionDepth ? 1.0F : 0.0F,
                Mth.clamp(centerDepth, 0.0F, 1.0F)
        );
        writePackedU16(
                7,
                row,
                1.0F,
                Mth.clamp(config.centerShadowScale(), 0.0F, 1.0F)
        );
    }

    private static void ensureResources(Minecraft mc) {
        if (dataTexture == null || dataPixels == null) {
            dataTexture = new DynamicTexture(
                    () -> VoidCraft.MODID + " phase tear data",
                    DATA_TEXTURE_WIDTH,
                    DATA_TEXTURE_HEIGHT,
                    true
            );
            dataPixels = dataTexture.getPixels();
            clearDataPixels();
            dataTexture.upload();
            mc.getTextureManager().register(DATA_TEXTURE_ID, dataTexture);
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (maskTarget == null || maskTarget.width != width || maskTarget.height != height) {
            if (maskTarget != null) {
                maskTarget.destroyBuffers();
            }

            maskTarget = new TextureTarget("Phase Tear Mask", width, height, false);
            if (maskTexture == null) {
                maskTexture = new RenderTargetTexture(maskTarget);
                mc.getTextureManager().register(MASK_TEXTURE_ID, maskTexture);
            } else {
                maskTexture.setTarget(maskTarget);
            }
        }
    }

    private static void writeHeader(Minecraft mc, float partialTick) {
        if (dataPixels == null) {
            return;
        }

        float inVoid = mc.player.getData(ModAttachments.IN_VOID.get()) ? 1.0F : 0.0F;
        float time = (mc.level.getGameTime() + partialTick) % 128.0F / 128.0F;
        writePackedU16(0, 0, inVoid, time);
        dataPixels.setPixel(1, 0, 0);
        dataPixels.setPixel(2, 0, 0);
        dataPixels.setPixel(3, 0, 0);
        dataPixels.setPixel(4, 0, 0);
        dataPixels.setPixel(5, 0, 0);
        dataPixels.setPixel(6, 0, 0);
        dataPixels.setPixel(7, 0, 0);
    }

    private static void clearDataPixels() {
        if (dataPixels != null) {
            dataPixels.fillRect(0, 0, DATA_TEXTURE_WIDTH, DATA_TEXTURE_HEIGHT, 0);
        }
    }

    private static void clearMaskTarget() {
        if (maskTarget != null && maskTarget.getColorTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(maskTarget.getColorTexture(), 0);
        }
    }

    private static void writePackedU16(int x, int y, float first, float second) {
        int firstPacked = Mth.clamp((int) Math.round(Mth.clamp(first, 0.0F, 1.0F) * 65535.0F), 0, 65535);
        int secondPacked = Mth.clamp((int) Math.round(Mth.clamp(second, 0.0F, 1.0F) * 65535.0F), 0, 65535);
        dataPixels.setPixel(
                x,
                y,
                ARGB.color(
                        secondPacked & 0xFF,
                        firstPacked >> 8 & 0xFF,
                        firstPacked & 0xFF,
                        secondPacked >> 8 & 0xFF
                )
        );
    }

    private static final class RenderTargetTexture extends AbstractTexture {
        private TextureTarget target;

        private RenderTargetTexture(TextureTarget target) {
            setTarget(target);
        }

        private void setTarget(TextureTarget target) {
            this.target = target;
            this.texture = target.getColorTexture();
            this.textureView = target.getColorTextureView();
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }

        @Override
        public void close() {
            TextureTarget targetToDestroy = this.target;
            this.target = null;
            if (maskTexture == this) {
                maskTexture = null;
            }
            if (maskTarget == targetToDestroy) {
                maskTarget = null;
            }
            if (targetToDestroy != null) {
                targetToDestroy.destroyBuffers();
            }
        }
    }
}
