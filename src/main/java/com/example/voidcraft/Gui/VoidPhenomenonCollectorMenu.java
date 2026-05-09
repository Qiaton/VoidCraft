package com.example.voidcraft.Gui;

import com.example.voidcraft.Block.ModBlock;
import com.example.voidcraft.Block.Block.VoidPhenomenonCollectorBlock;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Block.entity.VoidPhenomenonCollectorBlockEntity;
import com.example.voidcraft.Item.custom.VoidCrystalItem;
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

import java.util.List;

public class VoidPhenomenonCollectorMenu extends AbstractContainerMenu {
    // 这些尺寸和槽位同时给 Screen 使用，避免菜单和界面坐标各写一套。
    public static final int IMAGE_WIDTH = 176;
    public static final int PANEL_WIDTH = IMAGE_WIDTH;
    public static final int PANEL_HEIGHT = 108;
    public static final int CRYSTAL_GRID_CENTER_X = 48;
    public static final int PLAYER_INVENTORY_Y = 112;
    public static final int PLAYER_HOTBAR_Y = 170;

    private static final int CRYSTAL_GRID_COLUMNS = 3;
    private static final int CRYSTAL_SLOT_SIZE = 18;

    private final Container collectorContainer;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final ContainerData data;
    private final Block validBlock;
    // true 时显示结晶槽，false 时切到连接页并隐藏结晶槽。
    private boolean overviewPage = true;

    public VoidPhenomenonCollectorMenu(int id, Inventory playerInventory) {
        this(
                id,
                playerInventory,
                new SimpleContainer(VoidPhenomenonCollectorBlock.getConfig(ModBlock.VOID_PHENOMENON_COLLECTOR.get()).crystalSlotCount()),
                ContainerLevelAccess.NULL,
                BlockPos.ZERO,
                new SimpleContainerData(VoidPhenomenonCollectorBlockEntity.DATA_COUNT),
                ModBlock.VOID_PHENOMENON_COLLECTOR.get()
        );
    }

    public VoidPhenomenonCollectorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        // 客户端从服务端发来的 BlockPos 找回本地 BlockEntity，用它读取同步数据。
        this(id, playerInventory, createClientContext(playerInventory, data));
    }

    private VoidPhenomenonCollectorMenu(int id, Inventory playerInventory, ClientContext context) {
        this(id, playerInventory, context.container(), context.access(), context.blockPos(), context.data(), context.validBlock());
    }

    public VoidPhenomenonCollectorMenu(
            int id,
            Inventory playerInventory,
            Container collectorContainer,
            ContainerLevelAccess access,
            BlockPos blockPos,
            ContainerData data,
            Block validBlock
    ) {
        super(ModMenuType.VOID_PHENOMENON_COLLECTOR_MENU.get(), id);
        checkContainerSize(collectorContainer, getCrystalSlotCount(validBlock));
        checkContainerDataCount(data, VoidPhenomenonCollectorBlockEntity.DATA_COUNT);
        this.collectorContainer = collectorContainer;
        this.access = access;
        this.blockPos = blockPos;
        this.data = data;
        this.validBlock = validBlock;

        for (int slot = 0; slot < getCrystalSlotCount(); slot++) {
            this.addSlot(new VoidCrystalSlot(
                    collectorContainer,
                    slot,
                    getCrystalSlotX(slot, getCrystalSlotCount()),
                    getCrystalSlotY(slot, getCrystalSlotCount()),
                    // 页签切换时 Slot 仍在菜单里，但客户端不显示也不能交互。
                    () -> this.overviewPage
            ));
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public void setOverviewPage(boolean overviewPage) {
        this.overviewPage = overviewPage;
    }

    public int getEnergyStored() {
        return this.data.get(0);
    }

    public int getEnergyCapacity() {
        return (int) VoidPhenomenonCollectorBlock.getConfig(this.validBlock).cacheCapacity();
    }

    public boolean isRunning() {
        return this.data.get(1) != 0;
    }

    public int getOutputCount() {
        return this.data.get(2);
    }

    public int getMaxOutputCount() {
        return this.data.get(3);
    }

    public long getEnergyPerTick() {
        // GUI 直接按所有结晶槽里的倍率显示当前每 tick 总产能。
        long total = 0L;
        int slots = Math.min(getCrystalSlotCount(), this.collectorContainer.getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack crystalStack = this.collectorContainer.getItem(slot);
            if (crystalStack.getItem() instanceof VoidCrystalItem crystalItem) {
                total += VoidPhenomenonCollectorBlock.getConfig(this.validBlock).baseGenerationPerTick() * crystalItem.getGenerationMultiplier();
            }
        }
        return total;
    }

    public String getTierDisplayName() {
        return VoidPhenomenonCollectorBlock.getConfig(this.validBlock).tierDisplayName();
    }

    public List<VoidEnergyBinding> getOutputTargets() {
        // 服务端菜单能直接读 BlockEntity；客户端没有完整列表时返回空列表。
        if (this.collectorContainer instanceof VoidPhenomenonCollectorBlockEntity collector) {
            return List.copyOf(collector.getOutputTargets());
        }
        return List.of();
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        // Shift 点击：发电机槽和玩家背包之间移动，只有结晶能进发电机槽。
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack originalStack = sourceStack.copy();
        int collectorSlotCount = getCrystalSlotCount();
        int playerInventoryStart = collectorSlotCount;
        int playerInventoryEnd = playerInventoryStart + 27;
        int playerHotbarEnd = playerInventoryEnd + 9;

        if (index < collectorSlotCount) {
            if (!moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.slots.get(0).mayPlace(sourceStack)) {
            if (!moveItemStackTo(sourceStack, 0, collectorSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInventoryStart && index < playerInventoryEnd) {
            if (!moveItemStackTo(sourceStack, playerInventoryEnd, playerHotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInventoryEnd && index < playerHotbarEnd) {
            if (!moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
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
        // 背包坐标沿用原版 3x9 排列。
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
        // 快捷栏单独放在最下方。
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18;
            this.addSlot(new Slot(playerInventory, col, x, PLAYER_HOTBAR_Y));
        }
    }

    private static ClientContext createClientContext(Inventory playerInventory, RegistryFriendlyByteBuf data) {
        if (data == null) {
            return fallbackClientContext();
        }

        // 服务端打开菜单时会把方块坐标写进 buffer，客户端用它定位本地方块实体。
        BlockPos blockPos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof VoidPhenomenonCollectorBlockEntity collector) {
            return new ClientContext(
                    collector,
                    ContainerLevelAccess.create(playerInventory.player.level(), blockPos),
                    blockPos,
                    collector.getMenuData(),
                    collector.getBlockState().getBlock()
            );
        }

        Block block = playerInventory.player.level().getBlockState(blockPos).getBlock();
        Block validBlock = VoidPhenomenonCollectorBlock.isCollectorBlock(block)
                ? block
                : ModBlock.VOID_PHENOMENON_COLLECTOR.get();

        return new ClientContext(
                new SimpleContainer(getCrystalSlotCount(validBlock)),
                ContainerLevelAccess.create(playerInventory.player.level(), blockPos),
                blockPos,
                new SimpleContainerData(VoidPhenomenonCollectorBlockEntity.DATA_COUNT),
                validBlock
        );
    }

    private static ClientContext fallbackClientContext() {
        // 找不到方块实体时仍创建空容器，避免客户端菜单构造崩溃。
        return new ClientContext(
                new SimpleContainer(VoidPhenomenonCollectorBlock.getConfig(ModBlock.VOID_PHENOMENON_COLLECTOR.get()).crystalSlotCount()),
                ContainerLevelAccess.NULL,
                BlockPos.ZERO,
                new SimpleContainerData(VoidPhenomenonCollectorBlockEntity.DATA_COUNT),
                ModBlock.VOID_PHENOMENON_COLLECTOR.get()
        );
    }

    public int getCrystalSlotCount() {
        return getCrystalSlotCount(this.validBlock);
    }

    public static int getCrystalSlotX(int slot, int slotCount) {
        int columns = getCrystalGridColumns(slotCount);
        int startX = CRYSTAL_GRID_CENTER_X - columns * CRYSTAL_SLOT_SIZE / 2;
        return startX + slot % columns * CRYSTAL_SLOT_SIZE;
    }

    public static int getCrystalSlotY(int slot, int slotCount) {
        int row = slot / getCrystalGridColumns(slotCount);
        int rows = getCrystalGridRows(slotCount);
        int startY = switch (rows) {
            case 1 -> 54;
            case 2 -> 52;
            default -> 50;
        };
        return startY + row * CRYSTAL_SLOT_SIZE;
    }

    private static int getCrystalSlotCount(Block block) {
        return VoidPhenomenonCollectorBlock.getConfig(block).crystalSlotCount();
    }

    private static int getCrystalGridColumns(int slotCount) {
        return Math.min(CRYSTAL_GRID_COLUMNS, Math.max(1, slotCount));
    }

    private static int getCrystalGridRows(int slotCount) {
        return (slotCount + getCrystalGridColumns(slotCount) - 1) / getCrystalGridColumns(slotCount);
    }

    private record ClientContext(Container container, ContainerLevelAccess access, BlockPos blockPos, ContainerData data, Block validBlock) {
    }
}
