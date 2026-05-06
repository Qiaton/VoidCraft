package com.example.voidcraft.Item.custom;

import net.minecraft.world.item.Item;

public class VoidCrystalItem extends Item {
    public static final int BASE_DURABILITY = 1500;

    private final CrystalTier tier;

    public VoidCrystalItem(Properties properties, CrystalTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CrystalTier getTier() {
        return tier;
    }

    public int getDurabilityMultiplier() {
        return tier.durabilityMultiplier();
    }

    public int getConfiguredMaxDurability() {
        return BASE_DURABILITY * tier.durabilityMultiplier();
    }

    public enum CrystalTier {
        LOW_PURITY(1),
        HIGH_PURITY(10),
        PURE(45);

        private final int durabilityMultiplier;

        CrystalTier(int durabilityMultiplier) {
            this.durabilityMultiplier = durabilityMultiplier;
        }

        public int durabilityMultiplier() {
            return durabilityMultiplier;
        }
    }
}
