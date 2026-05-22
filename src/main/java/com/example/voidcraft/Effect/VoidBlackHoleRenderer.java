package com.example.voidcraft.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VoidBlackHoleRenderer {
    private static final int ANGLE_SEGMENTS = 64;
    private static final int GATE_RADIAL_SEGMENTS = 28;
    private static final int GATE_EDGE_SEGMENTS = 4;
    private static final int MASK_RADIAL_SEGMENTS = 10;
    private static final int HORIZONTAL_SEGMENTS = 32;
    private static final int VERTICAL_SEGMENTS = 16;
    private static final double BILLBOARD_EPSILON = 1.0E-8D;
    private static final double SCREEN_MASK_VIEW_MARGIN = 0.08D;

    private VoidBlackHoleRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBlackHoleInstance blackHole,
            float partialTick,
            FacingData coreFacingData,
            FacingData viewFacingData
    ) {
        RenderMetrics metrics = computeMetrics(blackHole, partialTick);
        renderBlackHole(
                buffer,
                facingMatrix(poseStack, coreFacingData, blackHole.config.coreFollowCameraPitch()),
                facingMatrix(poseStack, viewFacingData, true),
                blackHole.config,
                metrics,
                0,
                false,
                false,
                true
        );
    }

    public static void renderShaderCompat(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBlackHoleInstance blackHole,
            float partialTick,
            int light,
            FacingData coreFacingData,
            FacingData viewFacingData
    ) {
        RenderMetrics metrics = computeMetrics(blackHole, partialTick);
        renderBlackHole(
                buffer,
                facingMatrix(poseStack, coreFacingData, blackHole.config.coreFollowCameraPitch()),
                facingMatrix(poseStack, viewFacingData, true),
                blackHole.config,
                metrics,
                light,
                true,
                false,
                true
        );
    }

    public static void renderShaderGlow(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBlackHoleInstance blackHole,
            float partialTick,
            int light,
            FacingData coreFacingData,
            FacingData viewFacingData
    ) {
        RenderMetrics metrics = computeMetrics(blackHole, partialTick);
        renderBlackHole(
                buffer,
                facingMatrix(poseStack, coreFacingData, blackHole.config.coreFollowCameraPitch()),
                facingMatrix(poseStack, viewFacingData, true),
                blackHole.config,
                metrics,
                light,
                true,
                true,
                false
        );
    }

    public static void renderMask(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidBlackHoleInstance blackHole,
            float partialTick,
            int effectIndex
    ) {
        RenderMetrics metrics = computeMetrics(blackHole, partialTick);
        float halfHeight = metrics.halfHeight() * blackHole.config.distortionHeightScale();
        float halfWidth = metrics.halfWidth() * blackHole.config.distortionWidthScale();
        if (halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        renderMaskLayer(buffer, poseStack.last().pose(), halfHeight, halfWidth, -0.003F, effectIndex);
    }

    public static ScreenMaskData computeScreenMaskData(
            Minecraft mc,
            VoidBlackHoleInstance blackHole,
            Vec3 center,
            float partialTick,
            FacingData facingData
    ) {
        RenderMetrics metrics = computeMetrics(blackHole, partialTick);
        float halfHeight = metrics.halfHeight() * blackHole.config.distortionHeightScale();
        float halfWidth = metrics.halfWidth() * blackHole.config.distortionWidthScale();
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
        if (!arePointsOk(centerNdc, leftNdc, rightNdc, downNdc, upNdc)
                || !hasScreenDepth(centerNdc, leftNdc, rightNdc, downNdc, upNdc)
                || !isScreenBoxOnScreen(centerNdc, leftNdc, rightNdc, downNdc, upNdc)) {
            return null;
        }

        float centerU = (float) (centerNdc.x * 0.5D + 0.5D);
        float centerV = (float) (centerNdc.y * 0.5D + 0.5D);
        float halfWidthU = (float) (Math.max(Math.abs(rightNdc.x - centerNdc.x), Math.abs(leftNdc.x - centerNdc.x)) * 0.5D);
        float halfHeightV = (float) (Math.max(Math.abs(upNdc.y - centerNdc.y), Math.abs(downNdc.y - centerNdc.y)) * 0.5D);
        if (halfWidthU <= 0.0001F || halfHeightV <= 0.0001F) {
            return null;
        }
        float rightAxisU = (float) ((rightNdc.x - leftNdc.x) * 0.25D);
        float rightAxisV = (float) ((rightNdc.y - leftNdc.y) * 0.25D);
        float upAxisU = (float) ((upNdc.x - downNdc.x) * 0.25D);
        float upAxisV = (float) ((upNdc.y - downNdc.y) * 0.25D);
        if (!areScreenAxesOk(rightAxisU, rightAxisV, upAxisU, upAxisV)) {
            return null;
        }

        float centerDepth = Mth.clamp((float) (centerNdc.z * 0.5D + 0.5D), 0.0F, 1.0F);
        return new ScreenMaskData(
                Mth.clamp(centerU, 0.0F, 1.0F),
                Mth.clamp(centerV, 0.0F, 1.0F),
                Mth.clamp(halfWidthU, 0.0F, 1.0F),
                Mth.clamp(halfHeightV, 0.0F, 1.0F),
                rightAxisU,
                rightAxisV,
                upAxisU,
                upAxisV,
                centerDepth
        );
    }

    public static void applyDistortionFacingRotation(PoseStack poseStack, VoidBlackHoleInstance.Config config, FacingData facingData) {
        applyFacingRotation(poseStack, facingData, config.distortionFollowCameraPitch());
    }

    public static FacingData computeCoreFacingData(VoidBlackHoleInstance.Config config, Vec3 center, Vec3 cameraPos) {
        if (!config.coreFollowCameraPitch()) {
            return fixedFacingData(config.coreYaw());
        }
        return computeFacingData(center, cameraPos, config.coreFollowCameraPitch());
    }

    public static FacingData computeDistortionFacingData(VoidBlackHoleInstance.Config config, Vec3 center, Vec3 cameraPos) {
        return computeFacingData(center, cameraPos, config.distortionFollowCameraPitch());
    }

    public static FacingData computeViewFacingData(Vec3 center, Vec3 cameraPos) {
        return computeFacingData(center, cameraPos, true);
    }

    private static Matrix4f facingMatrix(
            PoseStack poseStack,
            FacingData facingData,
            boolean followPitch
    ) {
        poseStack.pushPose();
        applyFacingRotation(poseStack, facingData, followPitch);
        Matrix4f matrix4f = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();
        return matrix4f;
    }

    private static void applyFacingRotation(PoseStack poseStack, FacingData facingData, boolean followPitch) {
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(facingData.yaw()));
        if (followPitch) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(facingData.pitch()));
        }
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

    private static FacingData fixedFacingData(float yaw) {
        return new FacingData(
                yaw,
                0.0F,
                new Vec3(Mth.sin(yaw), 0.0D, Mth.cos(yaw)),
                new Vec3(Mth.cos(yaw), 0.0D, -Mth.sin(yaw)),
                new Vec3(0.0D, 1.0D, 0.0D)
        );
    }

    private static RenderMetrics computeMetrics(VoidBlackHoleInstance blackHole, float partialTick) {
        VoidBlackHoleInstance.Config config = blackHole.config;
        float progress = blackHole.getProgress(partialTick);
        float peakHoldRatio = Mth.clamp((float) config.peakHoldTicks() / Math.max(1.0F, config.durationTicks()), 0.0F, 0.70F);
        float expandEnd = 0.18F;
        float collapseStart = Mth.clamp(expandEnd + peakHoldRatio, expandEnd, 0.98F);
        float expand = smoothstep(0.0F, expandEnd, progress);
        float collapseTimeline = normalizedProgress(collapseStart, 1.0F, progress);
        float collapse = smoothstep(0.0F, 1.0F, collapseTimeline);
        float fade = 1.0F - smoothstep(0.60F, 1.0F, collapseTimeline);

        float burstHeight = Mth.lerp(expand, config.startHalfHeight(), config.peakHalfHeight());
        float burstWidth = Mth.lerp(expand, config.startHalfWidth(), config.peakHalfWidth());
        float halfHeight = Mth.lerp(collapse, burstHeight, config.endHalfHeight()) * blackHole.scale;
        float halfWidth = Mth.lerp(collapse, burstWidth, config.endHalfWidth()) * blackHole.scale;
        return new RenderMetrics(halfHeight, halfWidth, fade, blackHole.age + partialTick);
    }

    private static void renderBlackHole(
            VertexConsumer buffer,
            Matrix4f coreMatrix4f,
            Matrix4f viewMatrix4f,
            VoidBlackHoleInstance.Config config,
            RenderMetrics metrics,
            int light,
            boolean textured,
            boolean glowOnly,
            boolean renderRim
    ) {
        float fade = metrics.fade();
        if (fade <= 0.01F || metrics.halfHeight() <= 0.001F || metrics.halfWidth() <= 0.001F) {
            return;
        }

        float coreAlpha = config.coreAlpha() * fade;
        float rimAlpha = config.rimAlpha() * fade;
        if (config.flatGate()) {
            renderFlatBlackHole(buffer, coreMatrix4f, config, metrics, light, textured, glowOnly, renderRim, coreAlpha, rimAlpha);
            return;
        }

        if (!glowOnly) {
            renderHorizon(buffer, viewMatrix4f, metrics, coreAlpha * config.horizonAlphaScale(), config, light, textured);
            renderSphereLayer(
                    buffer,
                    coreMatrix4f,
                    metrics.halfHeight(),
                    metrics.halfWidth(),
                    coreAlpha * config.coreAlphaScale(),
                    config.coreColor(),
                    light,
                    textured,
                    false
            );
        }

        if (renderRim) {
            renderSphereLayer(
                    buffer,
                    coreMatrix4f,
                    metrics.halfHeight() * 1.08F,
                    metrics.halfWidth() * 1.08F,
                    rimAlpha * (glowOnly ? config.shaderRimAlphaScale() : config.rimAlphaScale()),
                    config.color(),
                    light,
                    textured,
                    true
            );
        }
    }

    private static void renderFlatBlackHole(
            VertexConsumer buffer,
            Matrix4f coreMatrix4f,
            VoidBlackHoleInstance.Config config,
            RenderMetrics metrics,
            int light,
            boolean textured,
            boolean glowOnly,
            boolean renderRim,
            float coreAlpha,
            float rimAlpha
    ) {
        if (!glowOnly) {
            renderGateCenter(buffer, coreMatrix4f, metrics, coreAlpha * config.horizonAlphaScale(), config, light, textured);
        }

        if (renderRim) {
            renderGateEdge(
                    buffer,
                    coreMatrix4f,
                    metrics,
                    rimAlpha * (glowOnly ? config.shaderRimAlphaScale() : config.rimAlphaScale()),
                    config,
                    light,
                    textured
            );
        }
    }

    private static void renderHorizon(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            RenderMetrics metrics,
            float alpha,
            VoidBlackHoleInstance.Config config,
            int light,
            boolean textured
    ) {
        if (alpha <= 0.01F || metrics.halfHeight() <= 0.001F || metrics.halfWidth() <= 0.001F) {
            return;
        }

        int r = colorRed(config.coreColor());
        int g = colorGreen(config.coreColor());
        int b = colorBlue(config.coreColor());
        for (int radial = 0; radial < 20; radial++) {
            float radius0 = (float) radial / 20.0F;
            float radius1 = (float) (radial + 1) / 20.0F;
            int alpha0 = horizonAlpha(alpha, radius0);
            int alpha1 = horizonAlpha(alpha, radius1);

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

                putBlackHoleVertex(buffer, matrix4f, localX00 * metrics.halfWidth(), localY00 * metrics.halfHeight(), 0.018F,
                        r, g, b, alpha0, localXToUv(localX00), localYToUv(localY00), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX01 * metrics.halfWidth(), localY01 * metrics.halfHeight(), 0.018F,
                        r, g, b, alpha0, localXToUv(localX01), localYToUv(localY01), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX11 * metrics.halfWidth(), localY11 * metrics.halfHeight(), 0.018F,
                        r, g, b, alpha1, localXToUv(localX11), localYToUv(localY11), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX10 * metrics.halfWidth(), localY10 * metrics.halfHeight(), 0.018F,
                        r, g, b, alpha1, localXToUv(localX10), localYToUv(localY10), light, textured);
            }
        }
    }

    private static void renderSphereLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float halfHeight,
            float halfWidth,
            float alpha,
            int color,
            int light,
            boolean textured,
            boolean rim
    ) {
        if (alpha <= 0.01F || halfHeight <= 0.001F || halfWidth <= 0.001F) {
            return;
        }

        int r = colorRed(color);
        int g = colorGreen(color);
        int b = colorBlue(color);
        for (int vertical = 0; vertical < VERTICAL_SEGMENTS; vertical++) {
            float v0 = (float) vertical / VERTICAL_SEGMENTS;
            float v1 = (float) (vertical + 1) / VERTICAL_SEGMENTS;
            float phi0 = Mth.PI * v0;
            float phi1 = Mth.PI * v1;
            float shellRadius0 = Mth.sin(phi0);
            float shellRadius1 = Mth.sin(phi1);
            float y0 = Mth.cos(phi0) * halfHeight;
            float y1 = Mth.cos(phi1) * halfHeight;

            for (int horizontal = 0; horizontal < HORIZONTAL_SEGMENTS; horizontal++) {
                float u0 = (float) horizontal / HORIZONTAL_SEGMENTS;
                float u1 = (float) (horizontal + 1) / HORIZONTAL_SEGMENTS;
                float theta0 = Mth.TWO_PI * u0;
                float theta1 = Mth.TWO_PI * u1;
                float x00 = Mth.cos(theta0) * halfWidth * shellRadius0;
                float z00 = Mth.sin(theta0) * halfWidth * shellRadius0;
                float x01 = Mth.cos(theta1) * halfWidth * shellRadius0;
                float z01 = Mth.sin(theta1) * halfWidth * shellRadius0;
                float x11 = Mth.cos(theta1) * halfWidth * shellRadius1;
                float z11 = Mth.sin(theta1) * halfWidth * shellRadius1;
                float x10 = Mth.cos(theta0) * halfWidth * shellRadius1;
                float z10 = Mth.sin(theta0) * halfWidth * shellRadius1;

                putBlackHoleVertex(buffer, matrix4f, x00, y0, z00, r, g, b,
                        blackHoleAlpha(alpha, x00, y0, z00, halfWidth, halfHeight, rim), u0, v0, light, textured);
                putBlackHoleVertex(buffer, matrix4f, x01, y0, z01, r, g, b,
                        blackHoleAlpha(alpha, x01, y0, z01, halfWidth, halfHeight, rim), u1, v0, light, textured);
                putBlackHoleVertex(buffer, matrix4f, x11, y1, z11, r, g, b,
                        blackHoleAlpha(alpha, x11, y1, z11, halfWidth, halfHeight, rim), u1, v1, light, textured);
                putBlackHoleVertex(buffer, matrix4f, x10, y1, z10, r, g, b,
                        blackHoleAlpha(alpha, x10, y1, z10, halfWidth, halfHeight, rim), u0, v1, light, textured);
            }
        }
    }

    private static void renderGateCenter(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            RenderMetrics metrics,
            float alpha,
            VoidBlackHoleInstance.Config config,
            int light,
            boolean textured
    ) {
        if (alpha <= 0.01F || metrics.halfHeight() <= 0.001F || metrics.halfWidth() <= 0.001F) {
            return;
        }

        int r = colorRed(config.coreColor());
        int g = colorGreen(config.coreColor());
        int b = colorBlue(config.coreColor());
        float z = config.flatGate() ? 0.006F : 0.018F;
        for (int radial = 0; radial < GATE_RADIAL_SEGMENTS; radial++) {
            float radius0 = (float) radial / GATE_RADIAL_SEGMENTS;
            float radius1 = (float) (radial + 1) / GATE_RADIAL_SEGMENTS;
            int alpha0 = gateCenterAlpha(alpha, radius0, config.flatGate());
            int alpha1 = gateCenterAlpha(alpha, radius1, config.flatGate());

            for (int angle = 0; angle < ANGLE_SEGMENTS; angle++) {
                float theta0 = Mth.TWO_PI * angle / ANGLE_SEGMENTS;
                float theta1 = Mth.TWO_PI * (angle + 1) / ANGLE_SEGMENTS;
                float radius00 = radius0 * gateEdge(theta0, metrics.time(), config.flatGate());
                float radius01 = radius0 * gateEdge(theta1, metrics.time(), config.flatGate());
                float radius11 = radius1 * gateEdge(theta1, metrics.time(), config.flatGate());
                float radius10 = radius1 * gateEdge(theta0, metrics.time(), config.flatGate());
                float localX00 = Mth.cos(theta0) * radius00;
                float localY00 = Mth.sin(theta0) * radius00;
                float localX01 = Mth.cos(theta1) * radius01;
                float localY01 = Mth.sin(theta1) * radius01;
                float localX11 = Mth.cos(theta1) * radius11;
                float localY11 = Mth.sin(theta1) * radius11;
                float localX10 = Mth.cos(theta0) * radius10;
                float localY10 = Mth.sin(theta0) * radius10;

                putBlackHoleVertex(buffer, matrix4f, localX00 * metrics.halfWidth(), localY00 * metrics.halfHeight(), z,
                        r, g, b, alpha0, localXToUv(localX00), localYToUv(localY00), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX01 * metrics.halfWidth(), localY01 * metrics.halfHeight(), z,
                        r, g, b, alpha0, localXToUv(localX01), localYToUv(localY01), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX11 * metrics.halfWidth(), localY11 * metrics.halfHeight(), z,
                        r, g, b, alpha1, localXToUv(localX11), localYToUv(localY11), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX10 * metrics.halfWidth(), localY10 * metrics.halfHeight(), z,
                        r, g, b, alpha1, localXToUv(localX10), localYToUv(localY10), light, textured);
            }
        }
    }

    private static void renderGateEdge(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            RenderMetrics metrics,
            float alpha,
            VoidBlackHoleInstance.Config config,
            int light,
            boolean textured
    ) {
        if (alpha <= 0.01F || metrics.halfHeight() <= 0.001F || metrics.halfWidth() <= 0.001F) {
            return;
        }

        int r = colorRed(config.color());
        int g = colorGreen(config.color());
        int b = colorBlue(config.color());
        float innerRadius = config.flatGate() ? 0.955F : 0.86F;
        float outerRadius = config.flatGate() ? 1.030F : 1.10F;
        float z = config.flatGate() ? 0.006F : 0.012F;
        for (int radial = 0; radial < GATE_EDGE_SEGMENTS; radial++) {
            float edge0 = (float) radial / GATE_EDGE_SEGMENTS;
            float edge1 = (float) (radial + 1) / GATE_EDGE_SEGMENTS;
            float radius0 = Mth.lerp(edge0, innerRadius, outerRadius);
            float radius1 = Mth.lerp(edge1, innerRadius, outerRadius);

            for (int angle = 0; angle < ANGLE_SEGMENTS; angle++) {
                float theta0 = Mth.TWO_PI * angle / ANGLE_SEGMENTS;
                float theta1 = Mth.TWO_PI * (angle + 1) / ANGLE_SEGMENTS;
                float edgeNoise0 = gateEdge(theta0, metrics.time(), config.flatGate());
                float edgeNoise1 = gateEdge(theta1, metrics.time(), config.flatGate());
                float localX00 = Mth.cos(theta0) * radius0 * edgeNoise0;
                float localY00 = Mth.sin(theta0) * radius0 * edgeNoise0;
                float localX01 = Mth.cos(theta1) * radius0 * edgeNoise1;
                float localY01 = Mth.sin(theta1) * radius0 * edgeNoise1;
                float localX11 = Mth.cos(theta1) * radius1 * edgeNoise1;
                float localY11 = Mth.sin(theta1) * radius1 * edgeNoise1;
                float localX10 = Mth.cos(theta0) * radius1 * edgeNoise0;
                float localY10 = Mth.sin(theta0) * radius1 * edgeNoise0;
                int alpha0 = gateEdgeAlpha(alpha, edge0, theta0, metrics.time(), config.flatGate());
                int alpha1 = gateEdgeAlpha(alpha, edge1, theta1, metrics.time(), config.flatGate());

                putBlackHoleVertex(buffer, matrix4f, localX00 * metrics.halfWidth(), localY00 * metrics.halfHeight(), z,
                        r, g, b, alpha0, localXToUv(localX00), localYToUv(localY00), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX01 * metrics.halfWidth(), localY01 * metrics.halfHeight(), z,
                        r, g, b, alpha0, localXToUv(localX01), localYToUv(localY01), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX11 * metrics.halfWidth(), localY11 * metrics.halfHeight(), z,
                        r, g, b, alpha1, localXToUv(localX11), localYToUv(localY11), light, textured);
                putBlackHoleVertex(buffer, matrix4f, localX10 * metrics.halfWidth(), localY10 * metrics.halfHeight(), z,
                        r, g, b, alpha1, localXToUv(localX10), localYToUv(localY10), light, textured);
            }
        }
    }

    private static void renderMaskLayer(VertexConsumer buffer, Matrix4f matrix4f, float halfHeight, float halfWidth, float z, int effectIndex) {
        int effectIdByte = Mth.clamp(effectIndex + 1, 1, 255);
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

                putMaskVertex(buffer, matrix4f, localX00 * halfWidth, localY00 * halfHeight, z, localX00, localY00, effectIdByte);
                putMaskVertex(buffer, matrix4f, localX01 * halfWidth, localY01 * halfHeight, z, localX01, localY01, effectIdByte);
                putMaskVertex(buffer, matrix4f, localX11 * halfWidth, localY11 * halfHeight, z, localX11, localY11, effectIdByte);
                putMaskVertex(buffer, matrix4f, localX10 * halfWidth, localY10 * halfHeight, z, localX10, localY10, effectIdByte);
            }
        }
    }

    private static int blackHoleAlpha(float alpha, float x, float y, float z, float halfWidth, float halfHeight, boolean rim) {
        float normalizedX = x / halfWidth;
        float normalizedY = y / halfHeight;
        float projectedRadius = Mth.sqrt(normalizedX * normalizedX + normalizedY * normalizedY);
        if (rim) {
            float sideFacing = 1.0F - Mth.clamp(Math.abs(z) / Math.max(0.001F, halfWidth), 0.0F, 1.0F);
            float rimBand = smoothstep(0.50F, 0.98F, projectedRadius)
                    * (1.0F - smoothstep(0.98F, 1.08F, projectedRadius));
            return alphaToByte(alpha * rimBand * (0.42F + 0.58F * sideFacing));
        }

        float centerWeight = 1.0F - smoothstep(0.18F, 0.86F, projectedRadius);
        float edgeFade = 1.0F - smoothstep(0.82F, 1.05F, projectedRadius);
        float depthWeight = 0.72F + 0.28F * Mth.clamp(Math.abs(z) / Math.max(0.001F, halfWidth), 0.0F, 1.0F);
        return alphaToByte(alpha * (0.34F + 0.66F * centerWeight) * edgeFade * depthWeight);
    }

    private static int horizonAlpha(float alpha, float radius) {
        float eventHorizon = 1.0F - smoothstep(0.78F, 1.0F, radius);
        return alphaToByte(alpha * eventHorizon);
    }

    private static float gateEdge(float angle, float time, boolean flatGate) {
        if (flatGate) {
            return 1.0F
                    + Mth.sin(angle * 5.0F + time * 0.06F) * 0.006F
                    + Mth.sin(angle * 13.0F - time * 0.04F) * 0.004F;
        }

        return 1.0F
                + Mth.sin(angle * 3.0F + time * 0.08F) * 0.020F
                + Mth.sin(angle * 7.0F - time * 0.05F) * 0.014F
                + Mth.sin(angle * 13.0F + time * 0.11F) * 0.008F;
    }

    private static int gateCenterAlpha(float alpha, float radius, boolean flatGate) {
        float edgeStart = flatGate ? 0.94F : 0.82F;
        float edgeFade = 1.0F - smoothstep(edgeStart, 1.0F, radius);
        return alphaToByte(alpha * edgeFade);
    }

    private static int gateEdgeAlpha(float alpha, float edge, float angle, float time, boolean flatGate) {
        float centerBand = 1.0F - Math.abs(edge * 2.0F - 1.0F);
        if (flatGate) {
            float line = smoothstep(0.08F, 0.78F, centerBand);
            float breakUp = 0.88F
                    + Mth.sin(angle * 7.0F - time * 0.08F) * 0.07F
                    + Mth.sin(angle * 17.0F + time * 0.05F) * 0.05F;
            return alphaToByte(alpha * line * Mth.clamp(breakUp, 0.62F, 1.0F) * 1.08F);
        }

        float line = smoothstep(0.0F, 0.48F, centerBand) * (1.0F - smoothstep(0.72F, 1.0F, edge));
        float breakUp = 0.72F
                + Mth.sin(angle * 5.0F - time * 0.12F) * 0.16F
                + Mth.sin(angle * 11.0F + time * 0.09F) * 0.12F;
        return alphaToByte(alpha * line * Mth.clamp(breakUp, 0.35F, 1.0F) * 0.92F);
    }

    private static void putBlackHoleVertex(
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
            int light,
            boolean textured
    ) {
        if (textured) {
            putCompatVertex(buffer, matrix4f, x, y, z, r, g, b, a, u, v, light);
            return;
        }

        putVertex(buffer, matrix4f, x, y, z, r, g, b, a);
    }

    private static void putVertex(VertexConsumer buffer, Matrix4f matrix4f, float x, float y, float z, int r, int g, int b, int a) {
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

    private static void putMaskVertex(VertexConsumer buffer, Matrix4f matrix4f, float x, float y, float z, float localX, float localY, int effectIdByte) {
        buffer.addVertex(matrix4f, x, y, z)
                .setColor(localCoordToByte(localX), localCoordToByte(localY), effectIdByte, 255);
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

    private static float localXToUv(float localX) {
        return localX * 0.5F + 0.5F;
    }

    private static float localYToUv(float localY) {
        return 0.5F - localY * 0.5F;
    }

    private static boolean arePointsOk(Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, Vec3 fifth) {
        return isPointOk(first)
                && isPointOk(second)
                && isPointOk(third)
                && isPointOk(fourth)
                && isPointOk(fifth);
    }

    private static boolean isPointOk(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static boolean hasScreenDepth(Vec3 center, Vec3 left, Vec3 right, Vec3 down, Vec3 up) {
        return isDepthOnScreen(center)
                || isDepthOnScreen(left)
                || isDepthOnScreen(right)
                || isDepthOnScreen(down)
                || isDepthOnScreen(up);
    }

    private static boolean isDepthOnScreen(Vec3 point) {
        return point.z >= -1.0D && point.z <= 1.0D;
    }

    private static boolean isScreenBoxOnScreen(Vec3 center, Vec3 left, Vec3 right, Vec3 down, Vec3 up) {
        double minX = Math.min(center.x, Math.min(left.x, Math.min(right.x, Math.min(down.x, up.x))));
        double maxX = Math.max(center.x, Math.max(left.x, Math.max(right.x, Math.max(down.x, up.x))));
        double minY = Math.min(center.y, Math.min(left.y, Math.min(right.y, Math.min(down.y, up.y))));
        double maxY = Math.max(center.y, Math.max(left.y, Math.max(right.y, Math.max(down.y, up.y))));
        return maxX >= -1.0D - SCREEN_MASK_VIEW_MARGIN
                && minX <= 1.0D + SCREEN_MASK_VIEW_MARGIN
                && maxY >= -1.0D - SCREEN_MASK_VIEW_MARGIN
                && minY <= 1.0D + SCREEN_MASK_VIEW_MARGIN;
    }

    private static boolean areScreenAxesOk(float rightAxisU, float rightAxisV, float upAxisU, float upAxisV) {
        float determinant = rightAxisU * upAxisV - rightAxisV * upAxisU;
        return Math.abs(determinant) > 0.000001F;
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

    private record RenderMetrics(float halfHeight, float halfWidth, float fade, float time) {
    }

    public record ScreenMaskData(
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
    }

    public record FacingData(float yaw, float pitch, Vec3 forward, Vec3 horizontal, Vec3 vertical) {
    }
}
