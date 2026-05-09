package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.VoidPhenomenonCollectorBlockEntity;
import com.example.voidcraft.Item.custom.CoordinateDesignatorItem;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class VoidPhenomenonCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<VoidPhenomenonCollectorBlock> CODEC = simpleCodec(VoidPhenomenonCollectorBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final CollectorTierConfig BASE_CONFIG = new CollectorTierConfig(1, "I", 1L, 50_000L, 1);
    private static final CollectorTierConfig IMPROVED_CONFIG = new CollectorTierConfig(2, "II", 2L, 70_000L, 3);
    private static final CollectorTierConfig ADVANCED_CONFIG = new CollectorTierConfig(3, "III", 3L, 150_000L, 6);
    private static final CollectorTierConfig ATTUNER_CONFIG = new CollectorTierConfig(4, "IV", 5L, 500_000L, 9);

    public VoidPhenomenonCollectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    public static CollectorTierConfig getConfig(BlockState state) {
        return getConfig(state.getBlock());
    }

    public static CollectorTierConfig getConfig(Block block) {
        if (block == ModBlock.IMPROVED_VOID_PHENOMENON_COLLECTOR.get()) {
            return IMPROVED_CONFIG;
        }
        if (block == ModBlock.ADVANCED_VOID_PHENOMENON_COLLECTOR.get()) {
            return ADVANCED_CONFIG;
        }
        if (block == ModBlock.VOID_ATTUNER.get()) {
            return ATTUNER_CONFIG;
        }
        return BASE_CONFIG;
    }

    public static boolean isCollectorBlock(Block block) {
        return block == ModBlock.VOID_PHENOMENON_COLLECTOR.get()
                || block == ModBlock.IMPROVED_VOID_PHENOMENON_COLLECTOR.get()
                || block == ModBlock.ADVANCED_VOID_PHENOMENON_COLLECTOR.get()
                || block == ModBlock.VOID_ATTUNER.get();
    }

    @Override
    protected MapCodec<VoidPhenomenonCollectorBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidPhenomenonCollectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // 发电逻辑只在服务端跑，客户端只负责显示方块状态和界面。
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.VOID_PHENOMENON_COLLECTOR_BLOCK_ENTITY.get(), VoidPhenomenonCollectorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 绑定工具右键时放行给工具处理，不直接打开发电机界面。
        if (stack.getItem() instanceof CoordinateDesignatorItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // 空手右键打开发电机菜单，菜单里的数据由 BlockEntity 同步。
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof VoidPhenomenonCollectorBlockEntity collector) {
            serverPlayer.openMenu(collector, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public record CollectorTierConfig(
            int tier,
            String tierDisplayName,
            long baseGenerationPerTick,
            long cacheCapacity,
            int crystalSlotCount
    ) {
    }
}
