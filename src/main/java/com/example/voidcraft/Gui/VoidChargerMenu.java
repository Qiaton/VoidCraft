package com.example.voidcraft.Gui;

import com.example.voidcraft.Block.ModBlock;
import com.example.voidcraft.Block.Block.VoidChargerBlock;
import com.example.voidcraft.Block.entity.VoidChargerBlockEntity;
import com.example.voidcraft.ModMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

public class VoidChargerMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 194;
    public static final int PANEL_WIDTH = IMAGE_WIDTH;
    public static final int PANEL_HEIGHT = 108;
    public static final int SLOT_GRID_CENTER_X = IMAGE_WIDTH / 2;
    public static final int PLAYER_INVENTORY_Y = 112;
    public static final int PLAYER_HOTBAR_Y = 170;

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GRID_COLUMNS = 3;

    private final Container chargerContainer;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final ContainerData data;
    private final Block validBlock;

    public VoidChargerMenu(int id, Inventory playerInventory) {
        this(
                id,
                playerInventory,
                new SimpleContainer(VoidChargerBlock.getConfig(ModBlock.LOW_VOID_CHARGER.get()).slotCount()),
                ContainerLevelAccess.NULL,
                BlockPos.ZERO,
                new SimpleContainerData(VoidChargerBlockEntity.DATA_COUNT),
                ModBlock.LOW_VOID_CHARGER.get()
        );
    }

    public VoidChargerMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(id, playerInventory, createClientContext(playerInventory, data));
    }

    private VoidChargerMenu(int id, Inventory playerInventory, ClientContext context) {
        this(id, playerInventory, context.container(), context.access(), context.blockPos(), context.data(), context.validBlock());
    }

    public VoidChargerMenu(
            int id,
            Inventory playerInventory,
            Container chargerContainer,
            ContainerLevelAccess access,
            BlockPos blockPos,
            ContainerData data,
            Block validBlock
    ) {
        super(ModMenuType.VOID_CHARGER_MENU.get(), id);
        checkContainerSize(chargerContainer, getChargerSlotCount(validBlock));
        checkContainerDataCount(data, VoidChargerBlockEntity.DATA_COUNT);
        this.chargerContainer = chargerContainer;
        this.access = access;
        this.blockPos = blockPos;
        this.data = data;
        this.validBlock = validBlock;

        for (int slot = 0; slot < getChargerSlotCount(); slot++) {
            this.addSlot(new VoidChargerSlot(
                    chargerContainer,
                    slot,
                    getChargerSlotX(slot, getChargerSlotCount()),
                    getChargerSlotY(slot, getChargerSlotCount())
            ));
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public int getEnergyStored() {
        return this.data.get(0);
    }

    public int getEnergyCapacity() {
        return (int) VoidChargerBlock.getConfig(this.validBlock).cacheCapacity();
    }

    public boolean isRunning() {
        return this.data.get(1) != 0;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack originalStack = sourceStack.copy();
        int chargerSlotCount = getChargerSlotCount();
        int playerInventoryStart = chargerSlotCount;
        int playerInventoryEnd = playerInventoryStart + 27;
        int playerHotbarEnd = playerInventoryEnd + 9;

        if (index < chargerSlotCount) {
            if (!moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInventoryStart && index < playerHotbarEnd) {
            if (!moveItemStackTo(sourceStack, 0, chargerSlotCount, false)) {
                if (index < playerInventoryEnd) {
                    if (!moveItemStackTo(sourceStack, playerInventoryEnd, playerHotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else {
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
        return stillValid(this.access, player, this.validBlock);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slot = row * 9 + col + 9;
                int x = 8 + col * 18;
                int y = PLAYER_INVENTORY_Y + row * 18;
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

    private static ClientContext createClientContext(Inventory playerInventory, RegistryFriendlyByteBuf data) {
        if (data == null) {
            return fallbackClientContext();
        }

        BlockPos blockPos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof VoidChargerBlockEntity charger) {
            return new ClientContext(
                    charger,
                    ContainerLevelAccess.create(playerInventory.player.level(), blockPos),
                    blockPos,
                    charger.getMenuData(),
                    charger.getBlockState().getBlock()
            );
        }

        Block block = playerInventory.player.level().getBlockState(blockPos).getBlock();
        Block validBlock = VoidChargerBlock.isVoidChargerBlock(block)
                ? block
                : ModBlock.LOW_VOID_CHARGER.get();

        return new ClientContext(
                new SimpleContainer(getChargerSlotCount(validBlock)),
                ContainerLevelAccess.create(playerInventory.player.level(), blockPos),
                blockPos,
                new SimpleContainerData(VoidChargerBlockEntity.DATA_COUNT),
                validBlock
        );
    }

    private static ClientContext fallbackClientContext() {
        return new ClientContext(
                new SimpleContainer(VoidChargerBlock.getConfig(ModBlock.LOW_VOID_CHARGER.get()).slotCount()),
                ContainerLevelAccess.NULL,
                BlockPos.ZERO,
                new SimpleContainerData(VoidChargerBlockEntity.DATA_COUNT),
                ModBlock.LOW_VOID_CHARGER.get()
        );
    }

    public int getChargerSlotCount() {
        return getChargerSlotCount(this.validBlock);
    }

    public static int getChargerSlotX(int slot, int slotCount) {
        int columns = getChargerGridColumns(slotCount);
        int startX = SLOT_GRID_CENTER_X - columns * SLOT_SIZE / 2;
        return startX + slot % columns * SLOT_SIZE;
    }

    public static int getChargerSlotY(int slot, int slotCount) {
        int row = slot / getChargerGridColumns(slotCount);
        int rows = getChargerGridRows(slotCount);
        int startY = switch (rows) {
            case 1 -> 55;
            case 2 -> 48;
            default -> 42;
        };
        return startY + row * SLOT_SIZE;
    }

    private static int getChargerSlotCount(Block block) {
        return VoidChargerBlock.getConfig(block).slotCount();
    }

    private static int getChargerGridColumns(int slotCount) {
        return Math.min(SLOT_GRID_COLUMNS, Math.max(1, slotCount));
    }

    private static int getChargerGridRows(int slotCount) {
        return (slotCount + getChargerGridColumns(slotCount) - 1) / getChargerGridColumns(slotCount);
    }

    private record ClientContext(Container container, ContainerLevelAccess access, BlockPos blockPos, ContainerData data, Block validBlock) {
    }
}
