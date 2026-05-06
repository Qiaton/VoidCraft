package com.example.voidcraft.Block.entity;

import java.util.List;

public interface VoidEnergyTransferBlockEntity {
    BoundVoidPosition getVoidPosition();

    List<VoidEnergyBinding> getInputSources();

    List<VoidEnergyBinding> getOutputTargets();

    boolean canReceiveVoidEnergy();

    boolean canExtractVoidEnergy();

    int getMaxInputBindings();

    int getMaxOutputBindings();

    long getVoidEnergyStored();

    long getVoidEnergyCapacity();

    long getMaxVoidEnergyInputPerTransfer();

    long getMaxVoidEnergyOutputPerTransfer();

    long receiveVoidEnergy(long amount, boolean simulate);

    long extractVoidEnergy(long amount, boolean simulate);

    void onVoidEnergyNetworkChanged();

    default boolean hasInputSource(BoundVoidPosition source) {
        return getInputSources().stream().anyMatch(binding -> binding.matches(source));
    }

    default boolean hasOutputTarget(BoundVoidPosition target) {
        return getOutputTargets().stream().anyMatch(binding -> binding.matches(target));
    }

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
