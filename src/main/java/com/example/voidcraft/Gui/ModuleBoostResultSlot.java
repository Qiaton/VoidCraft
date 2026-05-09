package com.example.voidcraft.Gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleBoostResultSlot extends Slot {
    private final ModuleBoostMenu menu;

    public ModuleBoostResultSlot(ModuleBoostMenu menu, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.hasItem();
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.menu.takeResult(player, stack);
        super.onTake(player, stack);
    }
}
