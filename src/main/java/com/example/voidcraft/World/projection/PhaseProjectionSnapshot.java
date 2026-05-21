package com.example.voidcraft.World.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;

public record PhaseProjectionSnapshot(ResourceLocation sourceDimension, BlockPos center, List<Entry> entries) {
    public static final int CHUNK_RADIUS = 2;
    public static final int Y_RADIUS = 64;

    public PhaseProjectionSnapshot {
        entries = List.copyOf(entries);
    }

    public static PhaseProjectionSnapshot empty(ResourceLocation sourceDimension, BlockPos center) {
        return new PhaseProjectionSnapshot(sourceDimension, center, List.of());
    }

    public static PhaseProjectionSnapshot make(ServerLevel sourceLevel, BlockPos center) {
        if (sourceLevel == null || center == null) {
            return empty(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"), BlockPos.ZERO);
        }

        int centerChunkX = SectionPos.blockToSectionCoord(center.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(center.getZ());
        return makeChunk(sourceLevel, center, centerChunkX, centerChunkZ);
    }

    public static PhaseProjectionSnapshot makeChunk(ServerLevel sourceLevel, BlockPos center, int chunkX, int chunkZ) {
        if (sourceLevel == null || center == null) {
            return empty(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"), BlockPos.ZERO);
        }

        ResourceLocation sourceDimension = sourceLevel.dimension().location();
        List<Entry> entries = new ArrayList<>();
        readChunk(sourceLevel, chunkX, chunkZ, getMinY(sourceLevel.getMinBuildHeight(), center), getMaxY(sourceLevel.getMaxBuildHeight(), center), entries);
        return new PhaseProjectionSnapshot(sourceDimension, center.immutable(), entries);
    }

    public static int getMinY(int levelMinY, BlockPos center) {
        return Math.max(levelMinY, center.getY() - Y_RADIUS);
    }

    public static int getMaxY(int levelMaxY, BlockPos center) {
        return Math.min(levelMaxY - 1, center.getY() + Y_RADIUS);
    }

    private static void readChunk(
            ServerLevel sourceLevel,
            int chunkX,
            int chunkZ,
            int minY,
            int maxY,
            List<Entry> entries
    ) {
        ChunkAccess chunk = sourceLevel.getChunk(chunkX, chunkZ);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minX = SectionPos.sectionToBlockCoord(chunkX);
        int minZ = SectionPos.sectionToBlockCoord(chunkZ);

        // 横向仍按完整 chunk 投影，垂直只取玩家上下 64 格，避免扫描整根区块。
        for (int y = minY; y <= maxY; y++) {
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    mutablePos.set(minX + localX, y, minZ + localZ);
                    BlockState state = chunk.getBlockState(mutablePos);
                    if (canShow(state)) {
                        entries.add(new Entry(mutablePos.immutable(), Block.getId(state)));
                    }
                }
            }
        }
    }

    private static boolean canShow(BlockState state) {
        return !state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE;
    }

    public record Entry(BlockPos pos, int stateId) {
    }
}
