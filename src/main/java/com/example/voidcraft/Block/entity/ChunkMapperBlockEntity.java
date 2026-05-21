package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.Block.ChunkMapperBlock;
import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyProfile;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.World.ChunkMapperChunkTickets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkMapperBlockEntity extends BlockEntity implements VoidEnergyTransferBlockEntity {
    // 区块映射器只收电不输出，缓存能量后按档位持续消耗来维持区块加载。
    public static final long CACHE_CAPACITY = 10_000L;
    public static final long MAX_INSERT = VoidEnergyProfile.DEFAULT_MAX_INPUT_PER_TRANSFER;
    public static final int MAX_INPUT_BINDINGS = 1;
    public static final int MAX_OUTPUT_BINDINGS = 0;
    private static final VoidEnergyProfile ENERGY_PROFILE = VoidEnergyProfile.inputOnly(
            CACHE_CAPACITY,
            MAX_INSERT,
            MAX_INPUT_BINDINGS,
            1
    );
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

        // 输入端不主动抢电；供能源会按输出列表公平分配给它。
        mapper.updateTierState();

        if (mapper.tryUseEnergy(mapper.getEnergyCostPerTick())) {
            mapper.ensureForcedChunks();
        } else {
            mapper.stopLoading();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tier = clampTier(tag.getInt("Tier"));
        this.voidEnergy = clampEnergy(tag.getLong("VoidEnergy"));
        this.running = tag.getBoolean("Running");

        // 映射器只允许一个输入源，读档时也限制数量，避免旧数据或手改数据超上限。
        this.inputSources.clear();
        ListTag inputList = tag.getList("InputSources", Tag.TAG_COMPOUND);
        for (int i = 0; i < inputList.size(); i++) {
            if (this.inputSources.size() >= MAX_INPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(inputList.getCompound(i)).ifPresent(this.inputSources::add);
        }

        this.outputTargets.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", this.tier);
        tag.putLong("VoidEnergy", this.voidEnergy);
        tag.putBoolean("Running", this.running);

        ListTag inputList = new ListTag();
        for (VoidEnergyBinding binding : this.inputSources) {
            CompoundTag child = new CompoundTag();
            binding.save(child);
            inputList.add(child);
        }
        tag.put("InputSources", inputList);
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
            // 运行中改档位时要马上重算加载范围。
            refreshForcedChunks();
        }
        onVoidEnergyNetworkChanged();
    }

    private boolean tryUseEnergy(long amount) {
        // 映射器的运行耗电直接扣自己的缓存，不走外部传输接口。
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
            // 第一次有电运行时申请区块票，并把 running 同步到客户端状态面板。
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
        // 目标范围由档位决定；先释放不需要的，再补上缺少的。
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

        // 没电时释放所有区块票，避免方块停机后仍然强加载。
        releaseForcedChunks();
        this.running = false;
        onVoidEnergyNetworkChanged();
    }

    public void releaseForcedChunks() {
        if (!(level instanceof ServerLevel serverLevel)) {
            // 世界对象不可用时只能清本地记录，真正票据由 NeoForge 校验器兜底清理。
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
        // 以映射器所在区块为中心，按半径生成方形加载范围。
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
        // 档位存在方块状态里，模型/材质可以按这个值切换。
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
        // 输入上限由 getMaxVoidEnergyInputPerTransfer 控制，这里只处理容量剩余。
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
        syncClient();
    }

    private void syncClient() {
        // 状态面板和方块实体数据都依赖这个服务端同步。
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(ChunkMapperBlock.MAX_TIER, tier));
    }

    private static long clampEnergy(long energy) {
        return ENERGY_PROFILE.clampEnergy(energy);
    }
}
