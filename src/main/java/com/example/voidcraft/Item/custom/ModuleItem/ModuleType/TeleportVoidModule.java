package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.ClientCustom.ModuleInputMode;
import com.example.voidcraft.Custom.Clock.ModuleSkill.TeleportVoidModuleClock;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.ACTIVE_DURATION;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.COOLDOWN_REDUCTION;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.SPEED_BOOST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

public class TeleportVoidModule extends ModuleItem {
    private static final long BURST_COOLDOWN_TICKS = 900L;
    private static final long BURST_ENERGY_COST = 600L;
    private static final double BASE_DEPLOY_ENERGY = 100.0D;
    private static final double DEPLOY_ENERGY_PER_LEVEL = 100.0D;
    private static final double DEPLOY_ENERGY_PER_DURATION_LEVEL = 50.0D;
    private static final int BASE_GATE_TICKS = 600;
    private static final int GATE_TICKS_PER_LEVEL = 400;
    private static final int GATE_TICKS_PER_DURATION_LEVEL = 300;
    private static final float BASE_DEPLOY_SPEED = 0.15F;
    private static final float DEPLOY_SPEED_PER_SPEED_LEVEL = 0.15F;
    private static final float BASE_BLOCKS_PER_SECOND = 30.0F;
    private static final float BLOCKS_PER_SECOND_PER_SPEED_LEVEL = 10.0F;

    public TeleportVoidModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() != BURST) {
            return;
        }

        if (TeleportVoidModuleClock.hasDeploy(player, slot)) {
            TeleportVoidModuleClock.endDeploy(player, slot);
            return;
        }

        boolean cooldownReady = ModuleSkillClock.canUseNow(player, slot);
        if (!cooldownReady && ModuleSkillClock.getEnergy(player) < stats.burstEnergyCost()) {
            return;
        }

        TeleportVoidModuleClock.startDeploy(player, slot, stats);
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) return null;

        int moduleLevel = Math.max(1, data.level());
        int extraLevel = moduleLevel - 1;
        int cooldownLevel = 0;
        int speedLevel = 0;
        int activeDurationLevel = 0;
        List<ModuleModifierData> modifiers = data.modifiers();

        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == null) continue;
            if (modifierType == COOLDOWN_REDUCTION) {
                cooldownLevel += modifier.level();
            }
            if (modifierType == SPEED_BOOST) {
                speedLevel += modifier.level();
            }
            if (modifierType == ACTIVE_DURATION) {
                activeDurationLevel += modifier.level();
            }
        }

        float cooldownReduction = addLess(1.0F, cooldownLevel, 0.15F);
        float energyReduction = addLess(1.0F, cooldownLevel, 0.10F);
        long cooldownTicks = Math.round(BURST_COOLDOWN_TICKS / cooldownReduction);
        long energyCost = Math.round(BURST_ENERGY_COST / energyReduction);
        double deployEnergy = BASE_DEPLOY_ENERGY
                + DEPLOY_ENERGY_PER_LEVEL * extraLevel
                + DEPLOY_ENERGY_PER_DURATION_LEVEL * activeDurationLevel;
        int gateTicks = BASE_GATE_TICKS
                + GATE_TICKS_PER_LEVEL * extraLevel
                + GATE_TICKS_PER_DURATION_LEVEL * activeDurationLevel;
        float deploySpeed = BASE_DEPLOY_SPEED + DEPLOY_SPEED_PER_SPEED_LEVEL * speedLevel;
        float blocksPerSecond = BASE_BLOCKS_PER_SECOND + BLOCKS_PER_SECOND_PER_SPEED_LEVEL * speedLevel;

        return new Stats(
                data.moduleMode(),
                moduleLevel,
                cooldownTicks,
                energyCost,
                deployEnergy,
                gateTicks,
                deploySpeed,
                blocksPerSecond
        );
    }

    @Override
    public ModuleInputMode getInputMode() {
        return ModuleInputMode.CLICK;
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == BURST;
    }

    public record Stats(
            ModuleMode mode,
            int level,
            long burstCooldownTicks,
            long burstEnergyCost,
            double deployEnergy,
            int gateTicks,
            float deploySpeed,
            float blocksPerSecond
    ) {
    }
}
