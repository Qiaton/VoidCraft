package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.ChunkMapperBlock;
import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.world.ChunkMapperChunkTickets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkMapperBlockEntity extends BlockEntity implements VoidEnergyTransferBlockEntity {
    public static final long CACHE_CAPACITY = 10_000L;
    public static final long MAX_INSERT = 1_000L;
    public static final int MAX_INPUT_BINDINGS = 1;
    public static final int MAX_OUTPUT_BINDINGS = 0;
    private static final int[] RADIUS_BY_TIER = {0, 1, 2, 3};
    private static final long[] ENERGY_COST_BY_TIER = {1L, 16L, 32L, 128L};

    private int tier;
    private long voidEnergy;
    private boolean running;
    private final List<VoidEnergyBinding> inputSources = new ArrayList<>();
    private final List<VoidEnergyBinding> outputTargets = new ArrayList<>();
    private final Set<Long> forcedChunks = new HashSet<>();

    public ChunkMapperBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHUNK_MAPPER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkMapperBlockEntity mapper) {
        if (level.isClientSide()) {
            return;
        }

        mapper.updateTierState();
        mapper.pullFromInputSource();

        if (mapper.tryUseEnergy(mapper.getEnergyCostPerTick())) {
            mapper.ensureForcedChunks();
        } else {
            mapper.stopLoading();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tier = clampTier(input.getIntOr("Tier", 0));
        this.voidEnergy = clampEnergy(input.getLongOr("VoidEnergy", 0L));
        this.running = input.getBooleanOr("Running", false);

        this.inputSources.clear();
        for (ValueInput child : input.childrenListOrEmpty("InputSources")) {
            if (this.inputSources.size() >= MAX_INPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(child).ifPresent(this.inputSources::add);
        }

        this.outputTargets.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Tier", this.tier);
        output.putLong("VoidEnergy", this.voidEnergy);
        output.putBoolean("Running", this.running);

        ValueOutput.ValueOutputList inputList = output.childrenList("InputSources");
        for (VoidEnergyBinding binding : this.inputSources) {
            binding.save(inputList.addChild());
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        releaseForcedChunks();
        super.preRemoveSideEffects(pos, state);
    }

    public int getTier() {
        return this.tier;
    }

    public int getChunkRadius() {
        return RADIUS_BY_TIER[this.tier];
    }

    public int getCoverageSize() {
        return getChunkRadius() * 2 + 1;
    }

    public long getEnergyCostPerTick() {
        return ENERGY_COST_BY_TIER[this.tier];
    }

    public long getEnergyStored() {
        return this.voidEnergy;
    }

    public long getEnergyCapacity() {
        return CACHE_CAPACITY;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean shouldKeepForcedTickets() {
        return this.running;
    }

    public void setTier(int tier) {
        int nextTier = clampTier(tier);
        if (this.tier == nextTier) {
            return;
        }

        this.tier = nextTier;
        updateTierState();
        if (this.running) {
            refreshForcedChunks();
        }
        onVoidEnergyNetworkChanged();
    }

    private void pullFromInputSource() {
        if (level == null || level.isClientSide() || !canReceiveVoidEnergy() || this.inputSources.isEmpty()) {
            return;
        }

        long request = Math.min(getMaxVoidEnergyInputPerTransfer(), getVoidEnergyCapacity() - getVoidEnergyStored());
        if (request <= 0L) {
            return;
        }

        VoidEnergyBinding binding = this.inputSources.get(0);
        VoidEnergyTransfer.ResolveResult sourceResult = VoidEnergyTransfer.resolve(level.getServer(), binding.target());
        if (sourceResult.status() == VoidEnergyTransfer.BindingStatus.UNLOADED) {
            return;
        }
        if (sourceResult.status() != VoidEnergyTransfer.BindingStatus.OK) {
            removeInputSource(binding.target());
            return;
        }

        VoidEnergyTransferBlockEntity source = sourceResult.endpoint();
        if (!source.canExtractVoidEnergy()) {
            removeInputSource(binding.target());
            source.removeOutputTarget(getVoidPosition());
            return;
        }

        if (!source.hasOutputTarget(getVoidPosition())) {
            removeInputSource(binding.target());
            return;
        }

        VoidEnergyTransfer.tryUseEnergy(source, this, request);
    }

    private boolean tryUseEnergy(long amount) {
        if (amount <= 0L) {
            return true;
        }
        if (this.voidEnergy < amount) {
            return false;
        }

        this.voidEnergy = clampEnergy(this.voidEnergy - amount);
        setChanged();
        return true;
    }

    private void ensureForcedChunks() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!this.running) {
            this.running = true;
            refreshForcedChunks(serverLevel);
            onVoidEnergyNetworkChanged();
            return;
        }

        if (this.forcedChunks.isEmpty()) {
            refreshForcedChunks(serverLevel);
        }
    }

    private void refreshForcedChunks() {
        if (level instanceof ServerLevel serverLevel) {
            refreshForcedChunks(serverLevel);
        }
    }

    private void refreshForcedChunks(ServerLevel serverLevel) {
        Set<Long> desired = buildDesiredChunks();

        for (long chunk : List.copyOf(this.forcedChunks)) {
            if (!desired.contains(chunk)) {
                forceChunk(serverLevel, chunk, false);
            }
        }

        for (long chunk : desired) {
            forceChunk(serverLevel, chunk, true);
        }

        this.forcedChunks.clear();
        this.forcedChunks.addAll(desired);
    }

    private void stopLoading() {
        if (!this.running && this.forcedChunks.isEmpty()) {
            return;
        }

        releaseForcedChunks();
        this.running = false;
        onVoidEnergyNetworkChanged();
    }

    public void releaseForcedChunks() {
        if (!(level instanceof ServerLevel serverLevel)) {
            this.forcedChunks.clear();
            this.running = false;
            return;
        }

        Set<Long> chunks = this.forcedChunks.isEmpty() ? buildDesiredChunks() : Set.copyOf(this.forcedChunks);
        for (long chunk : chunks) {
            forceChunk(serverLevel, chunk, false);
        }

        this.forcedChunks.clear();
        this.running = false;
        setChanged();
    }

    private void forceChunk(ServerLevel serverLevel, long chunk, boolean add) {
        ChunkMapperChunkTickets.CHUNK_MAPPER.forceChunk(
                serverLevel,
                this.worldPosition,
                ChunkPos.getX(chunk),
                ChunkPos.getZ(chunk),
                add,
                false
        );
    }

    private Set<Long> buildDesiredChunks() {
        ChunkPos center = new ChunkPos(this.worldPosition);
        int radius = getChunkRadius();
        Set<Long> chunks = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunks.add(ChunkPos.asLong(center.x + dx, center.z + dz));
            }
        }

        return chunks;
    }

    private void updateTierState() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(ChunkMapperBlock.TIER)) {
            return;
        }

        if (state.getValue(ChunkMapperBlock.TIER) != this.tier) {
            level.setBlock(worldPosition, state.setValue(ChunkMapperBlock.TIER, this.tier), Block.UPDATE_CLIENTS);
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
    public boolean canReceiveVoidEnergy() {
        return true;
    }

    @Override
    public boolean canExtractVoidEnergy() {
        return false;
    }

    @Override
    public int getMaxInputBindings() {
        return MAX_INPUT_BINDINGS;
    }

    @Override
    public int getMaxOutputBindings() {
        return MAX_OUTPUT_BINDINGS;
    }

    @Override
    public long getVoidEnergyStored() {
        return this.voidEnergy;
    }

    @Override
    public long getVoidEnergyCapacity() {
        return CACHE_CAPACITY;
    }

    @Override
    public long getMaxVoidEnergyInputPerTransfer() {
        return MAX_INSERT;
    }

    @Override
    public long getMaxVoidEnergyOutputPerTransfer() {
        return 0L;
    }

    @Override
    public long receiveVoidEnergy(long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0L, amount), getVoidEnergyCapacity() - this.voidEnergy);
        if (!simulate && accepted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy + accepted);
            setChanged();
        }
        return accepted;
    }

    @Override
    public long extractVoidEnergy(long amount, boolean simulate) {
        return 0L;
    }

    @Override
    public void onVoidEnergyNetworkChanged() {
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(ChunkMapperBlock.MAX_TIER, tier));
    }

    private static long clampEnergy(long energy) {
        return Math.max(0L, Math.min(CACHE_CAPACITY, energy));
    }
}
