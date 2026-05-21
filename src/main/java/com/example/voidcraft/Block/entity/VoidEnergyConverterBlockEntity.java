package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.Block.VoidEnergyConverterBlock;
import com.example.voidcraft.Block.Block.VoidEnergyConverterBlock.SideMode;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyProfile;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VoidEnergyConverterBlockEntity extends BlockEntity implements VoidEnergyTransferBlockEntity {
    public static final long CACHE_CAPACITY = 10_000L;
    public static final long MAX_INSERT = VoidEnergyProfile.DEFAULT_MAX_INPUT_PER_TRANSFER;
    public static final int MAX_INPUT_BINDINGS = 8;
    public static final int MAX_OUTPUT_BINDINGS = 8;
    public static final int MAX_FE_INPUT_PER_TICK = 1_000;
    public static final int MAX_FE_OUTPUT_PER_TICK = 1_000;
    public static final double FE_TO_VOID_RATE = 0.3D;
    public static final double VOID_TO_FE_RATE = 2.D;

    private static final VoidEnergyProfile ENERGY_PROFILE = VoidEnergyProfile.bidirectional(
            CACHE_CAPACITY,
            MAX_INSERT,
            MAX_INPUT_BINDINGS,
            MAX_OUTPUT_BINDINGS
    );

    private long voidEnergy;
    private long lastFeTick = -1L;
    private int feInputThisTick;
    private int feOutputThisTick;
    private final List<VoidEnergyBinding> inputSources = new ArrayList<>();
    private final List<VoidEnergyBinding> outputTargets = new ArrayList<>();
    private final VoidEnergyFeHandler[] feHandlers = new VoidEnergyFeHandler[Direction.values().length];

    public VoidEnergyConverterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VOID_ENERGY_CONVERTER_BLOCK_ENTITY.get(), pos, blockState);
        for (Direction side : Direction.values()) {
            this.feHandlers[side.ordinal()] = new VoidEnergyFeHandler(this, side);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VoidEnergyConverterBlockEntity converter) {
        if (level.isClientSide()) {
            return;
        }
        if (VoidEnergyTransfer.shouldRunTransfer(level, converter)) {
            VoidEnergyTransfer.pushToOutputTargets(converter);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.voidEnergy = clampEnergy(tag.getLong("VoidEnergy"));
        this.lastFeTick = -1L;
        this.feInputThisTick = 0;
        this.feOutputThisTick = 0;

        this.inputSources.clear();
        ListTag inputList = tag.getList("InputSources", Tag.TAG_COMPOUND);
        for (int i = 0; i < inputList.size(); i++) {
            if (this.inputSources.size() >= MAX_INPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(inputList.getCompound(i)).ifPresent(this.inputSources::add);
        }

        this.outputTargets.clear();
        ListTag outputList = tag.getList("OutputTargets", Tag.TAG_COMPOUND);
        for (int i = 0; i < outputList.size(); i++) {
            if (this.outputTargets.size() >= MAX_OUTPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(outputList.getCompound(i)).ifPresent(this.outputTargets::add);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("VoidEnergy", this.voidEnergy);

        ListTag inputList = new ListTag();
        for (VoidEnergyBinding binding : this.inputSources) {
            CompoundTag child = new CompoundTag();
            binding.save(child);
            inputList.add(child);
        }
        tag.put("InputSources", inputList);

        ListTag outputList = new ListTag();
        for (VoidEnergyBinding binding : this.outputTargets) {
            CompoundTag child = new CompoundTag();
            binding.save(child);
            outputList.add(child);
        }
        tag.put("OutputTargets", outputList);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public long getEnergyStored() {
        return this.voidEnergy;
    }

    public long getEnergyCapacity() {
        return CACHE_CAPACITY;
    }

    public @Nullable IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        if (side == null || getSideMode(side) == SideMode.NONE) {
            return null;
        }
        return this.feHandlers[side.ordinal()];
    }

    private SideMode getSideMode(Direction side) {
        return getBlockState().getValue(VoidEnergyConverterBlock.propertyFor(side));
    }

    private long getFeAmount(Direction side) {
        return switch (getSideMode(side)) {
            case INPUT -> ceilDivide(this.voidEnergy, FE_TO_VOID_RATE);
            case OUTPUT -> multiplyFloor(this.voidEnergy, VOID_TO_FE_RATE);
            case NONE -> 0L;
        };
    }

    private long getFeCapacity(Direction side) {
        return switch (getSideMode(side)) {
            case INPUT -> ceilDivide(CACHE_CAPACITY, FE_TO_VOID_RATE);
            case OUTPUT -> multiplyFloor(CACHE_CAPACITY, VOID_TO_FE_RATE);
            case NONE -> 0L;
        };
    }

    private int insertFe(Direction side, int amount, boolean simulate) {
        if (amount <= 0 || getSideMode(side) != SideMode.INPUT) {
            return 0;
        }

        resetFeTick();
        int tickLeft = Math.max(0, MAX_FE_INPUT_PER_TICK - this.feInputThisTick);
        if (tickLeft <= 0) {
            return 0;
        }

        long space = CACHE_CAPACITY - this.voidEnergy;
        if (space <= 0L) {
            return 0;
        }

        int maxFeBySpace = toInt(floorDivide(space, FE_TO_VOID_RATE));
        int feLimit = Math.min(amount, Math.min(tickLeft, maxFeBySpace));
        long addVoidEnergy = multiplyFloor(feLimit, FE_TO_VOID_RATE);
        if (addVoidEnergy <= 0L) {
            return 0;
        }

        int usedFe = Math.min(feLimit, toInt(ceilDivide(addVoidEnergy, FE_TO_VOID_RATE)));
        if (usedFe <= 0) {
            return 0;
        }

        if (!simulate) {
            this.voidEnergy = clampEnergy(this.voidEnergy + addVoidEnergy);
            this.feInputThisTick += usedFe;
            onVoidEnergyNetworkChanged();
        }
        return usedFe;
    }

    private int extractFe(Direction side, int amount, boolean simulate) {
        if (amount <= 0 || getSideMode(side) != SideMode.OUTPUT) {
            return 0;
        }

        resetFeTick();
        int tickLeft = Math.max(0, MAX_FE_OUTPUT_PER_TICK - this.feOutputThisTick);
        if (tickLeft <= 0) {
            return 0;
        }

        int maxFeByEnergy = toInt(multiplyFloor(this.voidEnergy, VOID_TO_FE_RATE));
        int extractedFe = Math.min(amount, Math.min(tickLeft, maxFeByEnergy));
        if (extractedFe <= 0) {
            return 0;
        }

        long usedVoidEnergy = ceilDivide(extractedFe, VOID_TO_FE_RATE);
        if (usedVoidEnergy <= 0L || usedVoidEnergy > this.voidEnergy) {
            return 0;
        }

        if (!simulate) {
            this.voidEnergy = clampEnergy(this.voidEnergy - usedVoidEnergy);
            this.feOutputThisTick += extractedFe;
            onVoidEnergyNetworkChanged();
        }
        return extractedFe;
    }

    private void resetFeTick() {
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        if (this.lastFeTick != gameTime) {
            this.lastFeTick = gameTime;
            this.feInputThisTick = 0;
            this.feOutputThisTick = 0;
        }
    }

    @Override
    public BoundVoidPosition getVoidPosition() {
        if (level == null) {
            return new BoundVoidPosition(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"), worldPosition);
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
        return ENERGY_PROFILE;
    }

    @Override
    public long getVoidEnergyStored() {
        return this.voidEnergy;
    }

    @Override
    public long receiveVoidEnergy(long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0L, amount), CACHE_CAPACITY - this.voidEnergy);
        if (!simulate && accepted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy + accepted);
            onVoidEnergyNetworkChanged();
        }
        return accepted;
    }

    @Override
    public long extractVoidEnergy(long amount, boolean simulate) {
        long extracted = Math.min(Math.max(0L, amount), this.voidEnergy);
        if (!simulate && extracted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy - extracted);
            onVoidEnergyNetworkChanged();
        }
        return extracted;
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

    private static long clampEnergy(long energy) {
        return ENERGY_PROFILE.clampEnergy(energy);
    }

    private static long multiplyFloor(long amount, double rate) {
        if (amount <= 0L || rate <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.floor((double) amount * rate));
    }

    private static long floorDivide(long amount, double rate) {
        if (amount <= 0L || rate <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.floor((double) amount / rate));
    }

    private static long ceilDivide(long amount, double rate) {
        if (amount <= 0L || rate <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.ceil((double) amount / rate));
    }

    private static int toInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    private static final class VoidEnergyFeHandler implements IEnergyStorage {
        private final VoidEnergyConverterBlockEntity converter;
        private final Direction side;

        private VoidEnergyFeHandler(VoidEnergyConverterBlockEntity converter, Direction side) {
            this.converter = converter;
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive < 0) {
                throw new IllegalArgumentException("maxReceive must be non-negative");
            }
            return this.converter.insertFe(this.side, maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract < 0) {
                throw new IllegalArgumentException("maxExtract must be non-negative");
            }
            return this.converter.extractFe(this.side, maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return toInt(this.converter.getFeAmount(this.side));
        }

        @Override
        public int getMaxEnergyStored() {
            return toInt(this.converter.getFeCapacity(this.side));
        }

        @Override
        public boolean canExtract() {
            return this.converter.getSideMode(this.side) == SideMode.OUTPUT;
        }

        @Override
        public boolean canReceive() {
            return this.converter.getSideMode(this.side) == SideMode.INPUT;
        }
    }
}
