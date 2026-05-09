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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
    private final FeJournal feJournal = new FeJournal();

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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.voidEnergy = clampEnergy(input.getLongOr("VoidEnergy", 0L));
        this.lastFeTick = -1L;
        this.feInputThisTick = 0;
        this.feOutputThisTick = 0;

        this.inputSources.clear();
        for (ValueInput child : input.childrenListOrEmpty("InputSources")) {
            if (this.inputSources.size() >= MAX_INPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(child).ifPresent(this.inputSources::add);
        }

        this.outputTargets.clear();
        for (ValueInput child : input.childrenListOrEmpty("OutputTargets")) {
            if (this.outputTargets.size() >= MAX_OUTPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(child).ifPresent(this.outputTargets::add);
        }
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

    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
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

    private int insertFe(Direction side, int amount, TransactionContext transaction) {
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

        this.feJournal.updateSnapshots(transaction);
        this.voidEnergy = clampEnergy(this.voidEnergy + addVoidEnergy);
        this.feInputThisTick += usedFe;
        return usedFe;
    }

    private int extractFe(Direction side, int amount, TransactionContext transaction) {
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

        this.feJournal.updateSnapshots(transaction);
        this.voidEnergy = clampEnergy(this.voidEnergy - usedVoidEnergy);
        this.feOutputThisTick += extractedFe;
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
            return new BoundVoidPosition(Identifier.fromNamespaceAndPath("minecraft", "overworld"), worldPosition);
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

    private record EnergySnapshot(long voidEnergy, long lastFeTick, int feInputThisTick, int feOutputThisTick) {
    }

    private final class FeJournal extends SnapshotJournal<EnergySnapshot> {
        @Override
        protected EnergySnapshot createSnapshot() {
            return new EnergySnapshot(voidEnergy, lastFeTick, feInputThisTick, feOutputThisTick);
        }

        @Override
        protected void revertToSnapshot(EnergySnapshot snapshot) {
            voidEnergy = snapshot.voidEnergy();
            lastFeTick = snapshot.lastFeTick();
            feInputThisTick = snapshot.feInputThisTick();
            feOutputThisTick = snapshot.feOutputThisTick();
        }

        @Override
        protected void onRootCommit(EnergySnapshot originalState) {
            if (originalState.voidEnergy() != voidEnergy) {
                onVoidEnergyNetworkChanged();
            }
        }
    }

    private static final class VoidEnergyFeHandler implements EnergyHandler {
        private final VoidEnergyConverterBlockEntity converter;
        private final Direction side;

        private VoidEnergyFeHandler(VoidEnergyConverterBlockEntity converter, Direction side) {
            this.converter = converter;
            this.side = side;
        }

        @Override
        public long getAmountAsLong() {
            return this.converter.getFeAmount(this.side);
        }

        @Override
        public long getCapacityAsLong() {
            return this.converter.getFeCapacity(this.side);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            return this.converter.insertFe(this.side, amount, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            return this.converter.extractFe(this.side, amount, transaction);
        }
    }
}
