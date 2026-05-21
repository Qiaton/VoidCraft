package com.example.voidcraft.Item.custom;

import com.example.voidcraft.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;

public class EnergyCoreItem extends Item {
    public static final long BASE_LIFETIME = 200L;
    public static final long LIFETIME_WEAR_ENERGY = 2_000L;
    public static final long MAX_LIFETIME_WEAR_ENERGY = 10_000L;
    private static final double MIN_MAX_LIFETIME_SCALE = 0.8D;

    private final CoreTier tier;

    public EnergyCoreItem(Properties properties, CoreTier tier) {
        super(properties);
        this.tier = tier;
    }

    public long getRechargePerTick() {
        return tier.rechargePerTick();
    }

    public double getRechargeEfficiencyPercent() {
        return tier.rechargePerTick() * 100.0D;
    }

    public long getInitialMaxLifetime() {
        return Math.max(1L, Math.round(BASE_LIFETIME * tier.lifetimeMultiplier()));
    }

    public EnergyCoreData getCoreData(ItemStack stack) {
        EnergyCoreData data = stack.get(ModDataComponents.ENERGY_CORE_DATA.get());
        if (data == null) {
            return new EnergyCoreData(getInitialMaxLifetime(), 0L, 0L, 0L);
        }
        long initialMaxLifetime = getInitialMaxLifetime();
        return data.clamp(initialMaxLifetime, getMaxLifetimeByCoreLife(initialMaxLifetime, data.maxLifetimeLoss()));
    }

    public long getCurrentMaxLifetime(ItemStack stack) {
        EnergyCoreData data = getCoreData(stack);
        return getMaxLifetimeByCoreLife(getInitialMaxLifetime(), data.maxLifetimeLoss());
    }

    public long getCurrentLifetime(ItemStack stack) {
        return getCoreData(stack).currentLifetime();
    }

    public long restoreLifetime(ItemStack stack, long amount) {
        if (amount <= 0L) {
            return 0L;
        }

        EnergyCoreData data = getCoreData(stack);
        long currentMaxLifetime = getCurrentMaxLifetime(stack);
        long currentLifetime = Math.min(data.currentLifetime(), currentMaxLifetime);
        if (currentMaxLifetime <= 0L || currentLifetime >= currentMaxLifetime) {
            return 0L;
        }

        long restored = Math.min(amount, currentMaxLifetime - currentLifetime);
        stack.set(ModDataComponents.ENERGY_CORE_DATA.value(), new EnergyCoreData(
                currentLifetime + restored,
                data.maxLifetimeLoss(),
                data.lifetimeWearProgress(),
                data.maxLifetimeWearProgress()
        ));
        return restored;
    }

    public WearResult applyRecoveredEnergy(ItemStack stack, long actualRecoveredEnergy) {
        if (actualRecoveredEnergy <= 0L) {
            EnergyCoreData data = getCoreData(stack);
            return new WearResult(data.currentLifetime(), getCurrentMaxLifetime(stack), false);
        }

        EnergyCoreData data = getCoreData(stack);
        long initialMaxLifetime = getInitialMaxLifetime();
        long currentMaxLifetime = getMaxLifetimeByCoreLife(initialMaxLifetime, data.maxLifetimeLoss());
        if (currentMaxLifetime <= 0L) {
            return new WearResult(0L, 0L, true);
        }

        long currentLifetime = Math.min(data.currentLifetime(), currentMaxLifetime);
        long lifetimeProgress = data.lifetimeWearProgress() + actualRecoveredEnergy;
        long lifetimeDamage = lifetimeProgress / LIFETIME_WEAR_ENERGY;
        lifetimeProgress = lifetimeProgress % LIFETIME_WEAR_ENERGY;
        currentLifetime = Math.max(0L, currentLifetime - lifetimeDamage);

        long maxLifetimeProgress = data.maxLifetimeWearProgress() + actualRecoveredEnergy;
        long maxLifetimeDamage = maxLifetimeProgress / MAX_LIFETIME_WEAR_ENERGY;
        maxLifetimeProgress = maxLifetimeProgress % MAX_LIFETIME_WEAR_ENERGY;
        long maxLifetimeLoss = Math.min(initialMaxLifetime, data.maxLifetimeLoss() + maxLifetimeDamage);
        long nextMaxLifetime = getMaxLifetimeByCoreLife(initialMaxLifetime, maxLifetimeLoss);

        if (currentLifetime > nextMaxLifetime) {
            currentLifetime = nextMaxLifetime;
        }

        if (nextMaxLifetime <= 0L) {
            return new WearResult(0L, 0L, true);
        }

        EnergyCoreData nextData = new EnergyCoreData(
                currentLifetime,
                maxLifetimeLoss,
                lifetimeProgress,
                maxLifetimeProgress
        );
        stack.set(ModDataComponents.ENERGY_CORE_DATA.value(), nextData);

        return new WearResult(currentLifetime, nextMaxLifetime, false);
    }

