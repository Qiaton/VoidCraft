package com.example.voidcraft.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class VoidTrailRenderer {
    private VoidTrailRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidTrailInstance trail,
            Vec3 cameraPos,
            float partialTick
    ) {
        List<VoidTrailInstance.TrailPoint> points = trail.getPoints();
        if (points.size() < 2) {
            return;
        }

        Matrix4f matrix4f = poseStack.last().pose();
        int pointCount = points.size();
        for (int i = 0; i < points.size() - 1; i++) {
            VoidTrailInstance.TrailPoint startPoint = points.get(i);
            VoidTrailInstance.TrailPoint endPoint = points.get(i + 1);

            Vec3 start = startPoint.position.subtract(cameraPos);
            Vec3 end = endPoint.position.subtract(cameraPos);
            Vec3 segment = end.subtract(start);
            if (segment.lengthSqr() < 1.0E-8D) {
                continue;
            }

            float startLife = shapedLife(startPoint.getLife(partialTick, trail.preset.lifetimeTicks()));
            float endLife = shapedLife(endPoint.getLife(partialTick, trail.preset.lifetimeTicks()));
            float startAlong = pointCount <= 1 ? 0.0F : (float) i / (pointCount - 1);
            float endAlong = pointCount <= 1 ? 1.0F : (float) (i + 1) / (pointCount - 1);
            float headFadeStart = 1.0F - trail.preset.headFadeRatio();
            float startHeadFade = 1.0F - smoothstep(headFadeStart, 1.0F, startAlong);
            float endHeadFade = 1.0F - smoothstep(headFadeStart, 1.0F, endAlong);
            startLife *= startHeadFade;
            endLife *= endHeadFade;
            if (startLife <= 0.01F && endLife <= 0.01F) {
                continue;
            }

            Vec3 direction = segment.normalize();
            Vec3 midpoint = start.add(end).scale(0.5D);
            Vec3 toCamera = midpoint.scale(-1.0D);
            if (toCamera.lengthSqr() < 1.0E-8D) {
                toCamera = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                toCamera = toCamera.normalize();
            }

            Vec3 side = direction.cross(toCamera);
            if (side.lengthSqr() < 1.0E-8D) {
                side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-8D) {
                side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            side = side.normalize();

            float baseHalfWidth = trail.preset.width() * trail.scale;
            float baseHalfHeight = trail.preset.height() * trail.scale;
            float startHalfWidth = baseHalfWidth * Mth.lerp(startLife, trail.preset.tailWidthScale(), 1.0F);
            float endHalfWidth = baseHalfWidth * Mth.lerp(endLife, trail.preset.tailWidthScale(), 1.0F);
            float startHalfHeight = baseHalfHeight * Mth.lerp(startLife, trail.preset.tailHeightScale(), 1.0F);
            float endHalfHeight = baseHalfHeight * Mth.lerp(endLife, trail.preset.tailHeightScale(), 1.0F);

            int startColor = lerpColor(trail.preset.tailColor(), trail.preset.headColor(), startLife);
            int endColor = lerpColor(trail.preset.tailColor(), trail.preset.headColor(), endLife);
            float startAlpha = trail.preset.alpha() * startLife;
            float endAlpha = trail.preset.alpha() * endLife;
            float startGlowAlpha = trail.preset.glowAlpha() * startLife;
            float endGlowAlpha = trail.preset.glowAlpha() * endLife;

            renderVerticalRibbon(
                    buffer,
                    matrix4f,
                    start,
                    end,
                    startHalfHeight * trail.preset.glowHeightMultiplier(),
                    endHalfHeight * trail.preset.glowHeightMultiplier(),
                    trail.preset.edgeFadeRatio(),
                    startColor,
                    endColor,
                    startGlowAlpha * 0.82F,
                    endGlowAlpha
            );
            renderVerticalRibbon(
                    buffer,
                    matrix4f,
                    start,
                    end,
                    startHalfHeight,
                    endHalfHeight,
                    trail.preset.edgeFadeRatio(),
                    startColor,
                    endColor,
                    startAlpha * 0.82F,
                    endAlpha
            );

            renderSideRibbon(
                    buffer,
                    matrix4f,
                    start,
                    end,
                    side,
                    startHalfWidth * trail.preset.glowWidthMultiplier(),
                    endHalfWidth * trail.preset.glowWidthMultiplier(),
                    trail.preset.edgeFadeRatio(),
                    startColor,
                    endColor,
                    startGlowAlpha * 0.40F,
                    endGlowAlpha * 0.52F
            );
            renderSideRibbon(
                    buffer,
                    matrix4f,
                    start,
                    end,
                    side,
                    startHalfWidth,
                    endHalfWidth,
                    trail.preset.edgeFadeRatio(),
                    startColor,
                    endColor,
                    startAlpha * 0.26F,
                    endAlpha * 0.36F
            );
        }
    }

    private static void renderVerticalRibbon(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 start,
            Vec3 end,
            float startHalfHeight,
            float endHalfHeight,
            float edgeFadeRatio,
            int startColor,
            int endColor,
            float startAlpha,
            float endAlpha
    ) {
        if (startAlpha <= 0.01F && endAlpha <= 0.01F) {
            return;
        }

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        float innerStartHalfHeight = startHalfHeight * edgeFadeRatio;
        float innerEndHalfHeight = endHalfHeight * edgeFadeRatio;
        Vec3 startOffset = up.scale(startHalfHeight);
        Vec3 endOffset = up.scale(endHalfHeight);
        Vec3 startInnerOffset = up.scale(innerStartHalfHeight);
        Vec3 endInnerOffset = up.scale(innerEndHalfHeight);

        renderQuad(
                buffer,
                matrix4f,
                start.subtract(startInnerOffset),
                start.add(startInnerOffset),
                end.add(endInnerOffset),
                end.subtract(endInnerOffset),
                startColor,
                startAlpha,
                startColor,
                startAlpha,
                endColor,
                endAlpha,
                endColor,
                endAlpha
        );
        renderQuad(
                buffer,
                matrix4f,
                start.add(startInnerOffset),
                start.add(startOffset),
                end.add(endOffset),
                end.add(endInnerOffset),
                startColor,
                startAlpha,
                startColor,
                0.0F,
                endColor,
                0.0F,
                endColor,
                endAlpha
        );
        renderQuad(
                buffer,
                matrix4f,
                start.subtract(startOffset),
                start.subtract(startInnerOffset),
                end.subtract(endInnerOffset),
                end.subtract(endOffset),
                startColor,
                0.0F,
                startColor,
                startAlpha,
                endColor,
                endAlpha,
                endColor,
                0.0F
        );
    }

    private static void renderSideRibbon(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 start,
            Vec3 end,
            Vec3 side,
            float startHalfWidth,
            float endHalfWidth,
            float edgeFadeRatio,
            int startColor,
            int endColor,
            float startAlpha,
            float endAlpha
    ) {
        if (startAlpha <= 0.01F && endAlpha <= 0.01F) {
            return;
        }

        float innerStartHalfWidth = startHalfWidth * edgeFadeRatio;
        float innerEndHalfWidth = endHalfWidth * edgeFadeRatio;
        Vec3 startOffset = side.scale(startHalfWidth);
        Vec3 endOffset = side.scale(endHalfWidth);
        Vec3 startInnerOffset = side.scale(innerStartHalfWidth);
        Vec3 endInnerOffset = side.scale(innerEndHalfWidth);

        renderQuad(
                buffer,
                matrix4f,
                start.subtract(startInnerOffset),
                start.add(startInnerOffset),
                end.add(endInnerOffset),
                end.subtract(endInnerOffset),
                startColor,
                startAlpha,
                startColor,
                startAlpha,
                endColor,
                endAlpha,
                endColor,
                endAlpha
        );
        renderQuad(
                buffer,
                matrix4f,
                start.subtract(startOffset),
                start.subtract(startInnerOffset),
                end.subtract(endInnerOffset),
                end.subtract(endOffset),
                startColor,
                0.0F,
                startColor,
                startAlpha,
                endColor,
                endAlpha,
                endColor,
                0.0F
        );
        renderQuad(
                buffer,
                matrix4f,
                start.add(startInnerOffset),
                start.add(startOffset),
                end.add(endOffset),
                end.add(endInnerOffset),
                startColor,
                startAlpha,
                startColor,
                0.0F,
                endColor,
                0.0F,
                endColor,
                endAlpha
        );
    }

    private static float shapedLife(float life) {
        return life * life * (3.0F - 2.0F * life);
    }

    private static float smoothstep(float start, float end, float value) {
        if (start == end) {
            return value < start ? 0.0F : 1.0F;
        }
        float t = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int lerpColor(int startColor, int endColor, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int r = Mth.lerpInt(clamped, startColor >> 16 & 0xFF, endColor >> 16 & 0xFF);
        int g = Mth.lerpInt(clamped, startColor >> 8 & 0xFF, endColor >> 8 & 0xFF);
        int b = Mth.lerpInt(clamped, startColor & 0xFF, endColor & 0xFF);
        return r << 16 | g << 8 | b;
    }

    private static void renderQuad(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 v0,
            Vec3 v1,
            Vec3 v2,
            Vec3 v3,
            int c0,
            float a0,
            int c1,
            float a1,
            int c2,
            float a2,
            int c3,
            float a3
    ) {
        putVertex(buffer, matrix4f, v0, c0, a0);
        putVertex(buffer, matrix4f, v1, c1, a1);
        putVertex(buffer, matrix4f, v2, c2, a2);
        putVertex(buffer, matrix4f, v3, c3, a3);
    }

    private static void putVertex(VertexConsumer buffer, Matrix4f matrix4f, Vec3 pos, int color, float alpha) {
        int a = alphaToByte(alpha);
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        buffer.addVertex(matrix4f, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, a);
    }

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }
}
