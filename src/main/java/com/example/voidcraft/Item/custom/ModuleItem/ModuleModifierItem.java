package com.example.voidcraft.Item.custom.ModuleItem;

import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Consumer;

public class ModuleModifierItem extends Item {
    public ModuleModifierItem(Properties properties) {
        super(properties);
    }

    public static void setData(ItemStack stack, ModuleModifierData data) {
        stack.set(ModDataComponents.MODULE_MODIFIER_DATA.get(), data);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(),
                List.of(),
                List.of(data.type().getId()),
                List.of()
        ));
    }

    public static void syncData(ItemStack stack) {
        ModuleModifierData data = stack.get(ModDataComponents.MODULE_MODIFIER_DATA.get());
        if (data == null || data.type() == null) {
            return;
        }

        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null && modelData.strings().contains(data.type().getId())) {
            return;
        }

        setData(stack, data);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        syncData(stack);
        super.inventoryTick(stack, level, entity, slot);
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
