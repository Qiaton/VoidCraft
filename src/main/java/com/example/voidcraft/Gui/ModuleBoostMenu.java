package com.example.voidcraft.Gui;

import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierType;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.ModMenuType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleBoostMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 166;
    public static final int TOP_HEIGHT = 78;
    public static final int PLAYER_INV_Y = 84;
    public static final int PLAYER_HOTBAR_Y = 142;

    public static final int MODULE_SLOT = 0;
    public static final int PART_SLOT_START = 1;
    public static final int PART_SLOT_COUNT = 3;
    public static final int INPUT_SLOT_COUNT = 4;
    public static final int RESULT_SLOT = 4;
    public static final int PLAYER_SLOT_START = 5;

    public static final int PART_SLOT_X = 18;
    public static final int PART_SLOT_Y = 24;
    public static final int PART_SLOT_STEP = 18;
    public static final int MODULE_SLOT_X = 36;
    public static final int MODULE_SLOT_Y = 47;
    public static final int RESULT_SLOT_X = 139;
    public static final int RESULT_SLOT_Y = 31;

    private final BoostContainer input;
    private final Container result = new SimpleContainer(1);
    private final ModuleModifierData[] pending = new ModuleModifierData[PART_SLOT_COUNT];
    private boolean lockChange;

    public ModuleBoostMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new BoostContainer(INPUT_SLOT_COUNT));
    }

    public ModuleBoostMenu(int id, Inventory playerInventory, BoostContainer input) {
        super(ModMenuType.MODULE_BOOST_MENU.get(), id);
        checkContainerSize(input, INPUT_SLOT_COUNT);
        this.input = input;
        this.input.setMenu(this);

        this.addSlot(new ModuleSlot(input, MODULE_SLOT, MODULE_SLOT_X, MODULE_SLOT_Y));
        for (int slot = 0; slot < PART_SLOT_COUNT; slot++) {
            this.addSlot(new ModuleBoostPartSlot(
                    this,
                    input,
                    PART_SLOT_START + slot,
                    PART_SLOT_X + slot * PART_SLOT_STEP,
                    PART_SLOT_Y
            ));
        }
        this.addSlot(new ModuleBoostResultSlot(this, result, 0, RESULT_SLOT_X, RESULT_SLOT_Y));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        slotsChanged(this.input);
    }

    @Override
    public void slotsChanged(@NonNull Container container) {
        if (this.lockChange || container != this.input) {
            return;
        }

        ItemStack resultStack = makeResult(this.pending);
        this.result.setItem(0, resultStack);
        broadcastChanges();
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack originalStack = sourceStack.copy();
        int playerSlotEnd = PLAYER_SLOT_START + 36;

        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START, playerSlotEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(sourceStack, originalStack);
            slot.onTake(player, sourceStack);
        } else if (index < PLAYER_SLOT_START) {
            if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START, playerSlotEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof ModuleItem) {
            if (!moveItemStackTo(sourceStack, MODULE_SLOT, MODULE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (getPartData(sourceStack) != null) {
            if (!moveItemStackTo(sourceStack, PART_SLOT_START, PART_SLOT_START + PART_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_SLOT_START + 27) {
            if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START + 27, playerSlotEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START, PLAYER_SLOT_START + 27, false)) {
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

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        if (player.level().isClientSide()) {
            return;
        }

        this.lockChange = true;
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack stack = this.input.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        this.lockChange = false;
        this.result.setItem(0, ItemStack.EMPTY);
    }

    public boolean hasResult() {
        return this.result.getItem(0).isEmpty() == false;
    }

    public void takeResult(Player player, ItemStack stack) {
        ModuleModifierData[] nowPending = new ModuleModifierData[PART_SLOT_COUNT];
        ItemStack nowResult = makeResult(nowPending);
        if (nowResult.isEmpty()) {
            return;
        }

        this.lockChange = true;
        this.input.removeItem(MODULE_SLOT, 1);
        for (int slot = 0; slot < PART_SLOT_COUNT; slot++) {
            if (nowPending[slot] != null) {
                this.input.removeItem(PART_SLOT_START + slot, 1);
            }
        }
        this.lockChange = false;
        this.result.setItem(0, ItemStack.EMPTY);
        slotsChanged(this.input);
    }

    public boolean hasSameType(ModuleModifierType type, int skipSlot) {
        if (type == null) {
            return false;
        }

        for (int slot = 0; slot < PART_SLOT_COUNT; slot++) {
            int inputSlot = PART_SLOT_START + slot;
            if (inputSlot == skipSlot) {
                continue;
            }

            ModuleModifierData data = getPartData(this.input.getItem(inputSlot));
            if (data != null && data.type() == type) {
                return true;
            }
        }
        return false;
    }

    ModuleModifierData getPartData(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ModuleModifierItem)) {
            return null;
        }

        ModuleModifierData data = stack.get(ModDataComponents.MODULE_MODIFIER_DATA.get());
        if (data == null || data.type() == null) {
            return null;
        }
        return data;
    }

    private ItemStack makeResult(ModuleModifierData[] targetPending) {
        clearPending(targetPending);

        ItemStack moduleStack = this.input.getItem(MODULE_SLOT);
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof ModuleItem)) {
            return ItemStack.EMPTY;
        }

        ModuleData moduleData = moduleStack.get(ModDataComponents.MODULE_DATA.get());
        if (moduleData == null || hasRepeat(moduleData.modifiers())) {
            return ItemStack.EMPTY;
        }

        boolean hasPart = false;
        for (int slot = 0; slot < PART_SLOT_COUNT; slot++) {
            ModuleModifierData data = getPartData(this.input.getItem(PART_SLOT_START + slot));
            if (data == null) {
                continue;
            }
            targetPending[slot] = data;
            hasPart = true;
        }

        if (!hasPart) {
            return ItemStack.EMPTY;
        }

        List<ModuleModifierData> newModifiers = new ArrayList<>(moduleData.modifiers());
        for (ModuleModifierData data : targetPending) {
            if (data == null) {
                continue;
            }

            int oldIndex = findType(newModifiers, data.type());
            if (oldIndex >= 0) {
                newModifiers.set(oldIndex, data);
            } else {
                newModifiers.add(data);
            }
        }

        ItemStack resultStack = moduleStack.copy();
        resultStack.setCount(1);
        resultStack.set(
                ModDataComponents.MODULE_DATA.get(),
                new ModuleData(moduleData.moduleMode(), moduleData.level(), newModifiers)
        );
        return resultStack;
    }

    private void clearPending(ModuleModifierData[] targetPending) {
        Arrays.fill(targetPending, null);
    }

    private boolean hasRepeat(List<ModuleModifierData> modifiers) {
        for (int i = 0; i < modifiers.size(); i++) {
            ModuleModifierType leftType = modifiers.get(i).type();
            if (leftType == null) {
                continue;
            }
            for (int j = i + 1; j < modifiers.size(); j++) {
                ModuleModifierType rightType = modifiers.get(j).type();
                if (leftType == rightType) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findType(List<ModuleModifierData> modifiers, ModuleModifierType type) {
        for (int i = 0; i < modifiers.size(); i++) {
            if (modifiers.get(i).type() == type) {
                return i;
            }
        }
        return -1;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slot = row * 9 + col + 9;
                int x = 8 + col * 18;
                int y = PLAYER_INV_Y + row * 18;
                this.addSlot(new Slot(playerInventory, slot, x, y));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18;
            this.addSlot(new Slot(playerInventory, col, x, PLAYER_HOTBAR_Y));
        }
    }

    public static class BoostContainer extends SimpleContainer {
        private ModuleBoostMenu menu;

        public BoostContainer(int size) {
            super(size);
        }

        public void setMenu(ModuleBoostMenu menu) {
            this.menu = menu;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (this.menu != null) {
                this.menu.slotsChanged(this);
            }
        }
    }
}
