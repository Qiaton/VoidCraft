package com.example.voidcraft.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VoidBeamRenderer {
    private static final double EPSILON = 1.0E-8D;
    private static final float EDGE_FADE_RATIO = 0.28F;
    private static final int EDGE_FADE_SEGMENTS = 7;
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 WORLD_RIGHT = new Vec3(1.0D, 0.0D, 0.0D);

    private VoidBeamRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBeamInstance beam,
            Vec3 cameraPos,
            float partialTick
    ) {
        renderInternal(poseStack, buffer, beam, cameraPos, partialTick, false, false, 0);
    }

    public static void renderShaderCompat(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBeamInstance beam,
            Vec3 cameraPos,
            float partialTick,
            int light
    ) {
        renderInternal(poseStack, buffer, beam, cameraPos, partialTick, true, false, light);
    }

    public static void renderShaderGlow(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBeamInstance beam,
            Vec3 cameraPos,
            float partialTick,
            int light
    ) {
        renderInternal(poseStack, buffer, beam, cameraPos, partialTick, true, true, light);
    }

    private static void renderInternal(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBeamInstance beam,
            Vec3 cameraPos,
            float partialTick,
            boolean shaderCompat,
            boolean glowPass,
            int light
    ) {
        BeamFrame frame = buildFrame(beam, cameraPos);
        if (frame == null) {
            return;
        }

        float fade = beam.fade(partialTick);
        if (fade <= 0.01F) {
            return;
        }

        Matrix4f matrix4f = poseStack.last().pose();
        VoidBeamInstance.Config config = beam.config;
        float scale = beam.scale();
        float startScale = config.startRadiusScale();
        float endScale = config.endRadiusScale();

        if (shaderCompat && glowPass) {
            float radius = config.glowRadius() * scale * config.shaderCompatBloomWidthScale();
            int color = towardWhite(config.glowColor(), 0.18F);
            float alpha = config.glowAlpha() * fade * config.shaderCompatBloomAlphaScale();
            renderBeamRibbon(buffer, matrix4f, frame, frame.billboardAxis(), radius * 1.55F, radius * 1.18F, startScale, endScale, color, alpha * 0.44F, true, light);
            renderBeamRibbon(buffer, matrix4f, frame, frame.crossAxis(), radius * 1.12F, radius * 0.86F, startScale, endScale, color, alpha * 0.24F * config.crossAlphaScale(), true, light);
            return;
        }

        if (shaderCompat) {
            float coreAlpha = boost(config.coreAlpha() * fade, config.shaderCompatCoreGain());
            float glowAlpha = boost(config.glowAlpha() * fade, config.shaderCompatGlowGain());
            renderBeamRibbon(buffer, matrix4f, frame, frame.billboardAxis(), config.glowRadius() * scale, config.glowRadius() * scale * 0.74F, startScale, endScale, config.glowColor(), glowAlpha * 0.58F, true, light);
            renderBeamRibbon(buffer, matrix4f, frame, frame.crossAxis(), config.glowRadius() * scale * 0.72F, config.glowRadius() * scale * 0.52F, startScale, endScale, config.glowColor(), glowAlpha * 0.28F * config.crossAlphaScale(), true, light);
            renderBeamRibbon(buffer, matrix4f, frame, frame.billboardAxis(), config.coreRadius() * scale, config.coreRadius() * scale * 0.82F, startScale, endScale, config.coreColor(), coreAlpha, true, light);
            renderBeamRibbon(buffer, matrix4f, frame, frame.crossAxis(), config.coreRadius() * scale * 0.70F, config.coreRadius() * scale * 0.56F, startScale, endScale, config.coreColor(), coreAlpha * config.crossAlphaScale(), true, light);
            return;
        }

        float coreAlpha = config.coreAlpha() * fade;
        float glowAlpha = config.glowAlpha() * fade;
        renderBeamRibbon(buffer, matrix4f, frame, frame.billboardAxis(), config.glowRadius() * scale, config.glowRadius() * scale * 0.76F, startScale, endScale, config.glowColor(), glowAlpha * 0.72F, false, light);
        renderBeamRibbon(buffer, matrix4f, frame, frame.crossAxis(), config.glowRadius() * scale * 0.72F, config.glowRadius() * scale * 0.54F, startScale, endScale, config.glowColor(), glowAlpha * 0.36F * config.crossAlphaScale(), false, light);
        renderBeamRibbon(buffer, matrix4f, frame, frame.billboardAxis(), config.coreRadius() * scale, config.coreRadius() * scale * 0.82F, startScale, endScale, config.coreColor(), coreAlpha, false, light);
        renderBeamRibbon(buffer, matrix4f, frame, frame.crossAxis(), config.coreRadius() * scale * 0.70F, config.coreRadius() * scale * 0.56F, startScale, endScale, config.coreColor(), coreAlpha * config.crossAlphaScale(), false, light);
    }

    private static BeamFrame buildFrame(VoidBeamInstance beam, Vec3 cameraPos) {
        Vec3 start = beam.start();
        Vec3 end = beam.end();
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < EPSILON) {
            return null;
        }

        direction = direction.normalize();
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = cameraPos.subtract(midpoint);
        Vec3 billboardAxis = direction.cross(toCamera);
        if (billboardAxis.lengthSqr() < EPSILON) {
            billboardAxis = fallbackAxis(direction);
        } else {
            billboardAxis = billboardAxis.normalize();
        }

        Vec3 crossAxis = direction.cross(billboardAxis);
        if (crossAxis.lengthSqr() < EPSILON) {
            crossAxis = fallbackAxis(direction);
        } else {
            crossAxis = crossAxis.normalize();
        }

        return new BeamFrame(
                start.subtract(cameraPos),
                end.subtract(cameraPos),
                billboardAxis,
                crossAxis
        );
    }

    private static Vec3 fallbackAxis(Vec3 direction) {
        Vec3 axis = direction.cross(WORLD_UP);
        if (axis.lengthSqr() < EPSILON) {
            axis = direction.cross(WORLD_RIGHT);
        }
        return axis.lengthSqr() < EPSILON ? WORLD_RIGHT : axis.normalize();
    }

    private static void renderBeamRibbon(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            BeamFrame frame,
            Vec3 axis,
            float startRadius,
            float endRadius,
            float startScale,
            float endScale,
            int color,
            float alpha,
            boolean shaderCompat,
            int light
    ) {
        if (alpha <= 0.01F) {
            return;
        }

        Vec3 startOffset = axis.scale(startRadius * startScale);
        Vec3 endOffset = axis.scale(endRadius * endScale);
        for (int step = 0; step < EDGE_FADE_SEGMENTS; step++) {
            float normalized0 = -1.0F + 2.0F * step / EDGE_FADE_SEGMENTS;
            float normalized1 = -1.0F + 2.0F * (step + 1) / EDGE_FADE_SEGMENTS;
            float fade0 = edgeFadeFactor(normalized0);
            float fade1 = edgeFadeFactor(normalized1);
            if (fade0 <= 0.001F && fade1 <= 0.001F) {
                continue;
            }

            Vec3 p0 = frame.start().add(startOffset.scale(normalized0));
            Vec3 p1 = frame.start().add(startOffset.scale(normalized1));
            Vec3 p2 = frame.end().add(endOffset.scale(normalized1));
            Vec3 p3 = frame.end().add(endOffset.scale(normalized0));
            if (shaderCompat) {
                renderCompatQuadDetailed(buffer, matrix4f, p0, p1, p2, p3, color, alpha * fade0, color, alpha * fade1, color, alpha * fade1, color, alpha * fade0, light);
            } else {
                renderQuadDetailed(buffer, matrix4f, p0, p1, p2, p3, color, alpha * fade0, color, alpha * fade1, color, alpha * fade1, color, alpha * fade0);
            }
        }
    }

    private static float edgeFadeFactor(float normalizedOffset) {
        return 1.0F - smoothstep(EDGE_FADE_RATIO, 1.0F, Math.abs(normalizedOffset));
    }

    private static float smoothstep(float start, float end, float value) {
        if (start == end) {
            return value < start ? 0.0F : 1.0F;
        }
        float t = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void renderQuadDetailed(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 p0,
            Vec3 p1,
            Vec3 p2,
            Vec3 p3,
            int c0,
            float a0,
            int c1,
            float a1,
            int c2,
            float a2,
            int c3,
            float a3
    ) {
        putVertex(buffer, matrix4f, p0, c0, a0);
        putVertex(buffer, matrix4f, p1, c1, a1);
        putVertex(buffer, matrix4f, p2, c2, a2);
        putVertex(buffer, matrix4f, p3, c3, a3);
    }

    private static void renderCompatQuadDetailed(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 p0,
            Vec3 p1,
            Vec3 p2,
            Vec3 p3,
            int c0,
            float a0,
            int c1,
            float a1,
            int c2,
            float a2,
            int c3,
            float a3,
            int light
    ) {
        putCompatVertex(buffer, matrix4f, p0, c0, a0, 0.5F, 0.5F, light);
        putCompatVertex(buffer, matrix4f, p1, c1, a1, 0.5F, 0.5F, light);
        putCompatVertex(buffer, matrix4f, p2, c2, a2, 0.5F, 0.5F, light);
        putCompatVertex(buffer, matrix4f, p3, c3, a3, 0.5F, 0.5F, light);
    }

    private static void putVertex(VertexConsumer buffer, Matrix4f matrix4f, Vec3 position, int color, float alpha) {
        int a = alphaToByte(alpha);
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        buffer.addVertex(matrix4f, (float) position.x, (float) position.y, (float) position.z)
                .setColor(r, g, b, a);
    }

    private static void putCompatVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 position,
            int color,
            float alpha,
            float u,
            float v,
            int light
    ) {
        int a = alphaToByte(alpha);
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        buffer.addVertex(matrix4f, (float) position.x, (float) position.y, (float) position.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }

    private static float boost(float alpha, float gain) {
        return Mth.clamp(alpha * gain, 0.0F, 1.0F);
    }

    private static int towardWhite(int color, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = Mth.lerpInt(t, color >> 16 & 0xFF, 255);
        int g = Mth.lerpInt(t, color >> 8 & 0xFF, 255);
        int b = Mth.lerpInt(t, color & 0xFF, 255);
        return r << 16 | g << 8 | b;
    }

    private record BeamFrame(Vec3 start, Vec3 end, Vec3 billboardAxis, Vec3 crossAxis) {
    }
}
