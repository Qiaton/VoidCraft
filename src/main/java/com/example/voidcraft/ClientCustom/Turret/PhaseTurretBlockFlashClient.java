package com.example.voidcraft.ClientCustom.Turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class PhaseTurretBlockFlashClient {
    private static final int FLASH_TICKS = 5;
    private static final Map<BlockPos, Integer> FLASHES = new HashMap<>();

    private PhaseTurretBlockFlashClient() {
    }

    public static void add(BlockPos pos) {
        if (pos != null) {
            FLASHES.put(pos.immutable(), FLASH_TICKS);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            FLASHES.clear();
            return;
        }

        Iterator<Map.Entry<BlockPos, Integer>> iterator = FLASHES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }

    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (FLASHES.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            FLASHES.clear();
            return;
        }

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderTypes.debugFilledBox());

        for (Map.Entry<BlockPos, Integer> entry : FLASHES.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            float alpha = getAlpha(entry.getValue(), mc.gameRenderer.getMainCamera().getPartialTickTime());
            if (alpha <= 0.0F) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            renderWhiteBox(poseStack, buffer, state, level, pos, alpha);
            poseStack.popPose();
        }
    }

    private static float getAlpha(int ticksLeft, float partialTick) {
        float age = Mth.clamp(FLASH_TICKS - ticksLeft + partialTick, 0.0F, FLASH_TICKS);
        float half = FLASH_TICKS / 2.0F;
        return age <= half ? age / half : (FLASH_TICKS - age) / half;
    }

    private static void renderWhiteBox(PoseStack poseStack, VertexConsumer buffer, BlockState state, Level level, BlockPos pos, float alpha) {
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            return;
        }

        AABB box = shape.bounds();
        float grow = 0.002F;
        float minX = (float) box.minX - grow;
        float minY = (float) box.minY - grow;
        float minZ = (float) box.minZ - grow;
        float maxX = (float) box.maxX + grow;
        float maxY = (float) box.maxY + grow;
        float maxZ = (float) box.maxZ + grow;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        Matrix4f matrix = poseStack.last().pose();

        putFace(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, a);
        putFace(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, a);
        putFace(buffer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, a);
        putFace(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, a);
        putFace(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, a);
        putFace(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, a);
    }

    private static void putFace(
            VertexConsumer buffer,
            Matrix4f matrix,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            int alpha
    ) {
        putVertex(buffer, matrix, x0, y0, z0, alpha);
        putVertex(buffer, matrix, x1, y1, z1, alpha);
        putVertex(buffer, matrix, x2, y2, z2, alpha);
        putVertex(buffer, matrix, x3, y3, z3, alpha);
    }

    private static void putVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int alpha) {
        buffer.addVertex(matrix, x, y, z).setColor(255, 255, 255, alpha);
    }
}
