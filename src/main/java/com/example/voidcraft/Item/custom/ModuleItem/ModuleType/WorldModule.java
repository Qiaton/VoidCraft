package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.World.GoWorld;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.voidcraft.Item.custom.ModuleItem.ModuleMode.BURST;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType.*;
import static com.example.voidcraft.Item.custom.ModuleItem.ModuleStatHelper.addLess;

public class WorldModule extends ModuleItem {
    private static final long ENERGY_COST = 900L;
    private static final long COOLDOWN_TICKS = 600L;

    public WorldModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void doUseSkill(ServerPlayer player, ItemStack watchStack, ItemStack moduleStack, int slot) {
        Stats stats = getStats(moduleStack);
        if (stats == null || stats.mode() != BURST) {
            return;
        }
        if (!ModuleSkillClock.canUseNow(player, slot)) {
            long energyCost = stats.offEnergy();
            if(!(ModuleSkillClock.tryUseEnergy(player,energyCost))){
                return;
            }
        }
        if (!GoWorld.canGo(player)) {
            return;
        }
        if (!GoWorld.goWorld(player)) {
            return;
        }
        ModuleSkillClock.setCooldown(player, slot, stats.cooldownTicks());
    }

    public static Stats getStats(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (data == null) {
            return null;
        }

        float cooldownReduction = 1.0F + data.level() * 0.1F;
        float offEnergy = 1.0F + data.level() * 0.1F;
        List<ModuleModifierData> modifiers = data.modifiers();
        for (ModuleModifierData modifier : modifiers) {
            ModuleModifierType modifierType = modifier.type();
            if (modifierType == COOLDOWN_REDUCTION) {
                cooldownReduction = addLess(cooldownReduction, modifier.level(), 0.15F);
            }
            if(modifierType == ACTIVE_DURATION){
                offEnergy = addLess(offEnergy, modifier.level(), 0.2F);
            }
            if(modifierType == SPEED_BOOST){
                offEnergy = addLess(offEnergy, modifier.level(), 0.2F);
            }

        }

        return new Stats(data.moduleMode(), cooldownReduction,(long)(ENERGY_COST/offEnergy));
    }

    @Override
    public boolean canUseMode(ModuleMode mode) {
        return mode == BURST;
    }

    @Override
    protected Component getModifierDisplayName(ModuleModifierData modifierData) {
        ModuleModifierType modifierType = modifierData.type();
        if (modifierType == ACTIVE_DURATION || modifierType == SPEED_BOOST) {
            return Component.translatable("module_modifier_type.void_craft.energy_reduction");
        }

        return super.getModifierDisplayName(modifierData);
    }

    public record Stats(ModuleMode mode, float cooldownReduction,long offEnergy) {

        public long cooldownTicks() {
            return Math.max(0L, (long) (COOLDOWN_TICKS / Math.max(0.001F, cooldownReduction)));
        }
    }
}
