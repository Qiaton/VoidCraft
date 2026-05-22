package com.example.voidcraft.Block.Block;

import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.entity.VoidEnergyConverterBlockEntity;
import com.example.voidcraft.Item.custom.CoordinateDesignatorItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class VoidEnergyConverterBlock extends BaseEntityBlock {
    public static final EnumProperty<SideMode> DOWN = EnumProperty.create("down", SideMode.class);
    public static final EnumProperty<SideMode> UP = EnumProperty.create("up", SideMode.class);
    public static final EnumProperty<SideMode> NORTH = EnumProperty.create("north", SideMode.class);
    public static final EnumProperty<SideMode> SOUTH = EnumProperty.create("south", SideMode.class);
    public static final EnumProperty<SideMode> WEST = EnumProperty.create("west", SideMode.class);
    public static final EnumProperty<SideMode> EAST = EnumProperty.create("east", SideMode.class);

    public VoidEnergyConverterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DOWN, SideMode.NONE)
                .setValue(UP, SideMode.NONE)
                .setValue(NORTH, SideMode.NONE)
                .setValue(SOUTH, SideMode.NONE)
                .setValue(WEST, SideMode.NONE)
                .setValue(EAST, SideMode.NONE));
    }

    public static EnumProperty<SideMode> propertyFor(Direction side) {
        return switch (side) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    protected MapCodec<VoidEnergyConverterBlock> codec() {
        return simpleCodec(VoidEnergyConverterBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidEnergyConverterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.VOID_ENERGY_CONVERTER_BLOCK_ENTITY.get(), VoidEnergyConverterBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return cycleSide(state, level, pos, player, hit.getDirection());
        }
        if (stack.getItem() instanceof CoordinateDesignatorItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return cycleSide(state, level, pos, player, hit.getDirection());
        }
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof VoidEnergyConverterBlockEntity converter) {
            serverPlayer.openMenu(converter, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult cycleSide(BlockState state, Level level, BlockPos pos, Player player, Direction side) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        EnumProperty<SideMode> property = propertyFor(side);
        SideMode nextMode = state.getValue(property).next();
        level.setBlock(pos, state.setValue(property, nextMode), Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
        level.updateNeighborsAt(pos, this);
        player.displayClientMessage(
                Component.translatable(
                        "message.void_craft.void_energy_converter.side_changed",
                        Component.translatable("message.void_craft.void_energy_converter.side." + side.getSerializedName()),
                        Component.translatable(nextMode.translationKey())
                ),
                true
        );
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    public enum SideMode implements StringRepresentable {
        NONE("none", "message.void_craft.void_energy_converter.mode.none"),
        INPUT("input", "message.void_craft.void_energy_converter.mode.input"),
        OUTPUT("output", "message.void_craft.void_energy_converter.mode.output");

        private final String id;
        private final String translationKey;

        SideMode(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public SideMode next() {
            return switch (this) {
                case NONE -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> NONE;
            };
        }

        public String translationKey() {
            return this.translationKey;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }
}
