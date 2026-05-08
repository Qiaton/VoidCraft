package com.example.voidcraft.Item.custom;

import com.example.voidcraft.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class VoidCrystalItem extends Item {
    public static final int BASE_DURABILITY = 1500;
    // 每累计这么多发电进度，才真正扣 1 点物品耐久。
    public static final int DAMAGE_PROGRESS_THRESHOLD = 500;

    private final CrystalTier tier;

    public VoidCrystalItem(Properties properties, CrystalTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CrystalTier getTier() {
        return tier;
    }

    public int getLifeMult() {
        return tier.durabilityMultiplier();
    }

    public int getMaxLife() {
        return BASE_DURABILITY * tier.durabilityMultiplier();
    }

    public int getGenerationMultiplier() {
        // 发电机用这个倍率决定每 tick 产多少虚空能。
        return tier.generationMultiplier();
    }

    public static int getProgress(ItemStack stack) {
        // 进度存在物品栈自己的数据组件里，插拔机器后也跟着结晶走。
        Integer progress = stack.get(ModDataComponents.VOID_CRYSTAL_PROGRESS.get());
        if (progress == null) {
            return 0;
        }
        return Math.max(0, Math.min(DAMAGE_PROGRESS_THRESHOLD - 1, progress));
    }

    public static void setProgress(ItemStack stack, int progress) {
        // 进度只保留到下一次扣耐久前，避免数值无限长。
        stack.set(
                ModDataComponents.VOID_CRYSTAL_PROGRESS.value(),
                Math.max(0, Math.min(DAMAGE_PROGRESS_THRESHOLD - 1, progress))
        );
    }

    public static DamageProgressResult addProgress(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof VoidCrystalItem) || amount <= 0) {
            return new DamageProgressResult(getProgress(stack), false, false);
        }

        // 发电先累积进度，进度满阈值后再折算成耐久损耗。
        int nextProgress = getProgress(stack) + amount;
        int durabilityDamage = nextProgress / DAMAGE_PROGRESS_THRESHOLD;
        nextProgress %= DAMAGE_PROGRESS_THRESHOLD;

        boolean damaged = durabilityDamage > 0;
        boolean depleted = false;
        if (damaged) {
            // 耐久耗尽时发电机会把结晶替换成废渣。
            int nextDamage = stack.getDamageValue() + durabilityDamage;
            depleted = nextDamage >= stack.getMaxDamage();
            stack.setDamageValue(Math.min(nextDamage, stack.getMaxDamage()));
        }

        setProgress(stack, depleted ? 0 : nextProgress);
        return new DamageProgressResult(getProgress(stack), damaged, depleted);
    }

    public enum CrystalTier {
        // 第一个数控制耐久倍率，第二个数控制发电倍率。
        LOW_PURITY(1, 1),
        HIGH_PURITY(10, 8),
        PURE(45, 32);

        private final int durabilityMultiplier;
        private final int generationMultiplier;

        CrystalTier(int durabilityMultiplier, int generationMultiplier) {
            this.durabilityMultiplier = durabilityMultiplier;
            this.generationMultiplier = generationMultiplier;
        }

        public int durabilityMultiplier() {
            return durabilityMultiplier;
        }

        public int generationMultiplier() {
            return generationMultiplier;
        }
    }

    public record DamageProgressResult(int progress, boolean damaged, boolean depleted) {
    }
}
