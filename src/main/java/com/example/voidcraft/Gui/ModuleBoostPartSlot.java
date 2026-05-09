package com.example.voidcraft.Gui;

import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleBoostPartSlot extends Slot {
    private final ModuleBoostMenu menu;

    public ModuleBoostPartSlot(ModuleBoostMenu menu, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        ModuleModifierData data = this.menu.getPartData(stack);
        if (data == null) {
            return false;
        }
        return !this.menu.hasSameType(data.type(), this.getContainerSlot());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
