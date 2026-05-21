package com.example.voidcraft.World;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class PhaseDimensions {
    public static final String PHASE_MIRROR_ID = "phase_mirror";
    public static final ResourceKey<Level> PHASE_MIRROR = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, PHASE_MIRROR_ID)
    );

    private PhaseDimensions() {
    }

    public static boolean isPhaseMirror(Level level) {
        return level != null && isPhaseMirror(level.dimension());
    }

    public static boolean isPhaseMirror(ResourceKey<Level> dimension) {
        return PHASE_MIRROR.equals(dimension);
    }

    public static ResourceKey<Level> getTargetWorld(ResourceKey<Level> currentDimension) {
        if (currentDimension == null) {
            return null;
        }

        if (isPhaseMirror(currentDimension)) {
            return Level.OVERWORLD;
        }

        return PHASE_MIRROR;
    }
}
