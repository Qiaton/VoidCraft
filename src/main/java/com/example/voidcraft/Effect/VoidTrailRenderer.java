package com.example.voidcraft.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class VoidTrailRenderer {
    private static final double EPSILON = 1.0E-8D;
    private static final float SOFT_GLOW_V = 0.5F;
    private static final double WORLD_UP_X = 0.0D;
    private static final double WORLD_UP_Y = 1.0D;
    private static final double WORLD_UP_Z = 0.0D;
    private static final double WORLD_RIGHT_X = 1.0D;
    private static final double WORLD_RIGHT_Y = 0.0D;
    private static final double WORLD_RIGHT_Z = 0.0D;
    private static double[] positionX = new double[0];
    private static double[] positionY = new double[0];
    private static double[] positionZ = new double[0];
    private static double[] sideX = new double[0];
    private static double[] sideY = new double[0];
    private static double[] sideZ = new double[0];
    private static float[] pointLife = new float[0];
    private static float[] pointAlong = new float[0];

    private VoidTrailRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidTrailInstance trail,
            Vec3 cameraPos,
            float partialTick
    ) {
        renderInternal(poseStack, buffer, trail, cameraPos, partialTick, false, false, 0);
    }

    public static void renderShaderCompat(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidTrailInstance trail,
            Vec3 cameraPos,
            float partialTick,
            int light
    ) {
        renderInternal(poseStack, buffer, trail, cameraPos, partialTick, true, false, light);
    }

    public static void renderShaderGlow(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidTrailInstance trail,
            Vec3 cameraPos,
            float partialTick,
            int light
    ) {
        renderInternal(poseStack, buffer, trail, cameraPos, partialTick, true, true, light);
    }

    private static void renderInternal(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidTrailInstance trail,
            Vec3 cameraPos,
            float partialTick,
            boolean shaderCompat,
            boolean additiveGlowPass,
            int light
    ) {
        List<VoidTrailInstance.TrailPoint> points = trail.rawPoints();
        if (points.size() < 2) {
            return;
        }

        Matrix4f matrix4f = poseStack.last().pose();
        int pointCount = buildRenderPoints(points, trail, cameraPos, partialTick);

        boolean hasPreviousSide = false;
        double previousSideX = WORLD_RIGHT_X;
        double previousSideY = WORLD_RIGHT_Y;
        double previousSideZ = WORLD_RIGHT_Z;
        for (int i = 0; i < pointCount; i++) {
            double backwardX = 0.0D;
            double backwardY = 0.0D;
            double backwardZ = 0.0D;
            boolean hasBackward = false;
            if (i > 0) {
                backwardX = positionX[i] - positionX[i - 1];
                backwardY = positionY[i] - positionY[i - 1];
                backwardZ = positionZ[i] - positionZ[i - 1];
                double backwardLengthSqr = backwardX * backwardX + backwardY * backwardY + backwardZ * backwardZ;
                if (backwardLengthSqr >= EPSILON) {
                    double inverseLength = 1.0D / Math.sqrt(backwardLengthSqr);
                    backwardX *= inverseLength;
                    backwardY *= inverseLength;
                    backwardZ *= inverseLength;
                    hasBackward = true;
                }
            }

            double forwardX = 0.0D;
            double forwardY = 0.0D;
            double forwardZ = 0.0D;
            boolean hasForward = false;
            if (i + 1 < pointCount) {
                forwardX = positionX[i + 1] - positionX[i];
                forwardY = positionY[i + 1] - positionY[i];
                forwardZ = positionZ[i + 1] - positionZ[i];
                double forwardLengthSqr = forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ;
                if (forwardLengthSqr >= EPSILON) {
                    double inverseLength = 1.0D / Math.sqrt(forwardLengthSqr);
                    forwardX *= inverseLength;
                    forwardY *= inverseLength;
                    forwardZ *= inverseLength;
                    hasForward = true;
                }
            }

            double tangentX;
            double tangentY;
            double tangentZ;
            if (hasBackward && hasForward) {
                tangentX = backwardX + forwardX;
                tangentY = backwardY + forwardY;
                tangentZ = backwardZ + forwardZ;
                double tangentLengthSqr = tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ;
                if (tangentLengthSqr >= EPSILON) {
                    double inverseLength = 1.0D / Math.sqrt(tangentLengthSqr);
                    tangentX *= inverseLength;
                    tangentY *= inverseLength;
                    tangentZ *= inverseLength;
                } else {
                    tangentX = forwardX;
                    tangentY = forwardY;
                    tangentZ = forwardZ;
                }
            } else if (hasForward) {
                tangentX = forwardX;
                tangentY = forwardY;
                tangentZ = forwardZ;
            } else if (hasBackward) {
                tangentX = backwardX;
                tangentY = backwardY;
                tangentZ = backwardZ;
            } else {
                tangentX = WORLD_RIGHT_X;
                tangentY = WORLD_RIGHT_Y;
                tangentZ = WORLD_RIGHT_Z;
            }

            double currentSideX = -tangentZ;
            double currentSideY = 0.0D;
            double currentSideZ = tangentX;
            double sideLengthSqr = currentSideX * currentSideX + currentSideY * currentSideY + currentSideZ * currentSideZ;
            if (sideLengthSqr < EPSILON && hasPreviousSide) {
                currentSideX = previousSideX;
                currentSideY = previousSideY;
                currentSideZ = previousSideZ;
                sideLengthSqr = 1.0D;
            }
            if (sideLengthSqr < EPSILON) {
                currentSideX = 0.0D;
                currentSideY = tangentZ;
                currentSideZ = -tangentY;
                sideLengthSqr = currentSideY * currentSideY + currentSideZ * currentSideZ;
            }
            if (sideLengthSqr < EPSILON) {
                if (hasPreviousSide) {
                    currentSideX = previousSideX;
                    currentSideY = previousSideY;
                    currentSideZ = previousSideZ;
                } else {
                    currentSideX = WORLD_RIGHT_X;
                    currentSideY = WORLD_RIGHT_Y;
                    currentSideZ = WORLD_RIGHT_Z;
                }
            } else {
                double inverseLength = 1.0D / Math.sqrt(sideLengthSqr);
                currentSideX *= inverseLength;
                currentSideY *= inverseLength;
                currentSideZ *= inverseLength;
            }

            if (hasPreviousSide && currentSideX * previousSideX + currentSideY * previousSideY + currentSideZ * previousSideZ < 0.0D) {
                currentSideX = -currentSideX;
                currentSideY = -currentSideY;
                currentSideZ = -currentSideZ;
            }

            sideX[i] = currentSideX;
            sideY[i] = currentSideY;
            sideZ[i] = currentSideZ;
            previousSideX = currentSideX;
            previousSideY = currentSideY;
            previousSideZ = currentSideZ;
            hasPreviousSide = true;
        }

        for (int i = 0; i < pointCount - 1; i++) {
            double startX = positionX[i];
            double startY = positionY[i];
            double startZ = positionZ[i];
            double endX = positionX[i + 1];
            double endY = positionY[i + 1];
            double endZ = positionZ[i + 1];
            double segmentX = endX - startX;
            double segmentY = endY - startY;
            double segmentZ = endZ - startZ;
            if (segmentX * segmentX + segmentY * segmentY + segmentZ * segmentZ < EPSILON) {
                continue;
            }
            double startSideX = sideX[i];
            double startSideY = sideY[i];
            double startSideZ = sideZ[i];
            double endSideX = sideX[i + 1];
            double endSideY = sideY[i + 1];
            double endSideZ = sideZ[i + 1];

            float startLife = pointLife[i];
            float endLife = pointLife[i + 1];
            float startAlong = pointAlong[i];
            float endAlong = pointAlong[i + 1];
            float headFadeStart = 1.0F - trail.preset.headFadeRatio();
            float startHeadFade = 1.0F - smoothstep(headFadeStart, 1.0F, startAlong);
            float endHeadFade = 1.0F - smoothstep(headFadeStart, 1.0F, endAlong);
            startLife *= startHeadFade;
            endLife *= endHeadFade;
            if (startLife <= 0.01F && endLife <= 0.01F) {
                continue;
            }

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
            float shaderBloomAlphaScale = trail.preset.shaderCompatBloomAlphaScale();
            float shaderBloomWidthScale = trail.preset.shaderCompatBloomWidthScale();
            float shaderBloomHeightScale = trail.preset.shaderCompatBloomHeightScale();

            if (shaderCompat) {
                if (additiveGlowPass) {
                    int bloomStartColor = boostShaderCompatColor(startColor, trail.preset.shaderCompatBloomTailWhiten());
                    int bloomEndColor = boostShaderCompatColor(endColor, trail.preset.shaderCompatBloomHeadWhiten());
                    renderCompatRibbonRaw(
                            buffer,
                            matrix4f,
                            startX,
                            startY,
                            startZ,
                            endX,
                            endY,
                            endZ,
                            WORLD_UP_X,
                            WORLD_UP_Y,
                            WORLD_UP_Z,
                            WORLD_UP_X,
                            WORLD_UP_Y,
                            WORLD_UP_Z,
                            startHalfHeight * trail.preset.glowHeightMultiplier() * 1.34F * shaderBloomHeightScale,
                            endHalfHeight * trail.preset.glowHeightMultiplier() * 1.34F * shaderBloomHeightScale,
                            bloomStartColor,
                            bloomEndColor,
                            boostShaderCompatAlpha(startGlowAlpha * 0.54F * shaderBloomAlphaScale, trail.preset.shaderCompatVerticalGlowGain()),
                            boostShaderCompatAlpha(endGlowAlpha * 0.68F * shaderBloomAlphaScale, trail.preset.shaderCompatVerticalGlowGain()),
                            light
                    );
                    renderCompatRibbonRaw(
                            buffer,
                            matrix4f,
                            startX,
                            startY,
                            startZ,
                            endX,
                            endY,
                            endZ,
                            WORLD_UP_X,
                            WORLD_UP_Y,
                            WORLD_UP_Z,
                            WORLD_UP_X,
                            WORLD_UP_Y,
                            WORLD_UP_Z,
                            startHalfHeight * trail.preset.glowHeightMultiplier() * 1.16F * shaderBloomHeightScale,
                            endHalfHeight * trail.preset.glowHeightMultiplier() * 1.16F * shaderBloomHeightScale,
                            bloomStartColor,
                            bloomEndColor,
                            boostShaderCompatAlpha(startGlowAlpha * 0.38F * shaderBloomAlphaScale, trail.preset.shaderCompatVerticalGlowGain()),
                            boostShaderCompatAlpha(endGlowAlpha * 0.50F * shaderBloomAlphaScale, trail.preset.shaderCompatVerticalGlowGain()),
                            light
                    );
                    renderCompatRibbonRaw(
                            buffer,
                            matrix4f,
                            startX,
                            startY,
                            startZ,
                            endX,
                            endY,
                            endZ,
                            startSideX,
                            startSideY,
                            startSideZ,
                            endSideX,
                            endSideY,
                            endSideZ,
                            startHalfWidth * trail.preset.glowWidthMultiplier() * 1.30F * shaderBloomWidthScale,
                            endHalfWidth * trail.preset.glowWidthMultiplier() * 1.30F * shaderBloomWidthScale,
                            bloomStartColor,
                            bloomEndColor,
                            boostShaderCompatAlpha(startGlowAlpha * 0.34F * shaderBloomAlphaScale, trail.preset.shaderCompatSideGlowGain()),
                            boostShaderCompatAlpha(endGlowAlpha * 0.44F * shaderBloomAlphaScale, trail.preset.shaderCompatSideGlowGain()),
                            light
                    );
                    renderCompatRibbonRaw(
                            buffer,
                            matrix4f,
                            startX,
                            startY,
                            startZ,
                            endX,
                            endY,
                            endZ,
                            startSideX,
                            startSideY,
                            startSideZ,
                            endSideX,
                            endSideY,
                            endSideZ,
                            startHalfWidth * trail.preset.glowWidthMultiplier() * 1.12F * shaderBloomWidthScale,
                            endHalfWidth * trail.preset.glowWidthMultiplier() * 1.12F * shaderBloomWidthScale,
                            bloomStartColor,
                            bloomEndColor,
                            boostShaderCompatAlpha(startGlowAlpha * 0.26F * shaderBloomAlphaScale, trail.preset.shaderCompatSideGlowGain()),
                            boostShaderCompatAlpha(endGlowAlpha * 0.34F * shaderBloomAlphaScale, trail.preset.shaderCompatSideGlowGain()),
                            light
                    );
                    continue;
                }

                renderCompatRibbonWithEdgeFadeRaw(
                        buffer,
                        matrix4f,
                        startX,
                        startY,
                        startZ,
                        endX,
                        endY,
                        endZ,
                        WORLD_UP_X,
                        WORLD_UP_Y,
                        WORLD_UP_Z,
                        WORLD_UP_X,
                        WORLD_UP_Y,
                        WORLD_UP_Z,
                        startHalfHeight * trail.preset.glowHeightMultiplier() * 1.12F,
                        endHalfHeight * trail.preset.glowHeightMultiplier() * 1.12F,
                        trail.preset.edgeFadeRatio(),
                        trail.preset.ribbonFadeSegments(),
                        startColor,
                        endColor,
                        boostShaderCompatAlpha(startGlowAlpha * 0.92F, trail.preset.shaderCompatVerticalGlowGain()),
                        boostShaderCompatAlpha(endGlowAlpha * 1.08F, trail.preset.shaderCompatVerticalGlowGain()),
                        light
                );
                renderCompatRibbonWithEdgeFadeRaw(
                        buffer,
                        matrix4f,
                        startX,
                        startY,
                        startZ,
                        endX,
                        endY,
                        endZ,
                        WORLD_UP_X,
                        WORLD_UP_Y,
                        WORLD_UP_Z,
                        WORLD_UP_X,
                        WORLD_UP_Y,
                        WORLD_UP_Z,
                        startHalfHeight * trail.preset.glowHeightMultiplier(),
                        endHalfHeight * trail.preset.glowHeightMultiplier(),
                        trail.preset.edgeFadeRatio(),
                        trail.preset.ribbonFadeSegments(),
                        startColor,
                        endColor,
                        boostShaderCompatAlpha(startGlowAlpha * 0.86F, trail.preset.shaderCompatVerticalGlowGain()),
                        boostShaderCompatAlpha(endGlowAlpha, trail.preset.shaderCompatVerticalGlowGain()),
                        light
                );
                renderCompatRibbonWithEdgeFadeRaw(
                        buffer,
                        matrix4f,
                        startX,
                        startY,
                        startZ,
                        endX,
                        endY,
                        endZ,
                        WORLD_UP_X,
                        WORLD_UP_Y,
                        WORLD_UP_Z,
                        WORLD_UP_X,
                        WORLD_UP_Y,
                        WORLD_UP_Z,
                        startHalfHeight,
                        endHalfHeight,
                        trail.preset.edgeFadeRatio(),
                        trail.preset.ribbonFadeSegments(),
                        startColor,
                        endColor,
                        boostShaderCompatAlpha(startAlpha * 0.82F, trail.preset.shaderCompatVerticalCoreGain()),
                        boostShaderCompatAlpha(endAlpha, trail.preset.shaderCompatVerticalCoreGain()),
                        light
                );
                renderCompatRibbonWithEdgeFadeRaw(
                        buffer,
                        matrix4f,
                        startX,
                        startY,
                        startZ,
                        endX,
                        endY,
                        endZ,
                        startSideX,
                        startSideY,
                        startSideZ,
                        endSideX,
                        endSideY,
                        endSideZ,
                        startHalfWidth * trail.preset.glowWidthMultiplier() * 1.08F,
                        endHalfWidth * trail.preset.glowWidthMultiplier() * 1.08F,
                        trail.preset.edgeFadeRatio(),
                        trail.preset.ribbonFadeSegments(),
                        startColor,
                        endColor,
                        boostShaderCompatAlpha(startGlowAlpha * 0.52F, trail.preset.shaderCompatSideGlowGain()),
                        boostShaderCompatAlpha(endGlowAlpha * 0.68F, trail.preset.shaderCompatSideGlowGain()),
                        light
                );
                renderCompatRibbonWithEdgeFadeRaw(
                        buffer,
                        matrix4f,
                        startX,
                        startY,
                        startZ,
                        endX,
                        endY,
                        endZ,
                        startSideX,
                        startSideY,
                        startSideZ,
                        endSideX,
                        endSideY,
                        endSideZ,
                        startHalfWidth * trail.preset.glowWidthMultiplier(),
                        endHalfWidth * trail.preset.glowWidthMultiplier(),
                        trail.preset.edgeFadeRatio(),
                        trail.preset.ribbonFadeSegments(),
                        startColor,
                        endColor,
                        boostShaderCompatAlpha(startGlowAlpha * 0.44F, trail.preset.shaderCompatSideGlowGain()),
                        boostShaderCompatAlpha(endGlowAlpha * 0.56F, trail.preset.shaderCompatSideGlowGain()),
                        light
                );
                renderCompatRibbonWithEdgeFadeRaw(
                        buffer,
                        matrix4f,
                        startX,
                        startY,
                        startZ,
                        endX,
                        endY,
                        endZ,
                        startSideX,
                        startSideY,
                        startSideZ,
                        endSideX,
                        endSideY,
                        endSideZ,
                        startHalfWidth,
                        endHalfWidth,
                        trail.preset.edgeFadeRatio(),
                        trail.preset.ribbonFadeSegments(),
                        startColor,
                        endColor,
                        boostShaderCompatAlpha(startAlpha * 0.30F, trail.preset.shaderCompatSideCoreGain()),
                        boostShaderCompatAlpha(endAlpha * 0.40F, trail.preset.shaderCompatSideCoreGain()),
                        light
                );
                continue;
            }

            renderRibbonWithEdgeFadeRaw(
                    buffer,
                    matrix4f,
                    startX,
                    startY,
                    startZ,
                    endX,
                    endY,
                    endZ,
                    WORLD_UP_X,
                    WORLD_UP_Y,
                    WORLD_UP_Z,
                    WORLD_UP_X,
                    WORLD_UP_Y,
                    WORLD_UP_Z,
                    startHalfHeight * trail.preset.glowHeightMultiplier(),
                    endHalfHeight * trail.preset.glowHeightMultiplier(),
                    trail.preset.edgeFadeRatio(),
                    trail.preset.ribbonFadeSegments(),
                    startColor,
                    endColor,
                    startGlowAlpha * 0.82F,
                    endGlowAlpha
            );
            renderRibbonWithEdgeFadeRaw(
                    buffer,
                    matrix4f,
                    startX,
                    startY,
                    startZ,
                    endX,
                    endY,
                    endZ,
                    WORLD_UP_X,
                    WORLD_UP_Y,
                    WORLD_UP_Z,
                    WORLD_UP_X,
                    WORLD_UP_Y,
                    WORLD_UP_Z,
                    startHalfHeight,
                    endHalfHeight,
                    trail.preset.edgeFadeRatio(),
                    trail.preset.ribbonFadeSegments(),
                    startColor,
                    endColor,
                    startAlpha * 0.82F,
                    endAlpha
            );

            renderRibbonWithEdgeFadeRaw(
                    buffer,
                    matrix4f,
                    startX,
                    startY,
                    startZ,
                    endX,
                    endY,
                    endZ,
                    startSideX,
                    startSideY,
                    startSideZ,
                    endSideX,
                    endSideY,
                    endSideZ,
                    startHalfWidth * trail.preset.glowWidthMultiplier(),
                    endHalfWidth * trail.preset.glowWidthMultiplier(),
                    trail.preset.edgeFadeRatio(),
                    trail.preset.ribbonFadeSegments(),
                    startColor,
                    endColor,
                    startGlowAlpha * 0.40F,
                    endGlowAlpha * 0.52F
            );
            renderRibbonWithEdgeFadeRaw(
                    buffer,
                    matrix4f,
                    startX,
                    startY,
                    startZ,
                    endX,
                    endY,
                    endZ,
                    startSideX,
                    startSideY,
                    startSideZ,
                    endSideX,
                    endSideY,
                    endSideZ,
                    startHalfWidth,
                    endHalfWidth,
                    trail.preset.edgeFadeRatio(),
                    trail.preset.ribbonFadeSegments(),
                    startColor,
                    endColor,
                    startAlpha * 0.26F,
                    endAlpha * 0.36F
            );
        }
    }

    private static void renderCompatRibbonRaw(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double startAxisX,
            double startAxisY,
            double startAxisZ,
            double endAxisX,
            double endAxisY,
            double endAxisZ,
            float startHalfWidth,
            float endHalfWidth,
            int startColor,
            int endColor,
            float startAlpha,
            float endAlpha,
            int light
    ) {
        if (startAlpha <= 0.01F && endAlpha <= 0.01F) {
            return;
        }

        double startOffsetX = startAxisX * startHalfWidth;
        double startOffsetY = startAxisY * startHalfWidth;
        double startOffsetZ = startAxisZ * startHalfWidth;
        double endOffsetX = endAxisX * endHalfWidth;
        double endOffsetY = endAxisY * endHalfWidth;
        double endOffsetZ = endAxisZ * endHalfWidth;
        renderCompatQuad(
                buffer,
                matrix4f,
                startX - startOffsetX,
                startY - startOffsetY,
                startZ - startOffsetZ,
                startX + startOffsetX,
                startY + startOffsetY,
                startZ + startOffsetZ,
                endX + endOffsetX,
                endY + endOffsetY,
                endZ + endOffsetZ,
                endX - endOffsetX,
                endY - endOffsetY,
                endZ - endOffsetZ,
                startColor,
                startAlpha,
                endColor,
                endAlpha,
                light
        );
    }

    private static int buildRenderPoints(
            List<VoidTrailInstance.TrailPoint> points,
            VoidTrailInstance trail,
            Vec3 cameraPos,
            float partialTick
    ) {
        int pointCount = points.size();
        ensureScratchCapacity(pointCount);

        int lifetimeTicks = trail.preset.lifetimeTicks();
        int currentTick = trail.currentTick();
        int lastPointIndex = pointCount - 1;
        for (int i = 0; i < pointCount; i++) {
            VoidTrailInstance.TrailPoint point = points.get(i);
            positionX[i] = point.position.x - cameraPos.x;
            positionY[i] = point.position.y - cameraPos.y;
            positionZ[i] = point.position.z - cameraPos.z;
            pointLife[i] = shapedLife(point.getLife(partialTick, lifetimeTicks, currentTick));
            pointAlong[i] = (float) i / lastPointIndex;
        }

        return pointCount;
    }

    private static void ensureScratchCapacity(int pointCount) {
        if (positionX.length >= pointCount) {
            return;
        }

        int newSize = Math.max(pointCount, Math.max(8, positionX.length * 2));
        positionX = new double[newSize];
        positionY = new double[newSize];
        positionZ = new double[newSize];
        sideX = new double[newSize];
        sideY = new double[newSize];
        sideZ = new double[newSize];
        pointLife = new float[newSize];
        pointAlong = new float[newSize];
    }

    private static void renderRibbonWithEdgeFadeRaw(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double startAxisX,
            double startAxisY,
            double startAxisZ,
            double endAxisX,
            double endAxisY,
            double endAxisZ,
            float startHalfWidth,
            float endHalfWidth,
            float edgeFadeRatio,
            int ribbonFadeSegments,
            int startColor,
            int endColor,
            float startAlpha,
            float endAlpha
    ) {
        if (startAlpha <= 0.01F && endAlpha <= 0.01F) {
            return;
        }

        double startOffsetX = startAxisX * startHalfWidth;
        double startOffsetY = startAxisY * startHalfWidth;
        double startOffsetZ = startAxisZ * startHalfWidth;
        double endOffsetX = endAxisX * endHalfWidth;
        double endOffsetY = endAxisY * endHalfWidth;
        double endOffsetZ = endAxisZ * endHalfWidth;

        for (int step = 0; step < ribbonFadeSegments; step++) {
            float normalized0 = -1.0F + 2.0F * step / ribbonFadeSegments;
            float normalized1 = -1.0F + 2.0F * (step + 1) / ribbonFadeSegments;
            float fade0 = edgeFadeFactor(normalized0, edgeFadeRatio);
            float fade1 = edgeFadeFactor(normalized1, edgeFadeRatio);
            if (fade0 <= 0.001F && fade1 <= 0.001F) {
                continue;
            }

            renderQuad(
                    buffer,
                    matrix4f,
                    startX + startOffsetX * normalized0,
                    startY + startOffsetY * normalized0,
                    startZ + startOffsetZ * normalized0,
                    startX + startOffsetX * normalized1,
                    startY + startOffsetY * normalized1,
                    startZ + startOffsetZ * normalized1,
                    endX + endOffsetX * normalized1,
                    endY + endOffsetY * normalized1,
                    endZ + endOffsetZ * normalized1,
                    endX + endOffsetX * normalized0,
                    endY + endOffsetY * normalized0,
                    endZ + endOffsetZ * normalized0,
                    startColor,
                    startAlpha * fade0,
                    startColor,
                    startAlpha * fade1,
                    endColor,
                    endAlpha * fade1,
                    endColor,
                    endAlpha * fade0
            );
        }
    }

    private static void renderCompatRibbonWithEdgeFadeRaw(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double startAxisX,
            double startAxisY,
            double startAxisZ,
            double endAxisX,
            double endAxisY,
            double endAxisZ,
            float startHalfWidth,
            float endHalfWidth,
            float edgeFadeRatio,
            int ribbonFadeSegments,
            int startColor,
            int endColor,
            float startAlpha,
            float endAlpha,
            int light
    ) {
        if (startAlpha <= 0.01F && endAlpha <= 0.01F) {
            return;
        }

        double startOffsetX = startAxisX * startHalfWidth;
        double startOffsetY = startAxisY * startHalfWidth;
        double startOffsetZ = startAxisZ * startHalfWidth;
        double endOffsetX = endAxisX * endHalfWidth;
        double endOffsetY = endAxisY * endHalfWidth;
        double endOffsetZ = endAxisZ * endHalfWidth;

        for (int step = 0; step < ribbonFadeSegments; step++) {
            float normalized0 = -1.0F + 2.0F * step / ribbonFadeSegments;
            float normalized1 = -1.0F + 2.0F * (step + 1) / ribbonFadeSegments;
            float fade0 = edgeFadeFactor(normalized0, edgeFadeRatio);
            float fade1 = edgeFadeFactor(normalized1, edgeFadeRatio);
            if (fade0 <= 0.001F && fade1 <= 0.001F) {
                continue;
            }

            renderCompatQuadDetailed(
                    buffer,
                    matrix4f,
                    startX + startOffsetX * normalized0,
                    startY + startOffsetY * normalized0,
                    startZ + startOffsetZ * normalized0,
                    startX + startOffsetX * normalized1,
                    startY + startOffsetY * normalized1,
                    startZ + startOffsetZ * normalized1,
                    endX + endOffsetX * normalized1,
                    endY + endOffsetY * normalized1,
                    endZ + endOffsetZ * normalized1,
                    endX + endOffsetX * normalized0,
                    endY + endOffsetY * normalized0,
                    endZ + endOffsetZ * normalized0,
                    startColor,
                    startAlpha * fade0,
                    startColor,
                    startAlpha * fade1,
                    endColor,
                    endAlpha * fade1,
                    endColor,
                    endAlpha * fade0,
                    light
            );
        }
    }

    private static float edgeFadeFactor(float normalizedOffset, float edgeFadeRatio) {
        return 1.0F - smoothstep(edgeFadeRatio, 1.0F, Math.abs(normalizedOffset));
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
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            int c0,
            float a0,
            int c1,
            float a1,
            int c2,
            float a2,
            int c3,
            float a3
    ) {
        putVertex(buffer, matrix4f, x0, y0, z0, c0, a0);
        putVertex(buffer, matrix4f, x1, y1, z1, c1, a1);
        putVertex(buffer, matrix4f, x2, y2, z2, c2, a2);
        putVertex(buffer, matrix4f, x3, y3, z3, c3, a3);
    }

    private static void renderCompatQuad(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            int startColor,
            float startAlpha,
            int endColor,
            float endAlpha,
            int light
    ) {
        putCompatVertex(buffer, matrix4f, x0, y0, z0, startColor, startAlpha, 0.0F, SOFT_GLOW_V, light);
        putCompatVertex(buffer, matrix4f, x1, y1, z1, startColor, startAlpha, 1.0F, SOFT_GLOW_V, light);
        putCompatVertex(buffer, matrix4f, x2, y2, z2, endColor, endAlpha, 1.0F, SOFT_GLOW_V, light);
        putCompatVertex(buffer, matrix4f, x3, y3, z3, endColor, endAlpha, 0.0F, SOFT_GLOW_V, light);
    }

    private static void renderCompatQuadDetailed(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
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
        putCompatVertex(buffer, matrix4f, x0, y0, z0, c0, a0, 0.5F, 0.5F, light);
        putCompatVertex(buffer, matrix4f, x1, y1, z1, c1, a1, 0.5F, 0.5F, light);
        putCompatVertex(buffer, matrix4f, x2, y2, z2, c2, a2, 0.5F, 0.5F, light);
        putCompatVertex(buffer, matrix4f, x3, y3, z3, c3, a3, 0.5F, 0.5F, light);
    }

    private static void putVertex(VertexConsumer buffer, Matrix4f matrix4f, double x, double y, double z, int color, float alpha) {
        int a = alphaToByte(alpha);
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        buffer.addVertex(matrix4f, (float) x, (float) y, (float) z)
                .setColor(r, g, b, a);
    }

    private static void putCompatVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            double x,
            double y,
            double z,
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
        buffer.addVertex(matrix4f, (float) x, (float) y, (float) z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }

    private static float boostShaderCompatAlpha(float alpha, float gain) {
        return Mth.clamp(alpha * gain, 0.0F, 1.0F);
    }

    private static int boostShaderCompatColor(int color, float towardWhite) {
        float t = Mth.clamp(towardWhite, 0.0F, 1.0F);
        int r = Mth.lerpInt(t, color >> 16 & 0xFF, 255);
        int g = Mth.lerpInt(t, color >> 8 & 0xFF, 255);
        int b = Mth.lerpInt(t, color & 0xFF, 255);
        return r << 16 | g << 8 | b;
    }
}
