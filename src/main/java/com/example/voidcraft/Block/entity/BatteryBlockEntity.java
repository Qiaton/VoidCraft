package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.BatteryBlock;
import com.example.voidcraft.Block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public class BatteryBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_INSERT = 1_000;
    public static final int MAX_EXTRACT = 1_000;

    private final SimpleEnergyHandler energyStorage = new SimpleEnergyHandler(CAPACITY, MAX_INSERT, MAX_EXTRACT) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            BatteryBlockEntity.this.setChanged();
            BatteryBlockEntity.this.updateEnergyStage();
        }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BATTERY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public EnergyHandler getEnergyStorage() {
        return energyStorage;
    }

    public long getEnergyStored() {
        return energyStorage.getAmountAsLong();
    }

    public long getEnergyCapacity() {
        return energyStorage.getCapacityAsLong();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.deserialize(input);
        updateEnergyStage();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energyStorage.serialize(output);
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
}
