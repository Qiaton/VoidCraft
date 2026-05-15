package com.example.voidcraft.World.projection;

import com.example.voidcraft.Block.Block.PhaseBlock;
import com.example.voidcraft.World.PhaseDimensions;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

public final class PhaseProjectionClient {
    private static final int KEEP_TICKS = 20 * 45;
    private static volatile Data data = Data.empty();
    private static volatile boolean hidePhaseBlocks;
    private static int ageTicks;
    private static boolean dirtyInPhase;

    private PhaseProjectionClient() {
    }

    public static void accept(PhaseProjectionSnapshot newSnapshot) {
        // 网络线程交来的数据只进客户端缓存，不改客户端世界里的真实方块。
        PhaseProjectionSnapshot oldSnapshot = data.snapshot();
        boolean newProjection = oldSnapshot == null || !isSameProjection(oldSnapshot, newSnapshot);
        PhaseProjectionSnapshot merged = mergeSnapshot(oldSnapshot, newSnapshot);
        Data newData = new Data(merged, makeStateIds(merged), makeSectionIds(merged));
        if (newProjection) {
            markFullDirty(oldSnapshot);
        }
        data = newData;
        ageTicks = 0;
        if (newProjection) {
            dirtyInPhase = false;
            hidePhaseBlocks = false;
        }
        markChunkDirty(newSnapshot);
        setDirtyIfInPhase(merged);
    }

    public static BlockState getDrawState(BlockPos pos, BlockState state) {
        // 区块编译时用投影方块替换相位方块，让原版区块 mesh 缓存接管渲染。
        Data currentData = data;
        PhaseProjectionSnapshot current = currentData.snapshot();
        if (!hidePhaseBlocks || current == null || !isInProjectionRange(current, pos)) {
            return state;
        }

        int stateId = currentData.stateIds().get(pos.asLong());
        if (stateId >= 0) {
            return Block.stateById(stateId);
        }
        if (state.getBlock() instanceof PhaseBlock) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    public static boolean hasDrawSection(long sectionPos) {
        Data currentData = data;
        return hidePhaseBlocks
                && currentData.snapshot() != null
                && currentData.sectionIds().contains(sectionPos);
    }

    public static FluidState getDrawFluidState(BlockPos pos, FluidState state) {
        Data currentData = data;
        PhaseProjectionSnapshot current = currentData.snapshot();
        if (!hidePhaseBlocks || current == null || !isInProjectionRange(current, pos)) {
            return state;
        }

        int stateId = currentData.stateIds().get(pos.asLong());
        if (stateId >= 0) {
            return Block.stateById(stateId).getFluidState();
        }
        return state;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }

        PhaseProjectionSnapshot current = data.snapshot();
        if (current == null) {
            return;
        }

        if (PhaseDimensions.isPhaseMirror(mc.level)) {
            hidePhaseBlocks = true;
            ageTicks = 0;
            if (!dirtyInPhase) {
                markFullDirty(current);
                dirtyInPhase = true;
            }
            return;
        }

        hidePhaseBlocks = false;
        // 不在相位维度时也先保留一小段时间，因为投影包会早于真正换维度到达。
        ageTicks++;
        if (ageTicks > KEEP_TICKS) {
            clear();
        }
    }

    public static void clear() {
        markFullDirty(data.snapshot());
        data = Data.empty();
        ageTicks = 0;
        dirtyInPhase = false;
        hidePhaseBlocks = false;
    }

