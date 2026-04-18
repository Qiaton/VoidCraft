package com.example.voidcraft.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class VoidRingRenderer {
    private static final int ANGLE_SEGMENTS = 48;
    private static final int RADIAL_SEGMENTS = 10;

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            float partialTick,
            int light
    ) {
        RenderMetrics metrics = computeMetrics(ring, partialTick);
        Matrix4f matrix4f = poseStack.last().pose();

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

    private static RenderMetrics computeMetrics(VoidRingInstance ring, float partialTick) {
        VoidRingInstance.Preset preset = ring.preset;
        float progress = ring.getProgress(partialTick);
        float expand = smoothstep(0.0F, 0.18F, progress);
        float collapse = smoothstep(0.20F, 1.0F, progress);
        float fade = 1.0F - smoothstep(0.72F, 1.0F, progress);

        float burstHeight = Mth.lerp(expand, preset.startHalfHeight(), preset.peakHalfHeight());
        float burstWidth = Mth.lerp(expand, preset.startHalfWidth(), preset.peakHalfWidth());
        float halfHeight = Mth.lerp(collapse, burstHeight, preset.endHalfHeight()) * ring.scale;
        float halfWidth = Mth.lerp(collapse, burstWidth, preset.endHalfWidth()) * ring.scale;

        float lineAlpha = preset.lineAlpha()
                * smoothstep(0.30F, 0.82F, progress)
                * (1.0F - smoothstep(0.90F, 1.0F, progress));
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

        for (int radial = 0; radial < RADIAL_SEGMENTS; radial++) {
            float radius0 = (float) radial / RADIAL_SEGMENTS;
            float radius1 = (float) (radial + 1) / RADIAL_SEGMENTS;
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

    private static void renderMaskLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float z,
            int effectIndex
    ) {
        int ringIdByte = Mth.clamp(effectIndex + 1, 1, 255);
        for (int radial = 0; radial < RADIAL_SEGMENTS; radial++) {
            float radius0 = (float) radial / RADIAL_SEGMENTS;
            float radius1 = (float) (radial + 1) / RADIAL_SEGMENTS;

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

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }

    private static int localCoordToByte(float local) {
        return Mth.clamp((int) Math.round((local * 0.5F + 0.5F) * 255.0F), 0, 255);
    }

    public static void putVertex(
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
}
