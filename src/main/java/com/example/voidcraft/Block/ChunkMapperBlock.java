package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.ChunkMapperBlockEntity;
import com.example.voidcraft.Item.custom.CoordinateDesignatorItem;
import com.example.voidcraft.network.ModNetworking;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ChunkMapperBlock extends BaseEntityBlock {
    public static final MapCodec<ChunkMapperBlock> CODEC = simpleCodec(ChunkMapperBlock::new);
    public static final int MAX_TIER = 3;
    private static final String[] TIER_DISPLAY_NAMES = {"I", "II", "III", "IV"};
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, MAX_TIER);

    public ChunkMapperBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TIER, 0));
    }

    public static String getTierDisplayName(int tier) {
        // UI 和服务端都走这里拿档位名，避免两边各维护一份 I/II/III。
        int index = Math.max(0, Math.min(MAX_TIER, tier));
        return TIER_DISPLAY_NAMES[index];
    }

    @Override
    protected MapCodec<ChunkMapperBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkMapperBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHUNK_MAPPER_BLOCK_ENTITY.get(), ChunkMapperBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 绑定工具右键时让工具优先执行，不打开状态面板。
        if (stack.getItem() instanceof CoordinateDesignatorItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // 空手右键打开状态面板，面板里的档位修改再发包回服务端。
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ChunkMapperBlockEntity mapper)) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendChunkMapperStatus(serverPlayer, mapper);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }
}
