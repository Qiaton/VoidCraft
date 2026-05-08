package com.example.voidcraft.Gui;

import com.example.voidcraft.Item.custom.VoidCrystalItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

public class VoidCrystalSlot extends Slot {
    // active 由菜单页签控制：切到连接页时隐藏结晶槽，避免界面元素重叠。
    private final BooleanSupplier active;

    public VoidCrystalSlot(Container container, int slot, int x, int y, BooleanSupplier active) {
        super(container, slot, x, y);
        this.active = active;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 这里只允许虚空结晶进入发电机。
        return stack.getItem() instanceof VoidCrystalItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return this.active.getAsBoolean();
    }
}
