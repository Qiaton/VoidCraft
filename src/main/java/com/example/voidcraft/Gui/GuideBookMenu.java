package com.example.voidcraft.Gui;

import com.example.voidcraft.ModMenuType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class GuideBookMenu extends AbstractContainerMenu {
    public GuideBookMenu(int id, Inventory inventory) {
        super(ModMenuType.GUIDE_BOOK_MENU.get(), id);
    }

    public GuideBookMenu(int id, Inventory inventory, Player player) {
        this(id, inventory);
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }
}
