package com.example.voidcraft.Effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class VoidRingRenderer {
    private static final int ANGLE_SEGMENTS = 32;
    private static final int RENDER_RADIAL_SEGMENTS = 20;
    private static final int FLASH_ANGLE_SEGMENTS = 16;
    private static final int FLASH_RADIAL_SEGMENTS = 4;
    private static final int MASK_RADIAL_SEGMENTS = 8;
    private static final float INNER_GLOW_BLEND = 3.0F / 7.0F;
    private static final float BLOOM_LAYER_TWO_BLEND = 2.0F / 3.0F;
    private static final float BLOOM_LAYER_FOUR_BLEND = 1.0F / 21.0F;
    private static final int VOLUME_STACK_SLICES = 17;
    private static final float VOLUME_STACK_DEPTH_SCALE = 0.52F;
    private static final float VOLUME_SHELL_CORE_ALPHA = 1.0F;
    private static final double BILLBOARD_EPSILON = 1.0E-8D;

    private VoidRingRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            float partialTick
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        if (ring.preset.renderStyle() == VoidRingInstance.Preset.RenderStyle.FLASH) {
            renderFlash(poseStack, buffer, ring, metrics);
            return;
        }

        Matrix4f matrix4f = poseStack.last().pose();
        int color = ring.preset.color();
        int r = colorRed(color);
        int g = colorGreen(color);
        int b = colorBlue(color);
        float filledFadeStart = ring.preset.filledFadeStart();
        float glowAlpha = ring.preset.glowAlpha() * metrics.fade();
        float outerGlowHalfHeight = scaleFromBase(metrics.halfHeight(), ring.preset.glowHeightScale(), 1.0F);
        float outerGlowHalfWidth = scaleFromBase(metrics.halfWidth(), ring.preset.glowWidthScale(), 1.0F);
        float innerGlowHalfHeight = scaleFromBase(metrics.halfHeight(), ring.preset.glowHeightScale(), INNER_GLOW_BLEND);
        float innerGlowHalfWidth = scaleFromBase(metrics.halfWidth(), ring.preset.glowWidthScale(), INNER_GLOW_BLEND);

        renderFilledLayer(
                buffer,
                matrix4f,
                outerGlowHalfHeight,
                outerGlowHalfWidth,
                -0.006F,
                glowAlpha * 0.40F,
                filledFadeStart,
                r,
                g,
                b
        );

        renderFilledLayer(
                buffer,
                matrix4f,
                innerGlowHalfHeight,
                innerGlowHalfWidth,
                -0.005F,
                glowAlpha * 0.68F,
                filledFadeStart,
                r,
                g,
                b
        );

        renderVolumeShell(
                buffer,
                matrix4f,
                metrics,
                filledFadeStart,
                r,
                g,
                b,
                ring
        );

        renderFilledLayer(
                buffer,
                matrix4f,
                metrics.halfHeight() * 0.92F,
                metrics.lineHalfWidth(),
                0.012F,
                metrics.lineAlpha(),
                filledFadeStart,
                r,
                g,
                b
        );
    }

    private static void renderFlash(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            RenderMetrics metrics
    ) {
        Matrix4f matrix4f = poseStack.last().pose();
        int color = ring.preset.color();
        int r = colorRed(color);
        int g = colorGreen(color);
        int b = colorBlue(color);
        float filledFadeStart = ring.preset.filledFadeStart();
        float fade = metrics.fade();
        float glowAlpha = ring.preset.glowAlpha() * fade;
        float coreAlpha = ring.preset.coreAlpha() * fade;

        // 炮台这类高频小闪光只保留两层平面柔光，跳过完整 ring 的体积壳。
        renderFilledLayer(
                buffer,
                matrix4f,
                scaleFromBase(metrics.halfHeight(), ring.preset.glowHeightScale(), 1.0F),
                scaleFromBase(metrics.halfWidth(), ring.preset.glowWidthScale(), 1.0F),
                -0.006F,
                glowAlpha * 0.58F,
                filledFadeStart,
                r,
                g,
                b,
                FLASH_RADIAL_SEGMENTS,
                FLASH_ANGLE_SEGMENTS
        );

        renderFilledLayer(
                buffer,
                matrix4f,
                metrics.halfHeight() * 0.88F,
                metrics.halfWidth() * 0.88F,
                0.006F,
                coreAlpha * 0.78F + metrics.lineAlpha() * 0.18F,
                filledFadeStart,
                r,
                g,
                b,
                FLASH_RADIAL_SEGMENTS,
                FLASH_ANGLE_SEGMENTS
        );
    }

    public static void renderShaderCompat(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            float partialTick,
            int light
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        if (ring.preset.renderStyle() == VoidRingInstance.Preset.RenderStyle.FLASH) {
            renderShaderFlashGlow(poseStack, buffer, ring, metrics, light);
            return;
        }

        Matrix4f matrix4f = poseStack.last().pose();
        float coreAlpha = ring.preset.coreAlpha() * metrics.fade();
        float glowAlpha = ring.preset.glowAlpha() * metrics.fade();
        float mergedAlpha = Mth.clamp(
                coreAlpha * 0.66F
                        + glowAlpha * 0.20F
                        + metrics.lineAlpha() * 0.10F,
                0.0F,
                1.0F
        );

        renderTexturedLayer(
                buffer,
                matrix4f,
                scaleFromBase(metrics.halfHeight(), ring.preset.shaderGlowHeightScale(), 1.0F),
                scaleFromBase(metrics.halfWidth(), ring.preset.shaderGlowWidthScale(), 1.0F),
                -0.006F,
                boostShaderCompatAlpha(mergedAlpha, ring.preset.shaderCompatCoreGain()),
                ring.preset.color(),
                light
        );
    }

    private static void renderShaderFlashGlow(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            RenderMetrics metrics,
            int light
    ) {
        Matrix4f matrix4f = poseStack.last().pose();
        float coreAlpha = ring.preset.coreAlpha() * metrics.fade();
        float glowAlpha = ring.preset.glowAlpha() * metrics.fade();
        float bloomAlpha = Mth.clamp(
                glowAlpha * ring.preset.shaderCompatBloomGlowWeight()
                        + coreAlpha * ring.preset.shaderCompatBloomCoreWeight(),
                0.0F,
                1.0F
        ) * ring.preset.shaderCompatBloomAlphaScale();

        renderTexturedLayer(
                buffer,
                matrix4f,
                scaleFromBase(metrics.halfHeight(), ring.preset.shaderGlowHeightScale(), 1.0F),
                scaleFromBase(metrics.halfWidth(), ring.preset.shaderGlowWidthScale(), 1.0F),
                -0.007F,
                boostShaderCompatAlpha(bloomAlpha * 0.62F, ring.preset.shaderCompatBloomGain()),
                ring.preset.color(),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                metrics.halfHeight() * 0.78F,
                metrics.halfWidth() * 0.78F,
                -0.003F,
                boostShaderCompatAlpha((coreAlpha * 0.72F + glowAlpha * 0.18F) * ring.preset.shaderCompatBloomAlphaScale(), ring.preset.shaderCompatBloomGain()),
                ring.preset.color(),
                light
        );
    }

    public static void renderShaderGlow(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            float partialTick,
            int light
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        Matrix4f matrix4f = poseStack.last().pose();
        float coreAlpha = ring.preset.coreAlpha() * metrics.fade();
        float glowAlpha = ring.preset.glowAlpha() * metrics.fade();
        float bloomAlpha = Mth.clamp(
                glowAlpha * ring.preset.shaderCompatBloomGlowWeight()
                        + coreAlpha * ring.preset.shaderCompatBloomCoreWeight(),
                0.0F,
                1.0F
        );
        float bloomOuterHalfHeight = scaleFromBase(metrics.halfHeight(), ring.preset.shaderGlowHeightScale(), 1.0F);
        float bloomOuterHalfWidth = scaleFromBase(metrics.halfWidth(), ring.preset.shaderGlowWidthScale(), 1.0F);
        float bloomMidLargeHalfHeight = scaleFromBase(metrics.halfHeight(), ring.preset.shaderGlowHeightScale(), BLOOM_LAYER_TWO_BLEND);
        float bloomMidLargeHalfWidth = scaleFromBase(metrics.halfWidth(), ring.preset.shaderGlowWidthScale(), BLOOM_LAYER_TWO_BLEND);
        float bloomInnerHalfHeight = scaleFromBase(metrics.halfHeight(), ring.preset.shaderGlowHeightScale(), BLOOM_LAYER_FOUR_BLEND);
        float bloomInnerHalfWidth = scaleFromBase(metrics.halfWidth(), ring.preset.shaderGlowWidthScale(), BLOOM_LAYER_FOUR_BLEND);
        float bloomCoreHalfHeight = scaleFromBase(metrics.halfHeight(), ring.preset.shaderGlowHeightScale(), 0.0F);
        float bloomCoreHalfWidth = scaleFromBase(metrics.halfWidth(), ring.preset.shaderGlowWidthScale(), 0.0F);
        float bloomLayerAlphaScale = ring.preset.shaderCompatBloomAlphaScale();

        renderShaderGlowVolumeShell(
                buffer,
                matrix4f,
                ring,
                light,
                bloomCoreHalfHeight,
                bloomCoreHalfWidth,
                coreAlpha,
                glowAlpha,
                bloomLayerAlphaScale
        );

        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomOuterHalfHeight,
                bloomOuterHalfWidth,
                -0.009F,
                boostShaderCompatAlpha(bloomAlpha * 0.34F * bloomLayerAlphaScale, ring.preset.shaderCompatBloomGain()),
                ring.preset.color(),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomMidLargeHalfHeight,
                bloomMidLargeHalfWidth,
                -0.008F,
                boostShaderCompatAlpha(bloomAlpha * 0.52F * bloomLayerAlphaScale, ring.preset.shaderCompatBloomGain()),
                ring.preset.color(),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomInnerHalfHeight,
                bloomInnerHalfWidth,
                -0.004F,
                boostShaderCompatAlpha(bloomAlpha * 0.56F * bloomLayerAlphaScale, ring.preset.shaderCompatBloomGain()),
                ring.preset.color(),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomCoreHalfHeight,
                bloomCoreHalfWidth,
                -0.003F,
                boostShaderCompatAlpha(
                        Mth.clamp(
                                coreAlpha * ring.preset.shaderCompatBloomCoreLayerCoreWeight()
                                        + glowAlpha * ring.preset.shaderCompatBloomCoreLayerGlowWeight(),
                                0.0F,
                                1.0F
                        ) * bloomLayerAlphaScale,
                        ring.preset.shaderCompatBloomGain()
                ),
                ring.preset.color(),
                light
        );
    }

    public static void renderMask(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            float partialTick,
            int effectIndex
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        MaskMetrics maskMetrics = computeMaskMetrics(ring, partialTick, metrics);
        if (maskMetrics.halfHeight() <= 0.001F || maskMetrics.halfWidth() <= 0.001F) {
            return;
        }

        renderMaskLayer(buffer, poseStack.last().pose(), maskMetrics.halfHeight(), maskMetrics.halfWidth(), -0.003F, effectIndex);
    }

    public static ScreenMaskData computeScreenMaskData(VoidRingInstance ring, Vec3 center, Vec3 cameraPos, float partialTick) {
        return computeScreenMaskData(
                Minecraft.getInstance(),
                ring,
                center,
                partialTick,
                computeDistortionFacingData(ring, center, cameraPos)
        );
    }

    public static ScreenMaskData computeScreenMaskData(
            Minecraft mc,
            VoidRingInstance ring,
            Vec3 center,
            float partialTick,
            FacingData facingData
    ) {
        return computeScreenMaskData(
                mc,
                ring,
                center,
                partialTick,
                facingData,
                RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix()
        );
    }

    public static ScreenMaskData computeScreenMaskData(
            Minecraft mc,
            VoidRingInstance ring,
            Vec3 center,
            float partialTick,
            FacingData facingData,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        MaskMetrics maskMetrics = computeMaskMetrics(ring, partialTick, metrics);
        if (maskMetrics.halfHeight() <= 0.001F || maskMetrics.halfWidth() <= 0.001F || mc.gameRenderer == null) {
            return null;
        }

        Vec3 forward = facingData.forward();
        Vec3 horizontal = facingData.horizontal();
        Vec3 vertical = facingData.vertical();
        Vec3 planeCenter = center.add(forward.scale(-0.003D));

        Vec3 centerNdc = projectPoint(mc, planeCenter, modelViewMatrix, projectionMatrix);
        Vec3 leftNdc = projectPoint(mc, planeCenter.subtract(horizontal.scale(maskMetrics.halfWidth())), modelViewMatrix, projectionMatrix);
        Vec3 rightNdc = projectPoint(mc, planeCenter.add(horizontal.scale(maskMetrics.halfWidth())), modelViewMatrix, projectionMatrix);
        Vec3 downNdc = projectPoint(mc, planeCenter.subtract(vertical.scale(maskMetrics.halfHeight())), modelViewMatrix, projectionMatrix);
        Vec3 upNdc = projectPoint(mc, planeCenter.add(vertical.scale(maskMetrics.halfHeight())), modelViewMatrix, projectionMatrix);
        if (centerNdc == null || leftNdc == null || rightNdc == null || downNdc == null || upNdc == null) {
            return null;
        }

        float centerU = (float) (centerNdc.x * 0.5D + 0.5D);
        float centerV = (float) (centerNdc.y * 0.5D + 0.5D);
        float halfWidthU = (float) (Math.max(Math.abs(rightNdc.x - centerNdc.x), Math.abs(leftNdc.x - centerNdc.x)) * 0.5D);
        float halfHeightV = (float) (Math.max(Math.abs(upNdc.y - centerNdc.y), Math.abs(downNdc.y - centerNdc.y)) * 0.5D);
        if (halfWidthU <= 0.0001F || halfHeightV <= 0.0001F) {
            return null;
        }

        float centerDepth = Mth.clamp((float) (centerNdc.z * 0.5D + 0.5D), 0.0F, 1.0F);
        return new ScreenMaskData(
                Mth.clamp(centerU, 0.0F, 1.0F),
                Mth.clamp(centerV, 0.0F, 1.0F),
                Mth.clamp(halfWidthU, 0.0F, 1.0F),
                Mth.clamp(halfHeightV, 0.0F, 1.0F),
                centerDepth
        );
    }

    public static void applyCameraFacingRotation(PoseStack poseStack, VoidRingInstance ring, Vec3 center, Vec3 cameraPos) {
        applyCameraFacingRotation(poseStack, ring, computeFacingData(ring, center, cameraPos));
    }

    private static Vec3 projectPoint(Minecraft mc, Vec3 point) {
        return projectPoint(mc, point, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
    }

    private static Vec3 projectPoint(Minecraft mc, Vec3 point, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vector4f clip = new Vector4f(
                (float) (point.x - cameraPos.x),
                (float) (point.y - cameraPos.y),
                (float) (point.z - cameraPos.z),
                1.0F
        );
        clip.mul(modelViewMatrix);
        clip.mul(projectionMatrix);
        if (clip.w() <= 1.0E-6F) {
            return null;
        }
        float invW = 1.0F / clip.w();
        return new Vec3(clip.x() * invW, clip.y() * invW, clip.z() * invW);
    }

    public static void applyCameraFacingRotation(PoseStack poseStack, VoidRingInstance ring, FacingData facingData) {
        applyFacingRotation(poseStack, facingData, ring.preset.followCameraPitch());
    }

    public static void applyDistortionFacingRotation(PoseStack poseStack, VoidRingInstance ring, FacingData facingData) {
        applyFacingRotation(poseStack, facingData, ring.preset.distortionFollowCameraPitch());
    }

    private static void applyFacingRotation(PoseStack poseStack, FacingData facingData, boolean followPitch) {
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(facingData.yaw()));
        if (followPitch) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(facingData.pitch()));
        }
    }

    public static FacingData computeFacingData(VoidRingInstance ring, Vec3 center, Vec3 cameraPos) {
        return computeFacingData(center, cameraPos, ring.preset.followCameraPitch());
    }

    public static FacingData computeDistortionFacingData(VoidRingInstance ring, Vec3 center, Vec3 cameraPos) {
        return computeFacingData(center, cameraPos, ring.preset.distortionFollowCameraPitch());
    }

    private static FacingData computeFacingData(Vec3 center, Vec3 cameraPos, boolean followPitch) {
        Vec3 toCamera = cameraPos.subtract(center);
        double horizontalLength = Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z);
        float yaw = (float) Math.atan2(toCamera.x, toCamera.z);
        if (!followPitch) {
            return new FacingData(
                    yaw,
                    0.0F,
                    new Vec3(Mth.sin(yaw), 0.0D, Mth.cos(yaw)),
                    new Vec3(Mth.cos(yaw), 0.0D, -Mth.sin(yaw)),
                    new Vec3(0.0D, 1.0D, 0.0D)
            );
        }

        Vec3 forward = toCamera.lengthSqr() < BILLBOARD_EPSILON
                ? new Vec3(Mth.sin(yaw), 0.0D, Mth.cos(yaw))
                : toCamera.normalize();
        Vec3 horizontal = new Vec3(forward.z, 0.0D, -forward.x);
        if (horizontal.lengthSqr() < BILLBOARD_EPSILON) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontal = horizontal.normalize();
        }

        Vec3 vertical = forward.cross(horizontal);
        if (vertical.lengthSqr() < BILLBOARD_EPSILON) {
            vertical = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            vertical = vertical.normalize();
        }

        float pitch = (float) -Math.atan2(forward.y, Math.max(1.0E-6D, horizontalLength / Math.max(1.0E-6D, toCamera.length())));
        return new FacingData(yaw, pitch, forward, horizontal, vertical);
    }

    private static RenderMetrics computeMetrics(VoidRingInstance ring, float partialTick) {
        VoidRingInstance.Preset preset = ring.preset;
        float progress = ring.getProgress(partialTick);
        float peakHoldRatio = Mth.clamp((float) preset.peakHoldTicks() / Math.max(1.0F, preset.durationTicks()), 0.0F, 0.70F);
        float expandEnd = 0.18F;
        float collapseStart = Mth.clamp(expandEnd + peakHoldRatio, expandEnd, 0.98F);
        float expand = smoothstep(0.0F, expandEnd, progress);
        float collapseTimeline = normalizedProgress(collapseStart, 1.0F, progress);
        float collapse = smoothstep(0.0F, 1.0F, collapseTimeline);
        float fade = 1.0F - smoothstep(0.60F, 1.0F, collapseTimeline);

        float burstHeight = Mth.lerp(expand, preset.startHalfHeight(), preset.peakHalfHeight());
        float burstWidth = Mth.lerp(expand, preset.startHalfWidth(), preset.peakHalfWidth());
        float halfHeight = Mth.lerp(collapse, burstHeight, preset.endHalfHeight()) * ring.scale;
        float halfWidth = Mth.lerp(collapse, burstWidth, preset.endHalfWidth()) * ring.scale;

        float lineAlpha = preset.lineAlpha()
                * smoothstep(0.18F, 0.78F, collapseTimeline)
                * (1.0F - smoothstep(0.90F, 1.0F, collapseTimeline));
        float lineHalfWidth = Math.max(preset.endHalfWidth() * ring.scale, halfWidth * 0.35F);

        return new RenderMetrics(halfHeight, halfWidth, fade, lineAlpha, lineHalfWidth);
    }

    private static MaskMetrics computeMaskMetrics(VoidRingInstance ring, float partialTick, RenderMetrics metrics) {
        float progress = ring.getProgress(partialTick);
        float expand = smoothstep(0.0F, 0.16F, progress);
        float baseHalfHeight = Math.max(metrics.halfHeight(), ring.preset.peakHalfHeight() * ring.scale * expand);
        float baseHalfWidth = Math.max(metrics.halfWidth(), ring.preset.peakHalfWidth() * ring.scale * expand);
        return new MaskMetrics(
                baseHalfHeight * ring.preset.distortionHeightScale(),
                baseHalfWidth * ring.preset.distortionWidthScale()
        );
    }

    private static void renderFilledLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            float alpha,
            float filledFadeStart,
            int r,
            int g,
            int b
    ) {
        renderFilledLayer(
                buffer,
                matrix4f,
                halfHeight,
                halfWidth,
                z,
                alpha,
                filledFadeStart,
                r,
                g,
                b,
                RENDER_RADIAL_SEGMENTS,
                ANGLE_SEGMENTS
        );
    }

    private static void renderFilledLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            float alpha,
            float filledFadeStart,
            int r,
            int g,
            int b,
            int radialSegments,
            int angleSegments
    ) {
        if (alpha <= 0.01F || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        int safeRadialSegments = Math.max(1, radialSegments);
        int safeAngleSegments = Math.max(3, angleSegments);
        for (int radial = 0; radial < safeRadialSegments; radial++) {
            float radius0 = (float) radial / safeRadialSegments;
            float radius1 = (float) (radial + 1) / safeRadialSegments;
            float alpha0 = filledLayerAlpha(alpha, radius0, filledFadeStart);
            float alpha1 = filledLayerAlpha(alpha, radius1, filledFadeStart);

            for (int angle = 0; angle < safeAngleSegments; angle++) {
                float theta0 = Mth.TWO_PI * angle / safeAngleSegments;
                float theta1 = Mth.TWO_PI * (angle + 1) / safeAngleSegments;

                float x00 = Mth.cos(theta0) * halfWidth * radius0;
                float y00 = Mth.sin(theta0) * halfHeight * radius0;
                float x01 = Mth.cos(theta1) * halfWidth * radius0;
                float y01 = Mth.sin(theta1) * halfHeight * radius0;
                float x11 = Mth.cos(theta1) * halfWidth * radius1;
                float y11 = Mth.sin(theta1) * halfHeight * radius1;
                float x10 = Mth.cos(theta0) * halfWidth * radius1;
                float y10 = Mth.sin(theta0) * halfHeight * radius1;

                putVertex(buffer, matrix4f, x00, y00, z, r, g, b, alphaToByte(alpha0));
                putVertex(buffer, matrix4f, x01, y01, z, r, g, b, alphaToByte(alpha0));
                putVertex(buffer, matrix4f, x11, y11, z, r, g, b, alphaToByte(alpha1));
                putVertex(buffer, matrix4f, x10, y10, z, r, g, b, alphaToByte(alpha1));
            }
        }
    }

    private static void renderVolumeShell(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            RenderMetrics metrics,
            float filledFadeStart,
            int r,
            int g,
            int b,
            VoidRingInstance ring
    ) {
        float fade = metrics.fade();
        if (fade <= 0.01F) {
            return;
        }

        renderVolumeStackLayer(
                buffer,
                matrix4f,
                metrics.halfHeight(),
                metrics.halfWidth(),
                ring.preset.coreAlpha() * fade * VOLUME_SHELL_CORE_ALPHA,
                filledFadeStart,
                r,
                g,
                b
        );
    }

    private static void renderVolumeStackLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float alpha,
            float filledFadeStart,
            int r,
            int g,
            int b
    ) {
        if (alpha <= 0.01F || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        float totalWeight = volumeStackTotalWeight();
        float middle = (VOLUME_STACK_SLICES - 1) * 0.5F;
        for (int slice = 0; slice < VOLUME_STACK_SLICES; slice++) {
            float normalizedDepth = (slice - middle) / middle;
            float crossSectionScale = volumeStackCrossSectionScale(normalizedDepth);
            if (crossSectionScale <= 0.001F) {
                continue;
            }

            renderFilledLayer(
                    buffer,
                    matrix4f,
                    halfHeight * crossSectionScale,
                    halfWidth * crossSectionScale,
                    -0.003F + normalizedDepth * halfWidth * VOLUME_STACK_DEPTH_SCALE,
                    alpha * crossSectionScale / totalWeight,
                    filledFadeStart,
                    r,
                    g,
                    b
            );
        }
    }

    private static void renderTexturedLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            float alpha,
            int color,
            int light
    ) {
        int a = alphaToByte(alpha);
        if (a <= 0 || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        int r = colorRed(color);
        int g = colorGreen(color);
        int b = colorBlue(color);
        putCompatVertex(buffer, matrix4f, -halfWidth, -halfHeight, z, r, g, b, a, 0.0F, 1.0F, light);
        putCompatVertex(buffer, matrix4f, halfWidth, -halfHeight, z, r, g, b, a, 1.0F, 1.0F, light);
        putCompatVertex(buffer, matrix4f, halfWidth, halfHeight, z, r, g, b, a, 1.0F, 0.0F, light);
        putCompatVertex(buffer, matrix4f, -halfWidth, halfHeight, z, r, g, b, a, 0.0F, 0.0F, light);
    }

    private static void renderShaderGlowVolumeShell(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            VoidRingInstance ring,
            int light,
            float bloomCoreHalfHeight,
            float bloomCoreHalfWidth,
            float coreAlpha,
            float glowAlpha,
            float bloomLayerAlphaScale
    ) {
        if (coreAlpha <= 0.01F && glowAlpha <= 0.01F) {
            return;
        }

        renderTexturedVolumeStackLayer(
                buffer,
                matrix4f,
                bloomCoreHalfHeight,
                bloomCoreHalfWidth,
                boostShaderCompatAlpha(
                        Mth.clamp(
                                coreAlpha * ring.preset.shaderCompatBloomCoreLayerCoreWeight()
                                        + glowAlpha * ring.preset.shaderCompatBloomCoreLayerGlowWeight(),
                                0.0F,
                                1.0F
                        ) * bloomLayerAlphaScale * VOLUME_SHELL_CORE_ALPHA,
                        ring.preset.shaderCompatBloomGain()
                ),
                ring.preset.color(),
                light
        );
    }

    private static void renderTexturedVolumeStackLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float alpha,
            int color,
            int light
    ) {
        int a = alphaToByte(alpha);
        if (a <= 0 || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        float totalWeight = volumeStackTotalWeight();
        float middle = (VOLUME_STACK_SLICES - 1) * 0.5F;
        for (int slice = 0; slice < VOLUME_STACK_SLICES; slice++) {
            float normalizedDepth = (slice - middle) / middle;
            float crossSectionScale = volumeStackCrossSectionScale(normalizedDepth);
            if (crossSectionScale <= 0.001F) {
                continue;
            }

            int sliceAlpha = alphaToByte(alpha * crossSectionScale / totalWeight);
            renderTexturedLayer(
                    buffer,
                    matrix4f,
                    halfHeight * crossSectionScale,
                    halfWidth * crossSectionScale,
                    -0.003F + normalizedDepth * halfWidth * VOLUME_STACK_DEPTH_SCALE,
                    sliceAlpha / 255.0F,
                    color,
                    light
            );
        }
    }

    private static void renderMaskLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            int effectIndex
    ) {
        int ringIdByte = Mth.clamp(effectIndex + 1, 1, 255);
        for (int radial = 0; radial < MASK_RADIAL_SEGMENTS; radial++) {
            float radius0 = (float) radial / MASK_RADIAL_SEGMENTS;
            float radius1 = (float) (radial + 1) / MASK_RADIAL_SEGMENTS;

            for (int angle = 0; angle < ANGLE_SEGMENTS; angle++) {
                float theta0 = Mth.TWO_PI * angle / ANGLE_SEGMENTS;
                float theta1 = Mth.TWO_PI * (angle + 1) / ANGLE_SEGMENTS;

                float localX00 = Mth.cos(theta0) * radius0;
                float localY00 = Mth.sin(theta0) * radius0;
                float localX01 = Mth.cos(theta1) * radius0;
                float localY01 = Mth.sin(theta1) * radius0;
                float localX11 = Mth.cos(theta1) * radius1;
                float localY11 = Mth.sin(theta1) * radius1;
                float localX10 = Mth.cos(theta0) * radius1;
                float localY10 = Mth.sin(theta0) * radius1;

                putMaskVertex(buffer, matrix4f, localX00 * halfWidth, localY00 * halfHeight, z, localX00, localY00, ringIdByte);
                putMaskVertex(buffer, matrix4f, localX01 * halfWidth, localY01 * halfHeight, z, localX01, localY01, ringIdByte);
                putMaskVertex(buffer, matrix4f, localX11 * halfWidth, localY11 * halfHeight, z, localX11, localY11, ringIdByte);
                putMaskVertex(buffer, matrix4f, localX10 * halfWidth, localY10 * halfHeight, z, localX10, localY10, ringIdByte);
            }
        }
    }

    private static float filledLayerAlpha(float alpha, float normalizedRadius, float fadeStart) {
        float edgeFade = 1.0F - smoothstep(fadeStart, 1.0F, normalizedRadius);
        return alpha * edgeFade;
    }

    private static float smoothstep(float start, float end, float value) {
        if (start == end) {
            return value < start ? 0.0F : 1.0F;
        }
        float t = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float normalizedProgress(float start, float end, float value) {
        if (start >= end) {
            return value >= end ? 1.0F : 0.0F;
        }
        return Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
    }

    private static float boostShaderCompatAlpha(float alpha, float gain) {
        return Mth.clamp(alpha * gain, 0.0F, 1.0F);
    }

    private static float scaleFromBase(float base, float maxScale, float blend) {
        return base * Mth.lerp(Mth.clamp(blend, 0.0F, 1.0F), 1.0F, Math.max(1.0F, maxScale));
    }

    private static float volumeStackTotalWeight() {
        float totalWeight = 0.0F;
        float middle = (VOLUME_STACK_SLICES - 1) * 0.5F;
        for (int slice = 0; slice < VOLUME_STACK_SLICES; slice++) {
            totalWeight += volumeStackCrossSectionScale((slice - middle) / middle);
        }
        return Math.max(totalWeight, 1.0F);
    }

    private static float volumeStackCrossSectionScale(float normalizedDepth) {
        return Mth.sqrt(Math.max(0.0F, 1.0F - normalizedDepth * normalizedDepth));
    }

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }

    private static int localCoordToByte(float local) {
        return Mth.clamp((int) Math.round((local * 0.5F + 0.5F) * 255.0F), 0, 255);
    }

    private static int colorRed(int color) {
        return color >> 16 & 255;
    }

    private static int colorGreen(int color) {
        return color >> 8 & 255;
    }

    private static int colorBlue(int color) {
        return color & 255;
    }

    private static void putVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float x,
            float y,
            float z,
            int r,
            int g,
            int b,
            int a
    ) {
        buffer.addVertex(matrix4f, x, y, z)
                .setColor(r, g, b, a);
    }

    private static void putCompatVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float x,
            float y,
            float z,
            int r,
            int g,
            int b,
            int a,
            float u,
            float v,
            int light
    ) {
        buffer.addVertex(matrix4f, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void putMaskVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float x,
            float y,
            float z,
            float localX,
            float localY,
            int ringIdByte
    ) {
        buffer.addVertex(matrix4f, x, y, z)
                .setColor(localCoordToByte(localX), localCoordToByte(localY), ringIdByte, 255);
    }

    private record RenderMetrics(float halfHeight, float halfWidth, float fade, float lineAlpha, float lineHalfWidth) {
    }

    private record MaskMetrics(float halfHeight, float halfWidth) {
    }

    public record ScreenMaskData(float centerU, float centerV, float halfWidthU, float halfHeightV, float centerDepth) {
    }

    public record FacingData(float yaw, float pitch, Vec3 forward, Vec3 horizontal, Vec3 vertical) {
    }
}
