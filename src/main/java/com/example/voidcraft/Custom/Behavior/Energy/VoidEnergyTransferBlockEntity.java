package com.example.voidcraft.Custom.Behavior.Energy;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

// 能接入虚空能网络的方块实体都实现这个接口。
// 具体方块只负责存能量和列表，通用的添加/删除绑定逻辑放在默认方法里。
public interface VoidEnergyTransferBlockEntity {
    VoidEnergyProfile getVoidEnergyProfile();

    BoundVoidPosition getVoidPosition();

    List<VoidEnergyBinding> getInputSources();

    List<VoidEnergyBinding> getOutputTargets();

    default boolean canReceiveVoidEnergy() {
        return getVoidEnergyProfile().canReceive();
    }

    default boolean canExtractVoidEnergy() {
        return getVoidEnergyProfile().canExtract();
    }

    default int getMaxInputBindings() {
        return getVoidEnergyProfile().maxInputBindings();
    }

    default int getMaxOutputBindings() {
        return getVoidEnergyProfile().maxOutputBindings();
    }

    long getVoidEnergyStored();

    default long getVoidEnergyCapacity() {
        return getVoidEnergyProfile().capacity();
    }

    default long getMaxVoidEnergyInputPerTransfer() {
        return getVoidEnergyProfile().maxInputPerTransfer();
    }

    long receiveVoidEnergy(long amount, boolean simulate);

    long extractVoidEnergy(long amount, boolean simulate);

    void onVoidEnergyNetworkChanged();

    default Level getVoidEnergyLevel() {
        if (this instanceof BlockEntity blockEntity) {
            return blockEntity.getLevel();
        }
        return null;
    }

    // 输入列表保存“谁给我供能”。
    default boolean hasInputSource(BoundVoidPosition source) {
        return getInputSources().stream().anyMatch(binding -> binding.matches(source));
    }

    // 输出列表保存“我要把能量送给谁”。
    default boolean hasOutputTarget(BoundVoidPosition target) {
        return getOutputTargets().stream().anyMatch(binding -> binding.matches(target));
    }

    // 添加前先检查能力、重复项和上限，避免不同方块各写一套规则。
    default boolean canAddInputSource(BoundVoidPosition source) {
        return canReceiveVoidEnergy()
                && !hasInputSource(source)
                && getInputSources().size() < getMaxInputBindings();
    }

    default boolean canAddOutputTarget(BoundVoidPosition target) {
        return canExtractVoidEnergy()
                && !hasOutputTarget(target)
                && getOutputTargets().size() < getMaxOutputBindings();
    }

    // 成功改动绑定后统一通知方块实体保存并同步客户端。
    default boolean addInputSource(VoidEnergyBinding binding) {
        if (!canAddInputSource(binding.target())) {
            return false;
        }
        getInputSources().add(binding);
        onVoidEnergyNetworkChanged();
        return true;
    }

    default boolean addOutputTarget(VoidEnergyBinding binding) {
        if (!canAddOutputTarget(binding.target())) {
            return false;
        }
        getOutputTargets().add(binding);
        onVoidEnergyNetworkChanged();
        return true;
    }

    default boolean removeInputSource(BoundVoidPosition source) {
        boolean removed = getInputSources().removeIf(binding -> binding.matches(source));
        if (removed) {
            onVoidEnergyNetworkChanged();
        }
        return removed;
    }

    default boolean removeOutputTarget(BoundVoidPosition target) {
        boolean removed = getOutputTargets().removeIf(binding -> binding.matches(target));
        if (removed) {
            onVoidEnergyNetworkChanged();
        }
        return removed;
    }
}
