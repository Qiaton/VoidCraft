package com.example.voidcraft.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VoidRingRenderer {
    private static final int ANGLE_SEGMENTS = 32;
    private static final int RENDER_RADIAL_SEGMENTS = 20;
    private static final int MASK_RADIAL_SEGMENTS = 8;
    private static final float INNER_GLOW_BLEND = 3.0F / 7.0F;
    private static final float BLOOM_LAYER_TWO_BLEND = 2.0F / 3.0F;
    private static final float BLOOM_LAYER_FOUR_BLEND = 1.0F / 21.0F;
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
        Matrix4f matrix4f = poseStack.last().pose();
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
                255,
                255,
                255
        );

        renderFilledLayer(
                buffer,
                matrix4f,
                innerGlowHalfHeight,
                innerGlowHalfWidth,
                -0.005F,
                glowAlpha * 0.68F,
                255,
                255,
                255
        );

        renderFilledLayer(
                buffer,
                matrix4f,
                metrics.halfHeight(),
                metrics.halfWidth(),
                -0.003F,
                ring.preset.coreAlpha() * metrics.fade(),
                255,
                255,
                255
        );

        renderFilledLayer(
                buffer,
                matrix4f,
                metrics.halfHeight() * 0.92F,
                metrics.lineHalfWidth(),
                0.012F,
                metrics.lineAlpha(),
                255,
                255,
                255
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
        Matrix4f matrix4f = poseStack.last().pose();
        float coreAlpha = ring.preset.coreAlpha() * metrics.fade();
        float glowAlpha = ring.preset.glowAlpha() * metrics.fade();

        renderTexturedLayer(
                buffer,
                matrix4f,
                scaleFromBase(metrics.halfHeight(), ring.preset.glowHeightScale(), 1.0F),
                scaleFromBase(metrics.halfWidth(), ring.preset.glowWidthScale(), 1.0F),
                -0.006F,
                boostShaderCompatAlpha(glowAlpha * 0.40F, ring.preset.shaderCompatOuterGlowGain()),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                metrics.halfHeight(),
                metrics.halfWidth(),
                -0.003F,
                boostShaderCompatAlpha(coreAlpha, ring.preset.shaderCompatCoreGain()),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                metrics.halfHeight() * 0.92F,
                metrics.lineHalfWidth(),
                0.012F,
                boostShaderCompatAlpha(metrics.lineAlpha(), ring.preset.shaderCompatLineGain()),
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

        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomOuterHalfHeight,
                bloomOuterHalfWidth,
                -0.009F,
                boostShaderCompatAlpha(bloomAlpha * 0.34F * bloomLayerAlphaScale, ring.preset.shaderCompatBloomGain()),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomMidLargeHalfHeight,
                bloomMidLargeHalfWidth,
                -0.008F,
                boostShaderCompatAlpha(bloomAlpha * 0.52F * bloomLayerAlphaScale, ring.preset.shaderCompatBloomGain()),
                light
        );
        renderTexturedLayer(
                buffer,
                matrix4f,
                bloomInnerHalfHeight,
                bloomInnerHalfWidth,
                -0.004F,
                boostShaderCompatAlpha(bloomAlpha * 0.56F * bloomLayerAlphaScale, ring.preset.shaderCompatBloomGain()),
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
        float halfHeight = metrics.halfHeight() * ring.preset.distortionHeightScale();
        float halfWidth = metrics.halfWidth() * ring.preset.distortionWidthScale();
        if (halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        renderMaskLayer(buffer, poseStack.last().pose(), halfHeight, halfWidth, -0.003F, effectIndex);
    }

    public static ScreenMaskData computeScreenMaskData(VoidRingInstance ring, Vec3 center, Vec3 cameraPos, float partialTick) {
        return computeScreenMaskData(
                Minecraft.getInstance(),
                ring,
                center,
                partialTick,
                computeFacingData(ring, center, cameraPos)
        );
    }

    public static ScreenMaskData computeScreenMaskData(
            Minecraft mc,
            VoidRingInstance ring,
            Vec3 center,
            float partialTick,
            FacingData facingData
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        float halfHeight = metrics.halfHeight() * ring.preset.distortionHeightScale();
        float halfWidth = metrics.halfWidth() * ring.preset.distortionWidthScale();
        if (halfHeight <= 0.001F || halfWidth <= 0.001F || mc.gameRenderer == null) {
            return null;
        }

        Vec3 forward = facingData.forward();
        Vec3 horizontal = facingData.horizontal();
        Vec3 vertical = facingData.vertical();
        Vec3 planeCenter = center.add(forward.scale(-0.003D));

        Vec3 centerNdc = mc.gameRenderer.projectPointToScreen(planeCenter);
        Vec3 leftNdc = mc.gameRenderer.projectPointToScreen(planeCenter.subtract(horizontal.scale(halfWidth)));
        Vec3 rightNdc = mc.gameRenderer.projectPointToScreen(planeCenter.add(horizontal.scale(halfWidth)));
        Vec3 downNdc = mc.gameRenderer.projectPointToScreen(planeCenter.subtract(vertical.scale(halfHeight)));
        Vec3 upNdc = mc.gameRenderer.projectPointToScreen(planeCenter.add(vertical.scale(halfHeight)));

        float centerU = (float) (centerNdc.x * 0.5D + 0.5D);
        float centerV = (float) (centerNdc.y * 0.5D + 0.5D);
        float halfWidthU = (float) (Math.max(Math.abs(rightNdc.x - centerNdc.x), Math.abs(leftNdc.x - centerNdc.x)) * 0.5D);
        float halfHeightV = (float) (Math.max(Math.abs(upNdc.y - centerNdc.y), Math.abs(downNdc.y - centerNdc.y)) * 0.5D);
        if (halfWidthU <= 0.0001F || halfHeightV <= 0.0001F) {
            return null;
        }

        return new ScreenMaskData(
                Mth.clamp(centerU, 0.0F, 1.0F),
                Mth.clamp(centerV, 0.0F, 1.0F),
                Mth.clamp(halfWidthU, 0.0F, 1.0F),
                Mth.clamp(halfHeightV, 0.0F, 1.0F)
        );
    }

    public static void applyCameraFacingRotation(PoseStack poseStack, VoidRingInstance ring, Vec3 center, Vec3 cameraPos) {
        applyCameraFacingRotation(poseStack, ring, computeFacingData(ring, center, cameraPos));
    }

    public static void applyCameraFacingRotation(PoseStack poseStack, VoidRingInstance ring, FacingData facingData) {
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(facingData.yaw()));
        if (ring.preset.followCameraPitch()) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(facingData.pitch()));
        }
    }

    public static FacingData computeFacingData(VoidRingInstance ring, Vec3 center, Vec3 cameraPos) {
        Vec3 toCamera = cameraPos.subtract(center);
        double horizontalLength = Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z);
        float yaw = (float) Math.atan2(toCamera.x, toCamera.z);
        if (!ring.preset.followCameraPitch()) {
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

    private static void renderFilledLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            float alpha,
            int r,
            int g,
            int b
    ) {
        if (alpha <= 0.01F || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        for (int radial = 0; radial < RENDER_RADIAL_SEGMENTS; radial++) {
            float radius0 = (float) radial / RENDER_RADIAL_SEGMENTS;
            float radius1 = (float) (radial + 1) / RENDER_RADIAL_SEGMENTS;
            float alpha0 = filledLayerAlpha(alpha, radius0);
            float alpha1 = filledLayerAlpha(alpha, radius1);

            for (int angle = 0; angle < ANGLE_SEGMENTS; angle++) {
                float theta0 = Mth.TWO_PI * angle / ANGLE_SEGMENTS;
                float theta1 = Mth.TWO_PI * (angle + 1) / ANGLE_SEGMENTS;

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

    private static void renderTexturedLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            float alpha,
            int light
    ) {
        int a = alphaToByte(alpha);
        if (a <= 0 || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        putCompatVertex(buffer, matrix4f, -halfWidth, -halfHeight, z, 255, 255, 255, a, 0.0F, 1.0F, light);
        putCompatVertex(buffer, matrix4f, halfWidth, -halfHeight, z, 255, 255, 255, a, 1.0F, 1.0F, light);
        putCompatVertex(buffer, matrix4f, halfWidth, halfHeight, z, 255, 255, 255, a, 1.0F, 0.0F, light);
        putCompatVertex(buffer, matrix4f, -halfWidth, halfHeight, z, 255, 255, 255, a, 0.0F, 0.0F, light);
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

    private static float filledLayerAlpha(float alpha, float normalizedRadius) {
        float edgeFade = 1.0F - smoothstep(0.62F, 1.0F, normalizedRadius);
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

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }

    private static int localCoordToByte(float local) {
        return Mth.clamp((int) Math.round((local * 0.5F + 0.5F) * 255.0F), 0, 255);
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

    public record ScreenMaskData(float centerU, float centerV, float halfWidthU, float halfHeightV) {
    }

    public record FacingData(float yaw, float pitch, Vec3 forward, Vec3 horizontal, Vec3 vertical) {
    }
}