    @Override
    public void appendHoverText(
            @NonNull ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);

        long currentMaxLifetime = getCurrentMaxLifetime(stack);
        long currentLifetime = getCurrentLifetime(stack);
        double lifetimePercent = currentMaxLifetime <= 0L
                ? 0.0D
                : currentLifetime * 100.0D / currentMaxLifetime;

        tooltip.add(Component.translatable(
                "tooltip.void_craft.energy_core.recharge_efficiency",
                formatPercent(getRechargeEfficiencyPercent())
        ));
        tooltip.add(Component.translatable(
                "tooltip.void_craft.energy_core.lifetime",
                formatPercent(lifetimePercent)
        ));
        tooltip.add(Component.translatable(
                "tooltip.void_craft.energy_core.max_lifetime",
                Component.translatable(getMaxLifetimeStateKey(stack))
        ));

        if (currentLifetime <= 0L) {
            tooltip.add(Component.translatable("tooltip.void_craft.energy_core.depleted"));
        }
    }

    private String getMaxLifetimeStateKey(ItemStack stack) {
        double percent = getCoreLifePercent(stack);

        if (percent >= 95.0D) {
            return "tooltip.void_craft.energy_core.max_lifetime.excellent_plus";
        }
        if (percent >= 80.0D) {
            return "tooltip.void_craft.energy_core.max_lifetime.excellent";
        }
        if (percent >= 60.0D) {
            return "tooltip.void_craft.energy_core.max_lifetime.normal";
        }
        if (percent >= 40.0D) {
            return "tooltip.void_craft.energy_core.max_lifetime.aged";
        }
        if (percent >= 20.0D) {
            return "tooltip.void_craft.energy_core.max_lifetime.unstable";
        }
        return "tooltip.void_craft.energy_core.max_lifetime.near_scrap";
    }

    private static String formatPercent(double percent) {
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private double getCoreLifePercent(ItemStack stack) {
        long initialMaxLifetime = getInitialMaxLifetime();
        if (initialMaxLifetime <= 0L) {
            return 0.0D;
        }

        EnergyCoreData data = getCoreData(stack);
        return getRemainingCoreLife(initialMaxLifetime, data.maxLifetimeLoss()) * 100.0D / initialMaxLifetime;
    }

    private static long getMaxLifetimeByCoreLife(long initialMaxLifetime, long maxLifetimeLoss) {
        if (initialMaxLifetime <= 0L) {
            return 0L;
        }

        long remainingLife = getRemainingCoreLife(initialMaxLifetime, maxLifetimeLoss);
        if (remainingLife <= 0L) {
            return 0L;
        }

        // 寿命只影响最高 20% 的可用耐久，避免老化后直接把核心耐久砍穿。
        double lifePercent = remainingLife / (double) initialMaxLifetime;
        double scale = MIN_MAX_LIFETIME_SCALE + lifePercent * (1.0D - MIN_MAX_LIFETIME_SCALE);
        return Math.max(1L, Math.round(initialMaxLifetime * scale));
    }

    private static long getRemainingCoreLife(long initialMaxLifetime, long maxLifetimeLoss) {
        return Math.max(0L, initialMaxLifetime - Math.max(0L, maxLifetimeLoss));
    }

    public enum CoreTier {
        BASIC(1L, 1.0D),
        PLUS(2L, 1.5D),
        PRO(3L, 2.0D),
        MAX(4L, 3.0D);

        private final long rechargePerTick;
        private final double lifetimeMultiplier;

        CoreTier(long rechargePerTick, double lifetimeMultiplier) {
            this.rechargePerTick = rechargePerTick;
            this.lifetimeMultiplier = lifetimeMultiplier;
        }

        public long rechargePerTick() {
            return rechargePerTick;
        }

        public double lifetimeMultiplier() {
            return lifetimeMultiplier;
        }
    }

    public record WearResult(long currentLifetime, long currentMaxLifetime, boolean scrapped) {
    }
}
