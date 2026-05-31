package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Custom.Clock.Clock;
import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class HealthVoidModule extends VoidModule {
    private static final float COST_MULTIPLIER = 1.5F;
    private static final float COOLDOWN_MULTIPLIER = 1.5F;
    private static final float HEALTH_DEFAULT = 0.08F;
    private UUID uuid = null;

    public HealthVoidModule(Properties properties) {
        super(properties);
    }

    @Override
    protected float getCostMultiplier(ItemStack moduleStack) {
        return super.getCostMultiplier(moduleStack) * COST_MULTIPLIER;
    }

    @Override
    protected float getCooldownMultiplier(ItemStack moduleStack) {
        return super.getCooldownMultiplier(moduleStack) * COOLDOWN_MULTIPLIER;
    }

    @Override
    protected void onBurstStart(ServerPlayer player, ItemStack moduleStack, int slot, Stats stats, int activeTicks) {
        player.heal(getHealAmount(moduleStack));
        ModuleSkillClock.startChannel(player, slot, 0);
        uuid = Clock.addClock(activeTicks, () -> {
            ModuleSkillClock.stopChannel(player, slot);
            uuid = null;
        });
    }

    @Override
    protected void onBurstStop(ServerPlayer player, ItemStack moduleStack, int slot) {
        ModuleSkillClock.stopChannel(player, slot);
        if(uuid != null){
            Clock.removeClock(uuid);
            uuid = null;
        }
    }

    public static VoidModule.Stats getStats(ItemStack moduleStack) {
        return VoidModule.getStats(moduleStack);
    }

    public static float getHealAmount(ItemStack moduleStack) {
        ModuleData data = moduleStack.get(ModDataComponents.MODULE_DATA);
        if(data == null) return 0F;

        return data.level() * HEALTH_DEFAULT;
    }
}
