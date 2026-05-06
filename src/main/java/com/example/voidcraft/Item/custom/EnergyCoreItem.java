package com.example.voidcraft.Item.custom;

import com.example.voidcraft.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.function.Consumer;

public class EnergyCoreItem extends Item {
    public static final long BASE_LIFETIME = 200L;
    public static final long LIFETIME_WEAR_ENERGY = 2_000L;
    public static final long MAX_LIFETIME_WEAR_ENERGY = 10_000L;

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
        return data.clamp(getInitialMaxLifetime());
    }

    public long getCurrentMaxLifetime(ItemStack stack) {
        EnergyCoreData data = getCoreData(stack);
        return Math.max(0L, getInitialMaxLifetime() - data.maxLifetimeLoss());
    }

    public long getCurrentLifetime(ItemStack stack) {
        return getCoreData(stack).currentLifetime();
    }

    public WearResult applyRecoveredEnergy(ItemStack stack, long actualRecoveredEnergy) {
        if (actualRecoveredEnergy <= 0L) {
            EnergyCoreData data = getCoreData(stack);
            return new WearResult(data.currentLifetime(), getCurrentMaxLifetime(stack), false);
        }

        EnergyCoreData data = getCoreData(stack);
        long initialMaxLifetime = getInitialMaxLifetime();
        long currentMaxLifetime = Math.max(0L, initialMaxLifetime - data.maxLifetimeLoss());
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
        long maxLifetimeLoss = data.maxLifetimeLoss() + maxLifetimeDamage;
        long nextMaxLifetime = Math.max(0L, initialMaxLifetime - maxLifetimeLoss);

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
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);

        long currentMaxLifetime = getCurrentMaxLifetime(stack);
        long currentLifetime = getCurrentLifetime(stack);
        double lifetimePercent = currentMaxLifetime <= 0L
                ? 0.0D
                : currentLifetime * 100.0D / currentMaxLifetime;

        tooltip.accept(Component.translatable(
                "tooltip.void_craft.energy_core.recharge_efficiency",
                formatPercent(getRechargeEfficiencyPercent())
        ));
        tooltip.accept(Component.translatable(
                "tooltip.void_craft.energy_core.lifetime",
                formatPercent(lifetimePercent)
        ));
        tooltip.accept(Component.translatable(
                "tooltip.void_craft.energy_core.max_lifetime",
                Component.translatable(getMaxLifetimeStateKey(stack))
        ));

        if (currentLifetime <= 0L) {
            tooltip.accept(Component.translatable("tooltip.void_craft.energy_core.depleted"));
        }
    }

    private String getMaxLifetimeStateKey(ItemStack stack) {
        long initialMaxLifetime = getInitialMaxLifetime();
        long currentMaxLifetime = getCurrentMaxLifetime(stack);
        double percent = initialMaxLifetime <= 0L
                ? 0.0D
                : currentMaxLifetime * 100.0D / initialMaxLifetime;

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

    public enum CoreTier {
        BASIC(1L, 1.0D),
        ADVANCED(2L, 1.5D),
        ELITE(3L, 2.0D);

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
