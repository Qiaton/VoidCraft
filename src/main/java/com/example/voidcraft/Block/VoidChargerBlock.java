package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.VoidChargerBlockEntity;
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

public class VoidChargerBlock extends BaseEntityBlock {
    public static final MapCodec<VoidChargerBlock> CODEC = simpleCodec(VoidChargerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoidChargerTier LOW_CONFIG = new VoidChargerTier(1, "I", 1, 5_000L, 1);
    private static final VoidChargerTier MID_CONFIG = new VoidChargerTier(2, "II", 3, 50_000L, 1);
    private static final VoidChargerTier HIGH_CONFIG = new VoidChargerTier(3, "III", 9, 200_000L, 10);

    public VoidChargerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    public static VoidChargerTier getConfig(BlockState state) {
        return getConfig(state.getBlock());
    }

    public static VoidChargerTier getConfig(Block block) {
        if (block == ModBlock.MID_VOID_CHARGER.get()) {
            return MID_CONFIG;
        }
        if (block == ModBlock.HIGH_VOID_CHARGER.get()) {
            return HIGH_CONFIG;
        }
        return LOW_CONFIG;
    }

    public static boolean isVoidChargerBlock(Block block) {
        return block == ModBlock.LOW_VOID_CHARGER.get()
                || block == ModBlock.MID_VOID_CHARGER.get()
                || block == ModBlock.HIGH_VOID_CHARGER.get();
    }

    @Override
    protected MapCodec<VoidChargerBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidChargerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.VOID_CHARGER_BLOCK_ENTITY.get(), VoidChargerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof CoordinateDesignatorItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof VoidChargerBlockEntity charger) {
            serverPlayer.openMenu(charger, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public record VoidChargerTier(
            int tier,
            String tierDisplayName,
            int slotCount,
            long cacheCapacity,
            long repairPerTick
    ) {
    }
}
