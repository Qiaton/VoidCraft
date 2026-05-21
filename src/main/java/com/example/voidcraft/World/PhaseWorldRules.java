package com.example.voidcraft.World;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PhaseWorldRules {
    private static final boolean BLOCK_MOB_SPAWNS = true;
    private static final boolean CLEAR_GENERATED_LOOT_CONTAINERS = true;
    private static final boolean ENABLE_PHASE_TRAVERSAL = true;

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
        return findSamePos(level, preferred);
    }

    private static Vec3 findSamePos(ServerLevel level, Vec3 preferred) {
        double minY = level.getMinBuildHeight() + 1.0D;
        double maxY = level.getMinBuildHeight() + level.getHeight() - 2.0D;
        double y = Mth.clamp(preferred.y, minY, maxY);
        return new Vec3(preferred.x, y, preferred.z);
    }
}
