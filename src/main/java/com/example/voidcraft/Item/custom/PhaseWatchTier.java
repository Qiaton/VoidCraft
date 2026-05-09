package com.example.voidcraft.Item.custom;

import net.minecraft.network.chat.Component;

public enum PhaseWatchTier {
    CRUDE(1, "crude", 200L),
    ATTUNED(2, "attuned", 600L),
    STABILIZED(3, "stabilized", 1_000L),
    RESONANT(4, "resonant", 2_000L),
    VOID_ENERGY(5, "void_energy_grade", 5_000L);

    private final int level;
    private final String id;
    private final long maxEnergy;

    PhaseWatchTier(int level, String id, long maxEnergy) {
        this.level = level;
        this.id = id;
        this.maxEnergy = maxEnergy;
    }

    public int level() {
        return level;
    }

    public String id() {
        return id;
    }

    public long maxEnergy() {
        return maxEnergy;
    }

    public Component getDisplayName() {
        return Component.translatable("phase_watch_tier.void_craft." + id);
    }
}
