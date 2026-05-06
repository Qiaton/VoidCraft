package com.example.voidcraft.Block.entity;

import com.example.voidcraft.Item.custom.CoordinateDesignatorData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public final class VoidEnergyTransfer {
    private VoidEnergyTransfer() {
    }

    public static ResolveResult resolve(MinecraftServer server, BoundVoidPosition position) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, position.dimension());
        ServerLevel level = server.getLevel(levelKey);
        if (level == null || !level.isLoaded(position.pos())) {
            return ResolveResult.unloaded();
        }

        BlockEntity blockEntity = level.getBlockEntity(position.pos());
        if (blockEntity instanceof VoidEnergyTransferBlockEntity endpoint) {
            return ResolveResult.loaded(endpoint);
        }

        return ResolveResult.invalid();
    }

    public static BindResult bind(MinecraftServer server, BoundVoidPosition primary, BoundVoidPosition secondary, CoordinateDesignatorData.Mode mode) {
        return bind(server, primary, secondary, mode, false);
    }

    public static BindResult bind(
            MinecraftServer server,
            BoundVoidPosition primary,
            BoundVoidPosition secondary,
            CoordinateDesignatorData.Mode mode,
            boolean replaceTargetInput
    ) {
        if (primary.sameBlock(secondary)) {
            return BindResult.SAME_BLOCK;
        }

        BoundVoidPosition source;
        BoundVoidPosition target;
        VoidEnergyBindingType bindingType;

        if (mode == CoordinateDesignatorData.Mode.INPUT) {
            source = secondary;
            target = primary;
            bindingType = VoidEnergyBindingType.INPUT;
        } else {
            source = primary;
            target = secondary;
            bindingType = VoidEnergyBindingType.OUTPUT;
        }

        ResolveResult sourceResult = resolve(server, source);
        ResolveResult targetResult = resolve(server, target);

        if (sourceResult.status() == BindingStatus.NOT_FUNCTIONAL || targetResult.status() == BindingStatus.NOT_FUNCTIONAL) {
            return BindResult.POSITION_CHANGED;
        }
        if (sourceResult.status() == BindingStatus.UNLOADED && targetResult.status() == BindingStatus.UNLOADED) {
            return BindResult.POSITION_CHANGED;
        }

        VoidEnergyTransferBlockEntity sourceEndpoint = sourceResult.endpoint();
        VoidEnergyTransferBlockEntity targetEndpoint = targetResult.endpoint();

        if (sourceEndpoint != null && !sourceEndpoint.canExtractVoidEnergy()) {
            return BindResult.SOURCE_CANNOT_OUTPUT;
        }
        if (targetEndpoint != null && !targetEndpoint.canReceiveVoidEnergy()) {
            return BindResult.TARGET_CANNOT_INPUT;
        }
        boolean sourceHasOutput = sourceEndpoint != null && sourceEndpoint.hasOutputTarget(target);
        boolean targetHasInput = targetEndpoint != null && targetEndpoint.hasInputSource(source);
        if (sourceHasOutput && targetHasInput) {
            return BindResult.DUPLICATE;
        }
        boolean shouldReplaceTargetInput = replaceTargetInput
                && targetEndpoint != null
                && !targetHasInput
                && !targetEndpoint.getInputSources().isEmpty();
        if (sourceEndpoint != null && !sourceHasOutput && !sourceEndpoint.canAddOutputTarget(target)) {
            return BindResult.SOURCE_OUTPUT_FULL;
        }
        if (targetEndpoint != null && !targetHasInput && !targetEndpoint.canAddInputSource(source) && !shouldReplaceTargetInput) {
            return BindResult.TARGET_INPUT_FULL;
        }

        if (targetEndpoint != null && shouldReplaceTargetInput) {
            for (VoidEnergyBinding binding : List.copyOf(targetEndpoint.getInputSources())) {
                removeInputBinding(server, targetEndpoint, binding.target());
            }
        }

        if (sourceEndpoint != null && !sourceHasOutput) {
            sourceEndpoint.addOutputTarget(new VoidEnergyBinding(target, bindingType));
        }
        if (targetEndpoint != null && !targetHasInput) {
            targetEndpoint.addInputSource(new VoidEnergyBinding(source, bindingType));
        }
        return BindResult.SUCCESS;
    }

    public static TransferResult tryUseEnergy(
            VoidEnergyTransferBlockEntity source,
            VoidEnergyTransferBlockEntity target,
            long requestedEnergy
    ) {
        if (source == null || target == null || requestedEnergy <= 0L) {
            return TransferResult.failed(BindingStatus.NOT_FUNCTIONAL);
        }
        if (!source.canExtractVoidEnergy()) {
            return TransferResult.failed(BindingStatus.SOURCE_CANNOT_OUTPUT);
        }
        if (!target.canReceiveVoidEnergy()) {
            return TransferResult.failed(BindingStatus.TARGET_CANNOT_INPUT);
        }

        long request = Math.min(requestedEnergy, source.getMaxVoidEnergyOutputPerTransfer());
        request = Math.min(request, target.getMaxVoidEnergyInputPerTransfer());
        long simulatedExtract = source.extractVoidEnergy(request, true);
        long simulatedInsert = target.receiveVoidEnergy(simulatedExtract, true);
        long moved = Math.min(simulatedExtract, simulatedInsert);
        if (moved <= 0L) {
            return TransferResult.failed(BindingStatus.NO_ENERGY_MOVED);
        }

        long extracted = source.extractVoidEnergy(moved, false);
        long inserted = target.receiveVoidEnergy(extracted, false);
        if (inserted < extracted) {
            source.receiveVoidEnergy(extracted - inserted, false);
        }

        return TransferResult.success(inserted);
    }

    public static BindingStatus describeOutputBinding(MinecraftServer server, VoidEnergyTransferBlockEntity owner, VoidEnergyBinding binding) {
        ResolveResult target = resolve(server, binding.target());
        if (target.status() != BindingStatus.OK) {
            return target.status();
        }
        if (!target.endpoint().canReceiveVoidEnergy()) {
            return BindingStatus.TARGET_CANNOT_INPUT;
        }
        if (!target.endpoint().hasInputSource(owner.getVoidPosition())) {
            return BindingStatus.NOT_RECIPROCAL;
        }
        return BindingStatus.OK;
    }

    public static BindingStatus describeInputBinding(MinecraftServer server, VoidEnergyTransferBlockEntity owner, VoidEnergyBinding binding) {
        ResolveResult source = resolve(server, binding.target());
        if (source.status() != BindingStatus.OK) {
            return source.status();
        }
        if (!source.endpoint().canExtractVoidEnergy()) {
            return BindingStatus.SOURCE_CANNOT_OUTPUT;
        }
        if (!source.endpoint().hasOutputTarget(owner.getVoidPosition())) {
            return BindingStatus.NOT_RECIPROCAL;
        }
        return BindingStatus.OK;
    }

    public static void removeOutputBinding(MinecraftServer server, VoidEnergyTransferBlockEntity source, BoundVoidPosition target) {
        BoundVoidPosition sourcePos = source.getVoidPosition();
        source.removeOutputTarget(target);
        ResolveResult targetResult = resolve(server, target);
        if (targetResult.status() == BindingStatus.OK) {
            targetResult.endpoint().removeInputSource(sourcePos);
        }
    }

    public static void removeInputBinding(MinecraftServer server, VoidEnergyTransferBlockEntity target, BoundVoidPosition source) {
        BoundVoidPosition targetPos = target.getVoidPosition();
        target.removeInputSource(source);
        ResolveResult sourceResult = resolve(server, source);
        if (sourceResult.status() == BindingStatus.OK) {
            sourceResult.endpoint().removeOutputTarget(targetPos);
        }
    }

    public enum BindResult {
        SUCCESS("message.void_craft.coordinate_designator.bound"),
        SAME_BLOCK("message.void_craft.coordinate_designator.same_block"),
        POSITION_CHANGED("message.void_craft.coordinate_designator.position_changed"),
        DUPLICATE("message.void_craft.coordinate_designator.duplicate"),
        SOURCE_CANNOT_OUTPUT("message.void_craft.coordinate_designator.source_cannot_output"),
        TARGET_CANNOT_INPUT("message.void_craft.coordinate_designator.target_cannot_input"),
        SOURCE_OUTPUT_FULL("message.void_craft.coordinate_designator.source_output_full"),
        TARGET_INPUT_FULL("message.void_craft.coordinate_designator.target_input_full");

        private final String translationKey;

        BindResult(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public enum BindingStatus {
        OK("message.void_craft.coordinate_designator.status.ok"),
        UNLOADED("message.void_craft.coordinate_designator.status.unloaded"),
        NOT_FUNCTIONAL("message.void_craft.coordinate_designator.status.not_functional"),
        SOURCE_CANNOT_OUTPUT("message.void_craft.coordinate_designator.status.source_cannot_output"),
        TARGET_CANNOT_INPUT("message.void_craft.coordinate_designator.status.target_cannot_input"),
        NOT_RECIPROCAL("message.void_craft.coordinate_designator.status.not_reciprocal"),
        NO_ENERGY_MOVED("message.void_craft.coordinate_designator.status.no_energy_moved");

        private final String translationKey;

        BindingStatus(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public record ResolveResult(BindingStatus status, VoidEnergyTransferBlockEntity endpoint) {
        public static ResolveResult loaded(VoidEnergyTransferBlockEntity endpoint) {
            return new ResolveResult(BindingStatus.OK, endpoint);
        }

        public static ResolveResult unloaded() {
            return new ResolveResult(BindingStatus.UNLOADED, null);
        }

        public static ResolveResult invalid() {
            return new ResolveResult(BindingStatus.NOT_FUNCTIONAL, null);
        }
    }

    public record TransferResult(boolean success, long movedEnergy, BindingStatus status) {
        public static TransferResult success(long movedEnergy) {
            return new TransferResult(true, movedEnergy, BindingStatus.OK);
        }

        public static TransferResult failed(BindingStatus status) {
            return new TransferResult(false, 0L, status);
        }
    }
}
