package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.BatteryBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;

public class BatteryBlockItem extends BlockItem {
    public BatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable(
                "tooltip.void_craft.battery_block.energy",
                getStoredEnergy(stack),
                BatteryBlockEntity.CAPACITY
        ).withStyle(ChatFormatting.GRAY));
    }

    private static long getStoredEnergy(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.type() != ModBlockEntities.BATTERY_BLOCK_ENTITY.get()) {
            return BatteryBlockEntity.DEFAULT_ENERGY;
        }
        return Math.max(0L, Math.min(BatteryBlockEntity.CAPACITY, data.copyTagWithoutId().getLongOr("VoidEnergy", BatteryBlockEntity.DEFAULT_ENERGY)));
    }
}
