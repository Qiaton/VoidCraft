package com.example.voidcraft.Gui;

import com.example.voidcraft.Item.custom.EnergyCoreItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import static com.example.voidcraft.Item.custom.PhaseWatch.WATCH_CONTAINER_SLOT_COUNT;
import static com.example.voidcraft.Item.custom.PhaseWatch.WATCH_CORE_SLOT;
import static com.example.voidcraft.Item.custom.PhaseWatch.WATCH_MODULE_SLOT_COUNT;


public class ModuleMenu extends AbstractContainerMenu{
    public static final int WATCH_SLOT_START_X = 61;
    public static final int WATCH_SLOT_Y = 20;
    public static final int WATCH_SLOT_SPACING = 18;
    private static final int PLAYER_SLOT_START = WATCH_CONTAINER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_SLOT_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    public ModuleMenu(int id, Inventory inventory) {
        this(id,inventory,new SimpleContainer(WATCH_CONTAINER_SLOT_COUNT));
    }
    public ModuleMenu(int id, Inventory playerInventory,Container moduleContainer) {
        super(ModMenuType.registerModuleMenu.get(), id);
        checkContainerSize(moduleContainer, WATCH_CONTAINER_SLOT_COUNT);

        for (int slot = 0; slot < WATCH_MODULE_SLOT_COUNT; slot++) {
            this.addSlot(new ModuleSlot(moduleContainer, slot, WATCH_SLOT_START_X + slot * WATCH_SLOT_SPACING, WATCH_SLOT_Y));
        }
        this.addSlot(new EnergyCoreSlot(moduleContainer, WATCH_CORE_SLOT, WATCH_SLOT_START_X + WATCH_CORE_SLOT * WATCH_SLOT_SPACING, WATCH_SLOT_Y));

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
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack originalStack = sourceStack.copy();

        if (index < WATCH_CONTAINER_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof ModuleItem) {
            if (!moveItemStackTo(sourceStack, 0, WATCH_MODULE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof EnergyCoreItem) {
            if (!moveItemStackTo(sourceStack, WATCH_CORE_SLOT, WATCH_CORE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return originalStack;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }
}
