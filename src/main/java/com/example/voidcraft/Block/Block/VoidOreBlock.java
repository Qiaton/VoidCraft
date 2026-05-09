package com.example.voidcraft.Block.Block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class VoidOreBlock extends PhaseBlock {
    private static final int MIN_XP = 6;
    private static final int MAX_XP = 14;

    public VoidOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int getExpDrop(
            BlockState state,
            LevelAccessor level,
            BlockPos pos,
            @Nullable BlockEntity blockEntity,
            @Nullable Entity breaker,
            ItemStack tool
    ) {
        return level.getRandom().nextIntBetweenInclusive(MIN_XP, MAX_XP);
    }
}
