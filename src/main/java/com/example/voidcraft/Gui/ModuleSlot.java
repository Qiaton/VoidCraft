package com.example.voidcraft.Gui;

import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleSlot extends Slot {

    public ModuleSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);

    }
    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof ModuleItem;
    }
    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
