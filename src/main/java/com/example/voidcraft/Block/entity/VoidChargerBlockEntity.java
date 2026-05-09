package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.VoidChargerBlock;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyProfile;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.Gui.VoidChargerMenu;
import com.example.voidcraft.Item.custom.EnergyCoreItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VoidChargerBlockEntity extends BlockEntity implements Container, MenuProvider, VoidEnergyTransferBlockEntity {
    public static final int DATA_COUNT = 2;
    public static final int MAX_INPUT_BINDINGS = 1;
    public static final int MAX_OUTPUT_BINDINGS = 0;
    public static final long ENERGY_PER_REPAIR = 20L;
    public static final long MAX_INSERT = VoidEnergyProfile.DEFAULT_MAX_INPUT_PER_TRANSFER;

    private static final int DATA_ENERGY_STORED = 0;
    private static final int DATA_RUNNING = 1;

    private long voidEnergy;
    private boolean running;
    private final NonNullList<ItemStack> items;
    private final List<VoidEnergyBinding> inputSources = new ArrayList<>();
    private final List<VoidEnergyBinding> outputTargets = new ArrayList<>();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_STORED -> (int) VoidChargerBlockEntity.this.voidEnergy;
                case DATA_RUNNING -> VoidChargerBlockEntity.this.running ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_ENERGY_STORED -> VoidChargerBlockEntity.this.voidEnergy = clampEnergy(value);
                case DATA_RUNNING -> VoidChargerBlockEntity.this.running = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public VoidChargerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VOID_CHARGER_BLOCK_ENTITY.get(), pos, blockState);
        this.items = NonNullList.withSize(VoidChargerBlock.getConfig(blockState).slotCount(), ItemStack.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VoidChargerBlockEntity charger) {
        if (level.isClientSide()) {
            return;
        }

        // 输入端不主动抢电；供能源会按输出列表公平分配给它。
        boolean charged = charger.chargeItems();
        charger.updateRunningState(charged);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.voidEnergy = clampEnergy(input.getLongOr("VoidEnergy", 0L));
        this.running = input.getBooleanOr("Running", false);

        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(input, this.items);

        this.inputSources.clear();
        for (ValueInput child : input.childrenListOrEmpty("InputSources")) {
            if (this.inputSources.size() >= MAX_INPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(child).ifPresent(this.inputSources::add);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("VoidEnergy", this.voidEnergy);
        output.putBoolean("Running", this.running);
        ContainerHelper.saveAllItems(output, this.items);

        ValueOutput.ValueOutputList inputList = output.childrenList("InputSources");
        for (VoidEnergyBinding binding : this.inputSources) {
            binding.save(inputList.addChild());
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VoidChargerMenu(
                containerId,
                playerInventory,
                this,
                ContainerLevelAccess.create(player.level(), this.worldPosition),
                this.worldPosition,
                this.menuData,
                this.getBlockState().getBlock()
        );
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public long getEnergyStored() {
        return this.voidEnergy;
    }

    public long getEnergyCapacity() {
        return getTierConfig().cacheCapacity();
    }

    private boolean chargeItems() {
        boolean chargedAny = false;
        long repairPerTick = getTierConfig().repairPerTick();
        for (int slot = 0; slot < this.items.size(); slot++) {
            if (this.voidEnergy < ENERGY_PER_REPAIR) {
                break;
            }

            ItemStack stack = this.items.get(slot);
            if (!(stack.getItem() instanceof EnergyCoreItem coreItem)) {
                continue;
            }

            long missing = coreItem.getCurrentMaxLifetime(stack) - coreItem.getCurrentLifetime(stack);
            if (missing <= 0L) {
                continue;
            }

            long repair = Math.min(repairPerTick, missing);
            long energyCost = repair * ENERGY_PER_REPAIR;
            if (this.voidEnergy < energyCost) {
                continue;
            }

            long restored = coreItem.restoreLifetime(stack, repair);
            if (restored <= 0L) {
                continue;
            }

            this.voidEnergy = clampEnergy(this.voidEnergy - restored * ENERGY_PER_REPAIR);
            chargedAny = true;
        }

        if (chargedAny) {
            setChanged();
        }
        return chargedAny;
    }

    private void updateRunningState(boolean running) {
        if (this.running == running) {
            return;
        }

        this.running = running;
        setChanged();

        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();
        if (state.hasProperty(VoidChargerBlock.ACTIVE)
                && state.getValue(VoidChargerBlock.ACTIVE) != running) {
            level.setBlock(worldPosition, state.setValue(VoidChargerBlock.ACTIVE, running), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public BoundVoidPosition getVoidPosition() {
        if (level == null) {
            return new BoundVoidPosition(Identifier.fromNamespaceAndPath("minecraft", "overworld"), worldPosition);
        }
        return BoundVoidPosition.of(level, worldPosition);
    }

    @Override
    public List<VoidEnergyBinding> getInputSources() {
        return this.inputSources;
    }

    @Override
    public List<VoidEnergyBinding> getOutputTargets() {
        return this.outputTargets;
    }

    @Override
    public VoidEnergyProfile getVoidEnergyProfile() {
        return VoidEnergyProfile.inputOnly(getTierConfig().cacheCapacity(), MAX_INSERT, MAX_INPUT_BINDINGS);
    }

    @Override
    public long getVoidEnergyStored() {
        return this.voidEnergy;
    }

    @Override
    public long receiveVoidEnergy(long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0L, amount), getVoidEnergyCapacity() - this.voidEnergy);
        if (!simulate && accepted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy + accepted);
            onVoidEnergyNetworkChanged();
        }
        return accepted;
    }

    @Override
    public long extractVoidEnergy(long amount, boolean simulate) {
        return 0L;
    }

    @Override
    public void onVoidEnergyNetworkChanged() {
        setChanged();
        syncClient();
    }

    private void syncClient() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private VoidChargerBlock.VoidChargerTier getTierConfig() {
        return VoidChargerBlock.getConfig(this.getBlockState());
    }

    private long clampEnergy(long energy) {
        return getVoidEnergyProfile().clampEnergy(energy);
    }
}
