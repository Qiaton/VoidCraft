package com.example.voidcraft.Item.custom.ModuleItem;

import com.example.voidcraft.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ModuleModifierItem extends Item {
    public ModuleModifierItem(Properties properties) {
        super(properties);
    }
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        ModuleModifierData data = stack.getOrDefault(
                ModDataComponents.MODULE_MODIFIER_DATA.get(),
                new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,1)
        );
        tooltipAdder.accept(Component.translatable(
                "tooltip.void_craft.module_modifier.effect",
                data.type().getDisplayName()
        ));
        tooltipAdder.accept(Component.translatable(
                "tooltip.void_craft.module_modifier.level",
                data.level()
        ));


    }
}
