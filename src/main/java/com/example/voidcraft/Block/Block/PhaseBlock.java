package com.example.voidcraft.Block.Block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PhaseBlock extends Block {
    public PhaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        // 相邻的相位方块之间不渲染内部面，避免整片相位地形重复绘制透明面。
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }
}
