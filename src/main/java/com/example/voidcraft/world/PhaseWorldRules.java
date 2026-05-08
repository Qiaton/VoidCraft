package com.example.voidcraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class PhaseWorldRules {
    private static final boolean BLOCK_MOB_SPAWNS = true;
    private static final boolean CLEAR_GENERATED_LOOT_CONTAINERS = true;
    private static final boolean ENABLE_PHASE_TRAVERSAL = true;
    private static final int NORMAL_DIMENSION_SAFE_SEARCH_RADIUS = 8;
    private static final int FORCE_SAME_POSITION_SURFACE_DISTANCE = 30;

    private PhaseWorldRules() {
    }

    public static ResourceKey<Level> getTargetWorld(ResourceKey<Level> currentDimension) {
        return PhaseDimensions.getTargetWorld(currentDimension);
    }

    public static boolean noMobSpawn(Level level) {
        return BLOCK_MOB_SPAWNS && PhaseDimensions.isPhaseMirror(level);
    }

    public static boolean needClearLoot(Level level) {
        return CLEAR_GENERATED_LOOT_CONTAINERS && PhaseDimensions.isPhaseMirror(level);
    }

    public static boolean showPhaseLook(Level level) {
        return PhaseDimensions.isPhaseMirror(level);
    }

    public static boolean canPhaseWalk(Level level) {
        return ENABLE_PHASE_TRAVERSAL && PhaseDimensions.isPhaseMirror(level);
    }

    public static Vec3 findArrivalPos(ServerLevel level, Vec3 preferred) {
        if (PhaseDimensions.isPhaseMirror(level) && canPhaseWalk(level)) {
            return findPhasePos(level, preferred);
        }

        Vec3 sameHeight = findSameY(level, preferred);
        if (sameHeight != null) {
            return sameHeight;
        }

        if (needSamePos(level, preferred)) {
            return findSamePos(level, preferred);
        }

        Vec3 safeSurface = findSafeGround(level, preferred);
        if (safeSurface != null) {
            return safeSurface;
        }

        double minY = level.getMinY() + 1.0D;
        double maxY = level.getMinY() + level.getHeight() - 2.0D;
        double y = Mth.clamp(preferred.y, minY, maxY);
        return new Vec3(preferred.x, y, preferred.z);
    }

    private static boolean needSamePos(ServerLevel level, Vec3 preferred) {
        int x = Mth.floor(preferred.x);
        int z = Mth.floor(preferred.z);
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return Math.abs(surfaceY - preferred.y) > FORCE_SAME_POSITION_SURFACE_DISTANCE;
    }

    private static Vec3 findSamePos(ServerLevel level, Vec3 preferred) {
        double minY = level.getMinY() + 1.0D;
        double maxY = level.getMinY() + level.getHeight() - 2.0D;
        double y = Mth.clamp(preferred.y, minY, maxY);
        return new Vec3(preferred.x, y, preferred.z);
    }

    private static Vec3 findPhasePos(ServerLevel level, Vec3 preferred) {
        int x = Mth.floor(preferred.x);
        int z = Mth.floor(preferred.z);
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));

        return findSamePos(level, preferred);
    }

    private static Vec3 findSafeGround(ServerLevel level, Vec3 preferred) {
        int baseX = Mth.floor(preferred.x);
        int baseZ = Mth.floor(preferred.z);

        for (int radius = 0; radius <= NORMAL_DIMENSION_SAFE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    Vec3 candidate = findGroundSpot(level, baseX + dx, baseZ + dz, preferred);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static Vec3 findSameY(ServerLevel level, Vec3 preferred) {
        int baseX = Mth.floor(preferred.x);
        int baseY = Mth.floor(preferred.y);
        int baseZ = Mth.floor(preferred.z);
        if (baseY <= level.getMinY() || baseY + 1 > level.getMaxY()) {
            return null;
        }

        for (int radius = 0; radius <= NORMAL_DIMENSION_SAFE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    Vec3 candidate = findSameYSpot(level, baseX + dx, baseY, baseZ + dz, preferred);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static Vec3 findSameYSpot(ServerLevel level, int x, int y, int z, Vec3 preferred) {
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        BlockPos feet = new BlockPos(x, y, z);
        if (!level.getWorldBorder().isWithinBounds(feet) || !isSafeAir(level, feet)) {
            return null;
        }

        double arrivalX = x == Mth.floor(preferred.x) ? preferred.x : x + 0.5D;
        double arrivalZ = z == Mth.floor(preferred.z) ? preferred.z : z + 0.5D;
        return new Vec3(arrivalX, y, arrivalZ);
    }

    private static Vec3 findGroundSpot(ServerLevel level, int x, int z, Vec3 preferred) {
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, y, z);
        if (!level.getWorldBorder().isWithinBounds(feet) || !isSafeGround(level, feet)) {
            return null;
        }

        double arrivalX = x == Mth.floor(preferred.x) ? preferred.x : x + 0.5D;
        double arrivalZ = z == Mth.floor(preferred.z) ? preferred.z : z + 0.5D;
        return new Vec3(arrivalX, y, arrivalZ);
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos feet) {
        if (!isSafeAir(level, feet)) {
            return false;
        }

        BlockState ground = level.getBlockState(feet.below());
        return !ground.getCollisionShape(level, feet.below()).isEmpty();
    }

    private static boolean isSafeAir(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinY() || feet.getY() + 1 > level.getMaxY()) {
            return false;
        }

        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        return feetState.isAir()
                && headState.isAir()
                && feetState.getFluidState().isEmpty()
                && headState.getFluidState().isEmpty();
    }
}
