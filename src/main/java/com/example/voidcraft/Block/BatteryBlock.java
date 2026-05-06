package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.BatteryBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BatteryBlock extends BaseEntityBlock {
    public static final MapCodec<BatteryBlock> CODEC = simpleCodec(BatteryBlock::new);
    public static final int MAX_ENERGY_STAGE = 9;
    public static final IntegerProperty ENERGY_STAGE = IntegerProperty.create("energy_stage", 0, MAX_ENERGY_STAGE);

    public BatteryBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ENERGY_STAGE, 0));
    }

    @Override
    protected MapCodec<BatteryBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            player.displayClientMessage(
                    Component.translatable("message.void_craft.battery_block.energy", battery.getEnergyStored(), battery.getEnergyCapacity()),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENERGY_STAGE);
    }
}
