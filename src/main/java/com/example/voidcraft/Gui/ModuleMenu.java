package com.example.voidcraft.Gui;

import com.example.voidcraft.Container.GauntletModuleContainer;
import com.example.voidcraft.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import static com.example.voidcraft.Item.custom.PhaseGauntlet.GAUNTLET_MODULE_SLOT_COUNT;


public class ModuleMenu extends AbstractContainerMenu{
    private final Container moduleContainer;
    public ModuleMenu(int id, Inventory inventory) {
        this(id,inventory,new SimpleContainer(GAUNTLET_MODULE_SLOT_COUNT));
    }
    public ModuleMenu(int id, Inventory playerInventory,Container moduleContainer) {
        super(ModMenuType.MODULE_MENU.get(), id);
        this.moduleContainer = moduleContainer;
        checkContainerSize(moduleContainer, GAUNTLET_MODULE_SLOT_COUNT);
        int startX = 44;
        int y = 20;

        for (int slot = 0; slot < GAUNTLET_MODULE_SLOT_COUNT; slot++) {
            this.addSlot(new ModuleSlot(moduleContainer, slot, startX + slot * 18, y));
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slot = row * 9 + col+9;
                int x = 8 + col * 18;
                int y = 51 + row * 18;
                this.addSlot(new Slot(playerInventory,slot,x,y));
            }
            }
        }


    private void addPlayerHotbar(Inventory playerInventory) {
    for(int col = 0; col < 9; ++col){
        int x = 8 + col * 18;
        int y = 109;
        this.addSlot(new Slot(playerInventory,col,x,y));
    }
}

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }
}