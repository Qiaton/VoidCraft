package com.example.voidcraft.Gui;

import com.example.voidcraft.Block.ModBlock;
import com.example.voidcraft.Block.entity.VoidEnergyConverterBlockEntity;
import com.example.voidcraft.ModMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

public class VoidEnergyConverterMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 276;
    public static final int IMAGE_HEIGHT = 166;
    public static final int PANEL_WIDTH = IMAGE_WIDTH;
    public static final int PANEL_HEIGHT = IMAGE_HEIGHT;

    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final ContainerData data;

    public VoidEnergyConverterMenu(int id, Inventory playerInventory) {
        this(
                id,
                playerInventory,
                ContainerLevelAccess.NULL,
                BlockPos.ZERO,
                new SimpleContainerData(VoidEnergyConverterBlockEntity.DATA_COUNT)
        );
    }

    public VoidEnergyConverterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(id, playerInventory, createClientContext(playerInventory, data));
    }

    private VoidEnergyConverterMenu(int id, Inventory playerInventory, ClientContext context) {
        this(id, playerInventory, context.access(), context.blockPos(), context.data());
    }

    public VoidEnergyConverterMenu(
            int id,
            Inventory playerInventory,
            ContainerLevelAccess access,
            BlockPos blockPos,
            ContainerData data
    ) {
        super(ModMenuType.VOID_ENERGY_CONVERTER_MENU.get(), id);
        checkContainerDataCount(data, VoidEnergyConverterBlockEntity.DATA_COUNT);
        this.access = access;
        this.blockPos = blockPos;
        this.data = data;
        addDataSlots(data);
    }

    public int getEnergyStored() {
        return this.data.get(0);
    }

    public int getEnergyCapacity() {
        return Math.max(1, this.data.get(1));
    }

    public boolean isRunning() {
        return this.data.get(2) != 0;
    }

    public int getInputCount() {
        return this.data.get(3);
    }

    public int getOutputCount() {
        return this.data.get(4);
    }

    public int getMaxInputCount() {
        return this.data.get(5);
    }

    public int getMaxOutputCount() {
        return this.data.get(6);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return stillValid(this.access, player, ModBlock.VOID_ENERGY_CONVERTER.get());
    }

    private static ClientContext createClientContext(Inventory playerInventory, RegistryFriendlyByteBuf data) {
        if (data == null) {
            return fallbackClientContext();
        }

        BlockPos blockPos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof VoidEnergyConverterBlockEntity converter) {
            return new ClientContext(
                    ContainerLevelAccess.create(playerInventory.player.level(), blockPos),
                    blockPos,
                    converter.getMenuData()
            );
        }

        return new ClientContext(
                ContainerLevelAccess.create(playerInventory.player.level(), blockPos),
                blockPos,
                new SimpleContainerData(VoidEnergyConverterBlockEntity.DATA_COUNT)
        );
    }

    private static ClientContext fallbackClientContext() {
        return new ClientContext(
                ContainerLevelAccess.NULL,
                BlockPos.ZERO,
                new SimpleContainerData(VoidEnergyConverterBlockEntity.DATA_COUNT)
        );
    }

    private record ClientContext(
            ContainerLevelAccess access,
            BlockPos blockPos,
            ContainerData data
    ) {
    }
}
