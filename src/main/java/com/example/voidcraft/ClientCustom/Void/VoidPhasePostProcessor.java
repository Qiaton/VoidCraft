package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidRingInstance;
import com.example.voidcraft.Item.custom.SpatialSword;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.World.PhaseDimensions;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.io.IOException;

public final class VoidPhasePostProcessor {
    public static final int MAX_EFFECTS = 16;
    public static final int DATA_TEXTURE_WIDTH = 12;
    public static final int DATA_TEXTURE_HEIGHT = MAX_EFFECTS + 1;
    public static final ResourceLocation DATA_TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/phase_tear_data.png");
    public static final ResourceLocation MASK_TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "textures/effect/phase_tear_mask.png");
    public static final float PHASE_DIMENSION_FILTER_STRENGTH = 0.70F;
    private static final float SCREEN_AXIS_PACK_RANGE = 2.0F;

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
        resetFrame();
    }

    public static void beginMaskWrite() {
        if (maskTarget == null) {
            return;
        }

        maskTarget.bindWrite(false);
    }

    public static void endMaskWrite() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
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
        writeEffectRow(
                effectIndex,
                ring,
                partialTick,
                centerU,
                centerV,
                halfWidthU,
                halfHeightV,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                centerDepth
        );
    }

    public static void writeEffectRow(
            int effectIndex,
            VoidRingInstance ring,
            float partialTick,
            float centerU,
            float centerV,
            float halfWidthU,
            float halfHeightV,
            float rightAxisU,
            float rightAxisV,
            float upAxisU,
            float upAxisV,
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
        writePackedU16(
                8,
                row,
                0.0F,
                0.0F
        );
        writePackedU16(
                9,
                row,
                0.0F,
                0.0F
        );
        writePackedSignedU16(10, row, rightAxisU, rightAxisV);
        writePackedSignedU16(11, row, upAxisU, upAxisV);
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
        writeBlackHoleEffectRow(
                effectIndex,
                blackHole,
                partialTick,
                centerU,
                centerV,
                halfWidthU,
                halfHeightV,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                centerDepth
        );
    }

    public static void writeBlackHoleEffectRow(
            int effectIndex,
            VoidBlackHoleInstance blackHole,
            float partialTick,
            float centerU,
            float centerV,
            float halfWidthU,
            float halfHeightV,
            float rightAxisU,
            float rightAxisV,
            float upAxisU,
            float upAxisV,
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
        writePackedU16(
                8,
                row,
                Mth.clamp(1.0F / Math.max(0.001F, config.distortionWidthScale()), 0.0F, 1.0F),
                Mth.clamp(1.0F / Math.max(0.001F, config.distortionHeightScale()), 0.0F, 1.0F)
        );
        writePackedU16(
                9,
                row,
                config.flatGate() ? 1.0F : 0.0F,
                0.0F
        );
        writePackedSignedU16(10, row, rightAxisU, rightAxisV);
        writePackedSignedU16(11, row, upAxisU, upAxisV);
    }

    private static void ensureResources(Minecraft mc) {
        if (dataTexture == null || dataPixels == null) {
            dataTexture = new DynamicTexture(DATA_TEXTURE_WIDTH, DATA_TEXTURE_HEIGHT, true);
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

            maskTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
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

        PhaseWorldTransitionClient.markPostEffectFrameRendered();
        float inVoid = getFullScreenPhaseStrength(mc);
        float time = (mc.level.getGameTime() + partialTick) % 128.0F / 128.0F;
        writePackedU16(0, 0, inVoid, time);
        writePackedU16(1, 0, PhaseWorldTransitionClient.enterProgress(), PhaseWorldTransitionClient.exitProgress());
        writePackedU16(
                2,
                0,
                PhaseWorldTransitionClient.holdWhite(),
                PhaseWorldTransitionClient.stageCode()
        );
        writePackedU16(
                3,
                0,
                VoidInOutEffectClient.warpProgress(),
                VoidInOutEffectClient.maskProgress()
        );
        writePackedU16(
                4,
                0,
                VoidInOutEffectClient.isActive() ? 1.0F : 0.0F,
                0.0F
        );
        dataPixels.setPixel(5, 0, 0);
        dataPixels.setPixel(6, 0, 0);
        dataPixels.setPixel(7, 0, 0);
        dataPixels.setPixel(8, 0, 0);
        dataPixels.setPixel(9, 0, 0);
        dataPixels.setPixel(10, 0, 0);
        dataPixels.setPixel(11, 0, 0);
    }

    private static float getFullScreenPhaseStrength(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return 0.0F;
        }

        boolean playerInVoid = mc.player.getData(ModAttachments.IN_PHASE.get())
                || (mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof SpatialSword);
        if (playerInVoid) {
            return 1.0F;
        }

        return PhaseDimensions.isPhaseMirror(mc.level) ? PHASE_DIMENSION_FILTER_STRENGTH : 0.0F;
    }

    private static void clearDataPixels() {
        if (dataPixels != null) {
            dataPixels.fillRect(0, 0, DATA_TEXTURE_WIDTH, DATA_TEXTURE_HEIGHT, 0);
        }
    }

    private static void clearMaskTarget() {
        if (maskTarget != null && maskTarget.getColorTextureId() > 0) {
            maskTarget.clear(Minecraft.ON_OSX);
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }

    private static void writePackedU16(int x, int y, float first, float second) {
        int firstPacked = Mth.clamp((int) Math.round(Mth.clamp(first, 0.0F, 1.0F) * 65535.0F), 0, 65535);
        int secondPacked = Mth.clamp((int) Math.round(Mth.clamp(second, 0.0F, 1.0F) * 65535.0F), 0, 65535);
        dataPixels.setPixelRGBA(
                x,
                y,
                FastColor.ARGB32.color(
                        secondPacked & 0xFF,
                        firstPacked >> 8 & 0xFF,
                        firstPacked & 0xFF,
                        secondPacked >> 8 & 0xFF
                )
        );
    }

    private static void writePackedSignedU16(int x, int y, float first, float second) {
        writePackedU16(x, y, packSigned(first), packSigned(second));
    }

    private static float packSigned(float value) {
        return Mth.clamp(value / (SCREEN_AXIS_PACK_RANGE * 2.0F) + 0.5F, 0.0F, 1.0F);
    }

    private static final class RenderTargetTexture extends AbstractTexture {
        private TextureTarget target;

        private RenderTargetTexture(TextureTarget target) {
            setTarget(target);
        }

        private void setTarget(TextureTarget target) {
            this.target = target;
            this.id = target.getColorTextureId();
            this.setFilter(false, false);
        }

        @Override
        public void load(ResourceManager resourceManager) throws IOException {
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
