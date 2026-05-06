package com.example.voidcraft.ClientCustom.Turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PhaseEmitterOrbRenderer {
    private static final int ANGLE_SEGMENTS = 36;
    private static final int RADIAL_SEGMENTS = 8;

    private PhaseEmitterOrbRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            Vec3 center,
            Vec3 cameraPos,
            float radius,
            int coreColor,
            int rimColor,
            int light,
            boolean shaderCompat
    ) {
        if (radius <= 0.001F) {
            return;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 right = new Vec3(
                -camera.leftVector().x(),
                -camera.leftVector().y(),
                -camera.leftVector().z()
        );
        Vec3 up = new Vec3(
                camera.upVector().x(),
                camera.upVector().y(),
                camera.upVector().z()
        );

        poseStack.pushPose();
        poseStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        Matrix4f matrix4f = poseStack.last().pose();

        float shaderAlphaScale = shaderCompat ? 0.72F : 1.0F;
        if (!shaderCompat) {
            renderDiscLayer(buffer, matrix4f, right, up, radius * 1.46F, coreColor, rimColor, 0.18F, light, false, true);
        }
        renderDiscLayer(buffer, matrix4f, right, up, radius, coreColor, rimColor, 0.78F * shaderAlphaScale, light, shaderCompat, false);

        poseStack.popPose();
    }

    private static void renderDiscLayer(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 right,
            Vec3 up,
            float radius,
            int coreColor,
            int rimColor,
            float alphaScale,
            int light,
            boolean textured,
            boolean halo
    ) {
        for (int radial = 0; radial < RADIAL_SEGMENTS; radial++) {
            float radius0 = (float) radial / RADIAL_SEGMENTS;
            float radius1 = (float) (radial + 1) / RADIAL_SEGMENTS;

            for (int angle = 0; angle < ANGLE_SEGMENTS; angle++) {
                float theta0 = Mth.TWO_PI * angle / ANGLE_SEGMENTS;
                float theta1 = Mth.TWO_PI * (angle + 1) / ANGLE_SEGMENTS;
                float cos0 = Mth.cos(theta0);
                float sin0 = Mth.sin(theta0);
                float cos1 = Mth.cos(theta1);
                float sin1 = Mth.sin(theta1);

                putOrbQuad(buffer, matrix4f, right, up, cos0, sin0, cos1, sin1, radius0, radius1, radius, coreColor, rimColor, alphaScale, light, textured, halo);
            }
        }
    }

    private static void putOrbQuad(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 right,
            Vec3 up,
            float cos0,
            float sin0,
            float cos1,
            float sin1,
            float radius0,
            float radius1,
            float radius,
            int coreColor,
            int rimColor,
            float alphaScale,
            int light,
            boolean textured,
            boolean halo
    ) {
        putOrbVertex(buffer, matrix4f, right, up, cos0, sin0, radius0, radius, coreColor, rimColor, alphaScale, light, textured, halo);
        putOrbVertex(buffer, matrix4f, right, up, cos1, sin1, radius0, radius, coreColor, rimColor, alphaScale, light, textured, halo);
        putOrbVertex(buffer, matrix4f, right, up, cos1, sin1, radius1, radius, coreColor, rimColor, alphaScale, light, textured, halo);
        putOrbVertex(buffer, matrix4f, right, up, cos0, sin0, radius1, radius, coreColor, rimColor, alphaScale, light, textured, halo);

        if (textured) {
            // Iris/光影的实体类 RenderType 可能开启背面剔除；补一份反向面，保证圆形 billboard 不会整片消失。
            putOrbVertex(buffer, matrix4f, right, up, cos0, sin0, radius1, radius, coreColor, rimColor, alphaScale, light, true, halo);
            putOrbVertex(buffer, matrix4f, right, up, cos1, sin1, radius1, radius, coreColor, rimColor, alphaScale, light, true, halo);
            putOrbVertex(buffer, matrix4f, right, up, cos1, sin1, radius0, radius, coreColor, rimColor, alphaScale, light, true, halo);
            putOrbVertex(buffer, matrix4f, right, up, cos0, sin0, radius0, radius, coreColor, rimColor, alphaScale, light, true, halo);
        }
    }

    private static void putOrbVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            Vec3 right,
            Vec3 up,
            float cos,
            float sin,
            float normalizedRadius,
            float radius,
            int coreColor,
            int rimColor,
            float alphaScale,
            int light,
            boolean textured,
            boolean halo
    ) {
        float localX = cos * normalizedRadius * radius;
        float localY = sin * normalizedRadius * radius;
        float x = (float) (right.x * localX + up.x * localY);
        float y = (float) (right.y * localX + up.y * localY);
        float z = (float) (right.z * localX + up.z * localY);

        float alpha = halo
                ? haloAlpha(normalizedRadius, alphaScale)
                : orbAlpha(normalizedRadius, alphaScale);
        int a = alphaToByte(alpha);
        int color = halo
                ? rimColor
                : mixColor(coreColor, rimColor, smoothstep(0.36F, 1.0F, normalizedRadius));
        float shade = halo ? 1.0F : 0.74F + 0.26F * Mth.sqrt(Math.max(0.0F, 1.0F - normalizedRadius * normalizedRadius));
        int r = shadeColor(colorRed(color), shade);
        int g = shadeColor(colorGreen(color), shade);
        int b = shadeColor(colorBlue(color), shade);

        VertexConsumer vertex = buffer.addVertex(matrix4f, x, y, z)
                .setColor(r, g, b, a);
        if (textured) {
            vertex.setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(0.0F, 0.0F, 1.0F);
        }
    }

    private static float orbAlpha(float normalizedRadius, float alphaScale) {
        float sphere = Mth.sqrt(Math.max(0.0F, 1.0F - normalizedRadius * normalizedRadius));
        float edgeFade = 1.0F - smoothstep(0.76F, 1.0F, normalizedRadius);
        return alphaScale * edgeFade * (0.32F + 0.68F * sphere);
    }

    private static float haloAlpha(float normalizedRadius, float alphaScale) {
        float ring = smoothstep(0.08F, 0.34F, normalizedRadius)
                * (1.0F - smoothstep(0.58F, 1.0F, normalizedRadius));
        return alphaScale * ring;
    }

    private static int mixColor(int from, int to, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int r = Mth.clamp((int) Mth.lerp(clamped, colorRed(from), colorRed(to)), 0, 255);
        int g = Mth.clamp((int) Mth.lerp(clamped, colorGreen(from), colorGreen(to)), 0, 255);
        int b = Mth.clamp((int) Mth.lerp(clamped, colorBlue(from), colorBlue(to)), 0, 255);
        return r << 16 | g << 8 | b;
    }

    private static float smoothstep(float start, float end, float value) {
        if (start == end) {
            return value < start ? 0.0F : 1.0F;
        }
        float t = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int shadeColor(int color, float shade) {
        return Mth.clamp((int) (color * shade), 0, 255);
    }

    private static int alphaToByte(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
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
}