    private static boolean isInProjectionRange(PhaseProjectionSnapshot current, BlockPos pos) {
        int centerChunkX = SectionPos.blockToSectionCoord(current.center().getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(current.center().getZ());
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return Math.abs(chunkX - centerChunkX) <= PhaseProjectionSnapshot.CHUNK_RADIUS
                && Math.abs(chunkZ - centerChunkZ) <= PhaseProjectionSnapshot.CHUNK_RADIUS
                && Math.abs(pos.getY() - current.center().getY()) <= PhaseProjectionSnapshot.Y_RADIUS;
    }

    private static PhaseProjectionSnapshot mergeSnapshot(PhaseProjectionSnapshot oldSnapshot, PhaseProjectionSnapshot newSnapshot) {
        if (oldSnapshot == null || !isSameProjection(oldSnapshot, newSnapshot)) {
            return newSnapshot;
        }

        LongSet usedPositions = new LongOpenHashSet(oldSnapshot.entries().size() + newSnapshot.entries().size());
        List<PhaseProjectionSnapshot.Entry> entries = new ArrayList<>(oldSnapshot.entries().size() + newSnapshot.entries().size());
        for (PhaseProjectionSnapshot.Entry entry : oldSnapshot.entries()) {
            if (usedPositions.add(entry.pos().asLong())) {
                entries.add(entry);
            }
        }
        for (PhaseProjectionSnapshot.Entry entry : newSnapshot.entries()) {
            if (usedPositions.add(entry.pos().asLong())) {
                entries.add(entry);
            }
        }
        return new PhaseProjectionSnapshot(oldSnapshot.sourceDimension(), oldSnapshot.center(), entries);
    }

    private static boolean isSameProjection(PhaseProjectionSnapshot oldSnapshot, PhaseProjectionSnapshot newSnapshot) {
        return newSnapshot != null
                && oldSnapshot.sourceDimension().equals(newSnapshot.sourceDimension())
                && oldSnapshot.center().equals(newSnapshot.center());
    }

    private static Long2IntMap makeStateIds(PhaseProjectionSnapshot current) {
        if (current == null || current.entries().isEmpty()) {
            return makeEmptyStateIds();
        }

        Long2IntOpenHashMap stateIds = new Long2IntOpenHashMap(current.entries().size());
        stateIds.defaultReturnValue(-1);
        for (PhaseProjectionSnapshot.Entry entry : current.entries()) {
            stateIds.put(entry.pos().asLong(), entry.stateId());
        }
        return stateIds;
    }

    private static LongSet makeSectionIds(PhaseProjectionSnapshot current) {
        if (current == null || current.entries().isEmpty()) {
            return new LongOpenHashSet();
        }

        LongSet sectionIds = new LongOpenHashSet();
        for (PhaseProjectionSnapshot.Entry entry : current.entries()) {
            sectionIds.add(SectionPos.asLong(entry.pos()));
        }
        return sectionIds;
    }

    private static Long2IntMap makeEmptyStateIds() {
        Long2IntOpenHashMap stateIds = new Long2IntOpenHashMap();
        stateIds.defaultReturnValue(-1);
        return stateIds;
    }

    private static void setDirtyIfInPhase(PhaseProjectionSnapshot current) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && PhaseDimensions.isPhaseMirror(mc.level) && !dirtyInPhase) {
            markFullDirty(current);
            dirtyInPhase = true;
            hidePhaseBlocks = true;
        }
    }

    private static void markChunkDirty(PhaseProjectionSnapshot current) {
        if (current == null) {
            return;
        }

        if (current.entries().isEmpty()) {
            markFullDirty(current);
            return;
        }

        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (PhaseProjectionSnapshot.Entry entry : current.entries()) {
            BlockPos pos = entry.pos();
            int chunkX = SectionPos.blockToSectionCoord(pos.getX());
            int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
            minChunkX = Math.min(minChunkX, chunkX);
            maxChunkX = Math.max(maxChunkX, chunkX);
            minChunkZ = Math.min(minChunkZ, chunkZ);
            maxChunkZ = Math.max(maxChunkZ, chunkZ);
        }
        // 新区块到达后，把旁边一圈也重建一下，让跨区块相邻面可以重新剔除。
        markChunkRangeDirty(
                current,
                clampChunkX(current, minChunkX - 1),
                clampChunkX(current, maxChunkX + 1),
                clampChunkZ(current, minChunkZ - 1),
                clampChunkZ(current, maxChunkZ + 1)
        );
    }

    private static void markFullDirty(PhaseProjectionSnapshot current) {
        if (current == null) {
            return;
        }

        int centerChunkX = SectionPos.blockToSectionCoord(current.center().getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(current.center().getZ());
        markChunkRangeDirty(
                current,
                centerChunkX - PhaseProjectionSnapshot.CHUNK_RADIUS,
                centerChunkX + PhaseProjectionSnapshot.CHUNK_RADIUS,
                centerChunkZ - PhaseProjectionSnapshot.CHUNK_RADIUS,
                centerChunkZ + PhaseProjectionSnapshot.CHUNK_RADIUS
        );
    }

    private static void markChunkRangeDirty(PhaseProjectionSnapshot current, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        Minecraft mc = Minecraft.getInstance();
        if (current == null || mc.level == null || !PhaseDimensions.isPhaseMirror(mc.level)) {
            return;
        }

        int minX = SectionPos.sectionToBlockCoord(minChunkX);
        int maxX = SectionPos.sectionToBlockCoord(maxChunkX) + 15;
        int minZ = SectionPos.sectionToBlockCoord(minChunkZ);
        int maxZ = SectionPos.sectionToBlockCoord(maxChunkZ) + 15;
        int minY = PhaseProjectionSnapshot.getMinY(mc.level.getMinBuildHeight(), current.center());
        int maxY = PhaseProjectionSnapshot.getMaxY(mc.level.getMaxBuildHeight(), current.center());
        // 投影范围改变时只重建投影高度内的相位区块，让区块网格重新套用隐藏规则。
        mc.levelRenderer.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
        mc.levelRenderer.needsUpdate();
    }

    private static int clampChunkX(PhaseProjectionSnapshot current, int chunkX) {
        int centerChunkX = SectionPos.blockToSectionCoord(current.center().getX());
        int minChunkX = centerChunkX - PhaseProjectionSnapshot.CHUNK_RADIUS;
        int maxChunkX = centerChunkX + PhaseProjectionSnapshot.CHUNK_RADIUS;
        return Math.max(minChunkX, Math.min(maxChunkX, chunkX));
    }

    private static int clampChunkZ(PhaseProjectionSnapshot current, int chunkZ) {
        int centerChunkZ = SectionPos.blockToSectionCoord(current.center().getZ());
        int minChunkZ = centerChunkZ - PhaseProjectionSnapshot.CHUNK_RADIUS;
        int maxChunkZ = centerChunkZ + PhaseProjectionSnapshot.CHUNK_RADIUS;
        return Math.max(minChunkZ, Math.min(maxChunkZ, chunkZ));
    }

    private record Data(PhaseProjectionSnapshot snapshot, Long2IntMap stateIds, LongSet sectionIds) {
        private static Data empty() {
            return new Data(null, makeEmptyStateIds(), new LongOpenHashSet());
        }
    }
}
