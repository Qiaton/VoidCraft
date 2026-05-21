package com.example.voidcraft.Item.custom.ModuleItem;

import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class ModuleModifierItem extends Item {
    public ModuleModifierItem(Properties properties) {
        super(properties);
    }

    public static void setData(ItemStack stack, ModuleModifierData data) {
        stack.set(ModDataComponents.MODULE_MODIFIER_DATA.get(), data);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(getModelData(data.type())));
    }

    public static void syncData(ItemStack stack) {
        ModuleModifierData data = stack.get(ModDataComponents.MODULE_MODIFIER_DATA.get());
        if (data == null || data.type() == null) {
            return;
        }

        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null && modelData.value() == getModelData(data.type())) {
            return;
        }

        setData(stack, data);
    }

    private static int getModelData(ModuleModifierType type) {
        return type.ordinal() + 1;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        syncData(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        ModuleModifierData data = stack.getOrDefault(
                ModDataComponents.MODULE_MODIFIER_DATA.get(),
                new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,1)
        );
        tooltipAdder.add(Component.translatable(
                "tooltip.void_craft.module_modifier.effect",
                data.type().getDisplayName()
        ));
        tooltipAdder.add(Component.translatable(
                "tooltip.void_craft.module_modifier.level",
                data.level()
        ));


    }
}
