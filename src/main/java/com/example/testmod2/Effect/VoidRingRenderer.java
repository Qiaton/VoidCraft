package com.example.testmod2.Effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class VoidRingRenderer {

    public static void render(
            PoseStack poseStack,
            VertexConsumer buffer,
            VoidRingInstance ring,
            float partialTick,
            int light
    ){
        final int SEGMENTS = 32;
        float progress = Mth.clamp(( ring.age + partialTick )/ring.duration, 0F, 1F);
        float pulse = 1- Math.abs(progress * 2.0f - 1.0f);
        float radius = ring.startRadius + pulse * (ring.maxRadius - ring.startRadius);
        float alpha = pulse;

        float halfThickness = ring.thickness/2f;
        float innerRadius = Math.max(0.0f, radius - halfThickness);
        float outerRadius = radius + halfThickness;
        int a = Mth.clamp((int)(alpha*255F),0,255);
        int r = 255;
        int g = 255;
        int b = 255;

        Matrix4f matrix4f = poseStack.last().pose();
        for(int i = 0; i < SEGMENTS; i++){
            float t0 = (float) i/SEGMENTS;
            float t1 = (float) (i+1)/SEGMENTS;
            float angle0 = t0*Mth.TWO_PI;
            float angle1 = t1*Mth.TWO_PI;

            float cos0 = Mth.cos(angle0);
            float sin0 = Mth.sin(angle0);
            float cos1 = Mth.cos(angle1);
            float sin1 = Mth.sin(angle1);

            float x0Outer = cos0 * outerRadius;
            float z0Outer = sin0 * outerRadius;
            float x0Inner = cos0 * innerRadius;
            float z0Inner = sin0 * innerRadius;

            float x1Outer = cos1 * outerRadius;
            float z1Outer = sin1 * outerRadius;
            float x1Inner = cos1 * innerRadius;
            float z1Inner = sin1 * innerRadius;

            PutVertex(buffer, matrix4f, x0Outer, 0F, z0Outer, r, g, b, a, light);
            PutVertex(buffer, matrix4f, x1Outer, 0F, z1Outer, r, g, b, a, light);
            PutVertex(buffer, matrix4f, x1Inner, 0F, z1Inner, r, g, b, a, light);
            PutVertex(buffer, matrix4f, x0Inner, 0F, z0Inner, r, g, b, a, light);
        }
    }
    public static void PutVertex(
            VertexConsumer buffer,
            Matrix4f matrix4f,
            float x,
            float y,
            float z,
            int r,
            int g,
            int b,
            int a,
            int light
    ){
        buffer.addVertex(matrix4f,x,y,z)
                .setColor(r,g,b,a);
    }
}
