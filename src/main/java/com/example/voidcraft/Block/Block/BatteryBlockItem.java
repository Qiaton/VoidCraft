package com.example.voidcraft.Block.Block;

import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.entity.BatteryBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class BatteryBlockItem extends BlockItem {
    public BatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.void_craft.battery_block.energy",
                getStoredEnergy(stack),
                BatteryBlockEntity.CAPACITY
        ).withStyle(ChatFormatting.GRAY));
    }

    private static long getStoredEnergy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return BatteryBlockEntity.DEFAULT_ENERGY;
        }
        CompoundTag tag = data.copyTag();
        String id = tag.getString("id");
        if (!id.isEmpty() && !id.equals(ModBlockEntities.BATTERY_BLOCK_ENTITY.getId().toString())) {
            return BatteryBlockEntity.DEFAULT_ENERGY;
        }
        long energy = tag.contains("VoidEnergy") ? tag.getLong("VoidEnergy") : BatteryBlockEntity.DEFAULT_ENERGY;
        return Math.max(0L, Math.min(BatteryBlockEntity.CAPACITY, energy));
    }
}
