package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.BatteryBlock;
import com.example.voidcraft.Block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BatteryBlockEntity extends BlockEntity implements VoidEnergyTransferBlockEntity {
    public static final long CAPACITY = 40_000L;
    public static final long DEFAULT_ENERGY = 40_000L;
    public static final long MAX_INSERT = 1_000L;
    public static final long MAX_EXTRACT = 1_000L;
    public static final int MAX_INPUT_BINDINGS = 8;
    public static final int MAX_OUTPUT_BINDINGS = 8;
    private static final int TRANSFER_INTERVAL_TICKS = 5;

    private long voidEnergy = DEFAULT_ENERGY;
    private final List<VoidEnergyBinding> inputSources = new ArrayList<>();
    private final List<VoidEnergyBinding> outputTargets = new ArrayList<>();

    public BatteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BATTERY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public long getEnergyStored() {
        return this.voidEnergy;
    }

    public long getEnergyCapacity() {
        return CAPACITY;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BatteryBlockEntity battery) {
        if (level.isClientSide() || level.getGameTime() % TRANSFER_INTERVAL_TICKS != 0L) {
            return;
        }

        battery.updateEnergyStage();
        battery.pullFromInputSources();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.voidEnergy = clampEnergy(input.getLongOr("VoidEnergy", DEFAULT_ENERGY));
        this.inputSources.clear();
        for (ValueInput child : input.childrenListOrEmpty("InputSources")) {
            VoidEnergyBinding.load(child).ifPresent(this.inputSources::add);
        }
        this.outputTargets.clear();
        for (ValueInput child : input.childrenListOrEmpty("OutputTargets")) {
            VoidEnergyBinding.load(child).ifPresent(this.outputTargets::add);
        }
        updateEnergyStage();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("VoidEnergy", this.voidEnergy);
        ValueOutput.ValueOutputList inputList = output.childrenList("InputSources");
        for (VoidEnergyBinding binding : this.inputSources) {
            binding.save(inputList.addChild());
        }
        ValueOutput.ValueOutputList outputList = output.childrenList("OutputTargets");
        for (VoidEnergyBinding binding : this.outputTargets) {
            binding.save(outputList.addChild());
        }
    }

    private int getEnergyStage() {
        long capacity = Math.max(1L, getEnergyCapacity());
        long stored = Math.max(0L, getEnergyStored());
        long stage = Math.round((double) stored * BatteryBlock.MAX_ENERGY_STAGE / (double) capacity);
        return (int) Math.max(0L, Math.min(BatteryBlock.MAX_ENERGY_STAGE, stage));
    }

    private void updateEnergyStage() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(BatteryBlock.ENERGY_STAGE)) {
            return;
        }

        int stage = getEnergyStage();
        if (state.getValue(BatteryBlock.ENERGY_STAGE) != stage) {
            level.setBlock(worldPosition, state.setValue(BatteryBlock.ENERGY_STAGE, stage), Block.UPDATE_CLIENTS);
        }
    }

    private void pullFromInputSources() {
        if (level == null || level.isClientSide() || !canReceiveVoidEnergy()) {
            return;
        }

        long remainingRequest = Math.min(getMaxVoidEnergyInputPerTransfer(), getVoidEnergyCapacity() - getVoidEnergyStored());
        if (remainingRequest <= 0L) {
            return;
        }

        for (VoidEnergyBinding binding : List.copyOf(this.inputSources)) {
            VoidEnergyTransfer.ResolveResult sourceResult = VoidEnergyTransfer.resolve(level.getServer(), binding.target());
            if (sourceResult.status() == VoidEnergyTransfer.BindingStatus.UNLOADED) {
                continue;
            }
            if (sourceResult.status() != VoidEnergyTransfer.BindingStatus.OK) {
                removeInputSource(binding.target());
                continue;
            }

            VoidEnergyTransferBlockEntity source = sourceResult.endpoint();
            if (!source.canExtractVoidEnergy()) {
                removeInputSource(binding.target());
                source.removeOutputTarget(getVoidPosition());
                continue;
            }

            if (!source.hasOutputTarget(getVoidPosition())) {
                removeInputSource(binding.target());
                continue;
            }

            VoidEnergyTransfer.TransferResult result = VoidEnergyTransfer.tryUseEnergy(source, this, remainingRequest);
            remainingRequest -= result.movedEnergy();
            if (remainingRequest <= 0L) {
                return;
            }
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
    public BoundVoidPosition getVoidPosition() {
        if (level == null) {
            return new BoundVoidPosition(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "overworld"), worldPosition);
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
    public boolean canReceiveVoidEnergy() {
        return true;
    }

    @Override
    public boolean canExtractVoidEnergy() {
        return true;
    }

    @Override
    public int getMaxInputBindings() {
        return MAX_INPUT_BINDINGS;
    }

    @Override
    public int getMaxOutputBindings() {
        return MAX_OUTPUT_BINDINGS;
    }

    @Override
    public long getVoidEnergyStored() {
        return this.voidEnergy;
    }

    @Override
    public long getVoidEnergyCapacity() {
        return CAPACITY;
    }

    @Override
    public long getMaxVoidEnergyInputPerTransfer() {
        return MAX_INSERT;
    }

    @Override
    public long getMaxVoidEnergyOutputPerTransfer() {
        return MAX_EXTRACT;
    }

    @Override
    public long receiveVoidEnergy(long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0L, amount), getVoidEnergyCapacity() - this.voidEnergy);
        if (!simulate && accepted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy + accepted);
            onVoidEnergyNetworkChanged();
            updateEnergyStage();
        }
        return accepted;
    }

    @Override
    public long extractVoidEnergy(long amount, boolean simulate) {
        long extracted = Math.min(Math.max(0L, amount), this.voidEnergy);
        if (!simulate && extracted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy - extracted);
            onVoidEnergyNetworkChanged();
            updateEnergyStage();
        }
        return extracted;
    }

    @Override
    public void onVoidEnergyNetworkChanged() {
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static long clampEnergy(long energy) {
        return Math.max(0L, Math.min(CAPACITY, energy));
    }
}
