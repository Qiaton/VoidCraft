package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.Block.VoidPhenomenonCollectorBlock;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyProfile;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.Gui.VoidPhenomenonCollectorMenu;
import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.Item.custom.VoidCrystalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VoidPhenomenonCollectorBlockEntity extends BlockEntity implements Container, MenuProvider, VoidEnergyTransferBlockEntity {
    // 结晶发电机自己产能，只向外输出虚空能；具体槽位数量由方块档位决定。
    public static final int MAX_INPUT_BINDINGS = 0;
    public static final int MAX_OUTPUT_BINDINGS = 8;
    public static final int DATA_COUNT = 4;

    private static final int DATA_ENERGY_STORED = 0;
    private static final int DATA_RUNNING = 1;
    private static final int DATA_OUTPUT_COUNT = 2;
    private static final int DATA_MAX_OUTPUTS = 3;

    private long voidEnergy;
    private boolean running;
    private int syncedOutputCount;
    private int syncedMaxOutputBindings = MAX_OUTPUT_BINDINGS;
    private final NonNullList<ItemStack> items;
    private final List<VoidEnergyBinding> inputSources = new ArrayList<>();
    private final List<VoidEnergyBinding> outputTargets = new ArrayList<>();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_STORED -> (int) VoidPhenomenonCollectorBlockEntity.this.voidEnergy;
                case DATA_RUNNING -> VoidPhenomenonCollectorBlockEntity.this.running ? 1 : 0;
                case DATA_OUTPUT_COUNT -> VoidPhenomenonCollectorBlockEntity.this.level != null && VoidPhenomenonCollectorBlockEntity.this.level.isClientSide()
                        ? VoidPhenomenonCollectorBlockEntity.this.syncedOutputCount
                        : VoidPhenomenonCollectorBlockEntity.this.outputTargets.size();
                case DATA_MAX_OUTPUTS -> VoidPhenomenonCollectorBlockEntity.this.syncedMaxOutputBindings;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_ENERGY_STORED -> VoidPhenomenonCollectorBlockEntity.this.voidEnergy = clampEnergy(value);
                case DATA_RUNNING -> VoidPhenomenonCollectorBlockEntity.this.running = value != 0;
                case DATA_OUTPUT_COUNT -> VoidPhenomenonCollectorBlockEntity.this.syncedOutputCount = Math.max(0, value);
                case DATA_MAX_OUTPUTS -> VoidPhenomenonCollectorBlockEntity.this.syncedMaxOutputBindings = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public VoidPhenomenonCollectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VOID_PHENOMENON_COLLECTOR_BLOCK_ENTITY.get(), pos, blockState);
        this.items = NonNullList.withSize(VoidPhenomenonCollectorBlock.getConfig(blockState).crystalSlotCount(), ItemStack.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VoidPhenomenonCollectorBlockEntity collector) {
        if (level.isClientSide()) {
            return;
        }

        if (VoidEnergyTransfer.shouldRunTransfer(level, collector)) {
            // 输出不需要每 tick 都扫，降低绑定列表检查频率。
            VoidEnergyTransfer.pushToOutputTargets(collector);
        }

        // 产能每 tick 尝试一次，返回值用于切换 ACTIVE 方块状态。
        boolean generated = collector.generateFromCrystal();
        collector.updateRunningState(generated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.voidEnergy = clampEnergy(tag.getLong("VoidEnergy"));
        this.running = tag.getBoolean("Running");

        // 先清空槽位再读入，避免旧内容残留。
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, this.items, registries);

        this.inputSources.clear();
        this.outputTargets.clear();
        ListTag outputList = tag.getList("OutputTargets", Tag.TAG_COMPOUND);
        for (int i = 0; i < outputList.size(); i++) {
            // 发电机只支持输出绑定，读档时也按上限截断。
            if (this.outputTargets.size() >= MAX_OUTPUT_BINDINGS) {
                break;
            }
            VoidEnergyBinding.load(outputList.getCompound(i)).ifPresent(this.outputTargets::add);
        }
        this.syncedOutputCount = this.outputTargets.size();
        this.syncedMaxOutputBindings = MAX_OUTPUT_BINDINGS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("VoidEnergy", this.voidEnergy);
        tag.putBoolean("Running", this.running);
        // 结晶槽、内部能量和输出目标都随方块实体保存。
        ContainerHelper.saveAllItems(tag, this.items, registries);

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

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // menuData 是服务端同步给客户端 GUI 的小整数状态。
        return new VoidPhenomenonCollectorMenu(
                containerId,
                playerInventory,
                this,
                ContainerLevelAccess.create(player.level(), this.worldPosition),
                this.worldPosition,
                this.menuData,
                this.getBlockState().getBlock()
        );
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public boolean isRunning() {
        return this.running;
    }

    public long getEnergyStored() {
        return this.voidEnergy;
    }

    public long getEnergyCapacity() {
        return getTierConfig().cacheCapacity();
    }

    private boolean generateFromCrystal() {
        long cacheCapacity = getTierConfig().cacheCapacity();
        // 缓存满了就不继续磨损结晶。
        if (this.voidEnergy >= cacheCapacity) {
            return false;
        }

        boolean generatedAny = false;
        for (int slot = 0; slot < this.items.size() && this.voidEnergy < cacheCapacity; slot++) {
            ItemStack crystalStack = this.items.get(slot);
            if (!(crystalStack.getItem() instanceof VoidCrystalItem crystalItem)) {
                continue;
            }

            long generated = Math.min(
                    getTierConfig().baseGenerationPerTick() * crystalItem.getGenerationMultiplier(),
                    cacheCapacity - this.voidEnergy
            );
            if (generated <= 0L) {
                continue;
            }

            // 每个槽里的结晶都独立贡献产能，并独立累积使用进度。
            this.voidEnergy = clampEnergy(this.voidEnergy + generated);
            VoidCrystalItem.DamageProgressResult result = VoidCrystalItem.addProgress(crystalStack, 1);
            if (result.depleted()) {
                this.items.set(slot, new ItemStack(ModItem.VOID_CRYSTAL_RESIDUE.get()));
            }
            generatedAny = true;
        }

        if (generatedAny) {
            setChanged();
        }
        return generatedAny;
    }

    private void updateRunningState(boolean running) {
        // running 只代表本 tick 是否成功发电，用来控制发电机发光/动画状态。
        if (this.running == running) {
            return;
        }

        this.running = running;
        setChanged();

        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();
        if (state.hasProperty(VoidPhenomenonCollectorBlock.ACTIVE)
                && state.getValue(VoidPhenomenonCollectorBlock.ACTIVE) != running) {
            // ACTIVE 是方块状态，资源包可以用它切换发光模型。
            level.setBlock(worldPosition, state.setValue(VoidPhenomenonCollectorBlock.ACTIVE, running), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            // 结晶槽只允许单个结晶，防止一格堆叠导致耐久/进度不好处理。
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof VoidCrystalItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        setChanged();
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
        VoidPhenomenonCollectorBlock.CollectorTierConfig config = getTierConfig();
        return VoidEnergyProfile.outputOnly(config.cacheCapacity(), MAX_OUTPUT_BINDINGS);
    }

    @Override
    public long getVoidEnergyStored() {
        return this.voidEnergy;
    }

    @Override
    public long receiveVoidEnergy(long amount, boolean simulate) {
        return 0L;
    }

    @Override
    public long extractVoidEnergy(long amount, boolean simulate) {
        // 输出时支持模拟，供 VoidEnergyTransfer 先算出真实移动量。
        long extracted = Math.min(Math.max(0L, amount), this.voidEnergy);
        if (!simulate && extracted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy - extracted);
            setChanged();
        }
        return extracted;
    }

    @Override
    public void onVoidEnergyNetworkChanged() {
        setChanged();
        syncClient();
    }

    private void syncClient() {
        // 输出绑定或能量变化后刷新客户端方块实体数据。
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private VoidPhenomenonCollectorBlock.CollectorTierConfig getTierConfig() {
        return VoidPhenomenonCollectorBlock.getConfig(this.getBlockState());
    }

    private long clampEnergy(long energy) {
        return getVoidEnergyProfile().clampEnergy(energy);
    }
}
