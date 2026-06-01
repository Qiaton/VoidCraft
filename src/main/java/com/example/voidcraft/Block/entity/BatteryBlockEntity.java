package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Block.Block.BatteryBlock;
import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyBinding;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyProfile;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BatteryBlockEntity extends BlockEntity implements VoidEnergyTransferBlockEntity {
    // 电池既能收也能发，是虚空能网络里的中转缓存。
    public static final long CAPACITY = 500_000L;
    public static final long DEFAULT_ENERGY = 500_000L;
    public static final long MAX_INSERT = VoidEnergyProfile.DEFAULT_MAX_INPUT_PER_TRANSFER;
    public static final int MAX_INPUT_BINDINGS = 8;
    public static final int MAX_OUTPUT_BINDINGS = 8;
    private static final VoidEnergyProfile ENERGY_PROFILE = VoidEnergyProfile.bidirectional(
            CAPACITY,
            MAX_INSERT,
            MAX_INPUT_BINDINGS,
            MAX_OUTPUT_BINDINGS
    );

    private long voidEnergy = DEFAULT_ENERGY;
    private final List<VoidEnergyBinding> inputSources = new ArrayList<>();
    private final List<VoidEnergyBinding> outputTargets = new ArrayList<>();

    public BatteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BATTERY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public long getEnergyStored() {
        return this.voidEnergy;
    }

    public long getEnergyCapacity() {
        return CAPACITY;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BatteryBlockEntity battery) {
        if (level.isClientSide() || !VoidEnergyTransfer.shouldRunTransfer(level, battery)) {
            return;
        }

        // 每隔几 tick 更新外观，并把自己的缓存公平分给输出目标。
        battery.updateEnergyStage();
        VoidEnergyTransfer.pushToOutputTargets(battery);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.voidEnergy = clampEnergy(input.getLongOr("VoidEnergy", DEFAULT_ENERGY));
        // 输入和输出都持久化，重进世界后网络关系还能恢复。
        this.inputSources.clear();
        for (ValueInput child : input.childrenListOrEmpty("InputSources")) {
            VoidEnergyBinding.load(child).ifPresent(this.inputSources::add);
        }
        this.outputTargets.clear();
        for (ValueInput child : input.childrenListOrEmpty("OutputTargets")) {
            VoidEnergyBinding.load(child).ifPresent(this.outputTargets::add);
        }
        updateEnergyStage();
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

    private int getEnergyStage() {
        // 方块模型用 0-9 档显示电量，真实能量还是存在 voidEnergy 里。
        long capacity = Math.max(1L, getEnergyCapacity());
        long stored = Math.max(0L, getEnergyStored());
        long stage = Math.round((double) stored * BatteryBlock.MAX_ENERGY_STAGE / (double) capacity);
        return (int) Math.max(0L, Math.min(BatteryBlock.MAX_ENERGY_STAGE, stage));
    }

    private void updateEnergyStage() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(BatteryBlock.ENERGY_STAGE)) {
            return;
        }

        int stage = getEnergyStage();
        if (state.getValue(BatteryBlock.ENERGY_STAGE) != stage) {
            // 只有档位变化时才 setBlock，减少无意义的方块状态同步。
            level.setBlock(worldPosition, state.setValue(BatteryBlock.ENERGY_STAGE, stage), Block.UPDATE_CLIENTS);
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
            return new BoundVoidPosition(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "overworld"), worldPosition);
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
        // simulate 为 true 时只回答“能收多少”，不真的改能量。
        long accepted = Math.min(Math.max(0L, amount), getVoidEnergyCapacity() - this.voidEnergy);
        if (!simulate && accepted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy + accepted);
            onVoidEnergyNetworkChanged();
            updateEnergyStage();
        }
        return accepted;
    }

    @Override
    public long extractVoidEnergy(long amount, boolean simulate) {
        // 输出同样支持模拟，供 VoidEnergyTransfer 先试算再真正搬运。
        long extracted = Math.min(Math.max(0L, amount), this.voidEnergy);
        if (!simulate && extracted > 0L) {
            this.voidEnergy = clampEnergy(this.voidEnergy - extracted);
            onVoidEnergyNetworkChanged();
            updateEnergyStage();
        }
        return extracted;
    }

    @Override
    public void onVoidEnergyNetworkChanged() {
        setChanged();
        syncClient();
    }

    private void syncClient() {
        // 能量变化后通知客户端刷新方块实体数据和方块外观。
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static long clampEnergy(long energy) {
        return ENERGY_PROFILE.clampEnergy(energy);
    }
}
