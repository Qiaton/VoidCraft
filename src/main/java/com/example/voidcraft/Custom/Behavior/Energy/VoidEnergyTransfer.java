package com.example.voidcraft.Custom.Behavior.Energy;

import com.example.voidcraft.Item.custom.CoordinateDesignatorData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class VoidEnergyTransfer {
    private VoidEnergyTransfer() {
    }

    // 根据存下来的维度和坐标找到真实方块实体；区块没加载时先不判坏。
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
        // 两次点到同一个方块没有意义，直接给玩家提示。
        if (primary.sameBlock(secondary)) {
            return BindResult.SAME_BLOCK;
        }

        BoundVoidPosition source;
        BoundVoidPosition target;
        VoidEnergyBindingType bindingType;

        // INPUT 模式是“当前方块从第一次记录的方块拿电”，OUTPUT 模式反过来。
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

        // 方块被拆掉或两端都没加载时，不写入新的坏绑定。
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
        // 输出端和输入端都要各自有容量；区块映射器这类单输入方块可走二次确认覆盖。
        if (sourceEndpoint != null && !sourceHasOutput && !sourceEndpoint.canAddOutputTarget(target)) {
            return BindResult.SOURCE_OUTPUT_FULL;
        }
        if (targetEndpoint != null && !targetHasInput && !targetEndpoint.canAddInputSource(source) && !shouldReplaceTargetInput) {
            return BindResult.TARGET_INPUT_FULL;
        }

        if (targetEndpoint != null && shouldReplaceTargetInput) {
            // 覆盖输入时要同时清掉旧来源的输出列表，保持两端列表互相对应。
            for (VoidEnergyBinding binding : List.copyOf(targetEndpoint.getInputSources())) {
                removeInputBinding(server, targetEndpoint, binding.target());
            }
        }

        // 真正写入绑定时，两端各记一份；之后校验和移除才能从任意一端操作。
        if (sourceEndpoint != null && !sourceHasOutput) {
            sourceEndpoint.addOutputTarget(new VoidEnergyBinding(target, bindingType));
        }
        if (targetEndpoint != null && !targetHasInput) {
            targetEndpoint.addInputSource(new VoidEnergyBinding(source, bindingType));
        }
        return BindResult.SUCCESS;
    }

    public static boolean shouldRunTransfer(Level level, VoidEnergyTransferBlockEntity endpoint) {
        return level != null
                && !level.isClientSide()
                && level.getGameTime() % endpoint.getVoidEnergyProfile().transferIntervalTicks() == 0L;
    }

    public static long pushToOutputTargets(VoidEnergyTransferBlockEntity source) {
        Level level = source.getVoidEnergyLevel();
        if (level == null || level.isClientSide() || level.getServer() == null || !source.canExtractVoidEnergy()) {
            return 0L;
        }

        long availableEnergy = source.extractVoidEnergy(source.getVoidEnergyStored(), true);
        if (availableEnergy <= 0L) {
            return 0L;
        }

        List<TargetNeed> requests = getTargetRequests(level, source);
        if (requests.isEmpty()) {
            return 0L;
        }

        long[] allocations = splitEnergy(availableEnergy, requests);
        long movedEnergy = 0L;
        for (int i = 0; i < requests.size(); i++) {
            long allocation = allocations[i];
            if (allocation <= 0L) {
                continue;
            }

            long extracted = source.extractVoidEnergy(allocation, false);
            if (extracted <= 0L) {
                break;
            }

            long inserted = requests.get(i).target().receiveVoidEnergy(extracted, false);
            movedEnergy += inserted;
            if (inserted < extracted) {
                source.receiveVoidEnergy(extracted - inserted, false);
            }
        }
        return movedEnergy;
    }

    private static List<TargetNeed> getTargetRequests(Level level, VoidEnergyTransferBlockEntity source) {
        List<VoidEnergyBinding> bindings = List.copyOf(source.getOutputTargets());
        List<TargetNeed> requests = new ArrayList<>();
        if (bindings.isEmpty()) {
            return requests;
        }

        BoundVoidPosition sourcePosition = source.getVoidPosition();
        int start = getStartIndex(level, source, bindings.size());
        for (int step = 0; step < bindings.size(); step++) {
            VoidEnergyBinding binding = bindings.get((start + step) % bindings.size());
            ResolveResult targetResult = resolve(level.getServer(), binding.target());
            if (targetResult.status() == BindingStatus.UNLOADED) {
                continue;
            }
            if (targetResult.status() != BindingStatus.OK) {
                source.removeOutputTarget(binding.target());
                continue;
            }

            VoidEnergyTransferBlockEntity target = targetResult.endpoint();
            if (!target.canReceiveVoidEnergy()) {
                source.removeOutputTarget(binding.target());
                target.removeInputSource(sourcePosition);
                continue;
            }

            if (!target.hasInputSource(sourcePosition)) {
                source.removeOutputTarget(binding.target());
                continue;
            }

            long targetRequest = Math.min(
                    target.getMaxVoidEnergyInputPerTransfer(),
                    Math.max(0L, target.getVoidEnergyCapacity() - target.getVoidEnergyStored())
            );
            targetRequest = target.receiveVoidEnergy(targetRequest, true);
            if (targetRequest > 0L) {
                requests.add(new TargetNeed(target, targetRequest));
            }
        }
        return requests;
    }

    private static int getStartIndex(Level level, VoidEnergyTransferBlockEntity source, int size) {
        int interval = source.getVoidEnergyProfile().transferIntervalTicks();
        long transferTurn = level.getGameTime() / Math.max(1, interval);
        return (int) (transferTurn % size);
    }

    private static long[] splitEnergy(long availableEnergy, List<TargetNeed> requests) {
        long[] allocations = new long[requests.size()];
        long remainingEnergy = availableEnergy;
        while (remainingEnergy > 0L) {
            int activeTargets = 0;
            for (int i = 0; i < requests.size(); i++) {
                if (allocations[i] < requests.get(i).needEnergy()) {
                    activeTargets++;
                }
            }

            if (activeTargets <= 0) {
                break;
            }

            long share = Math.max(1L, remainingEnergy / activeTargets);
            boolean movedAny = false;
            for (int i = 0; i < requests.size() && remainingEnergy > 0L; i++) {
                long need = requests.get(i).needEnergy() - allocations[i];
                if (need <= 0L) {
                    continue;
                }

                long amount = Math.min(need, share);
                amount = Math.min(amount, remainingEnergy);
                allocations[i] += amount;
                remainingEnergy -= amount;
                movedAny = true;
            }

            if (!movedAny) {
                break;
            }
        }
        return allocations;
    }

    public static BindingStatus describeOutputBinding(MinecraftServer server, VoidEnergyTransferBlockEntity owner, VoidEnergyBinding binding) {
        // 状态面板用这个检查输出列表：目标是否存在、能否输入、是否也记着我。
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
        // 状态面板用这个检查输入列表：来源是否存在、能否输出、是否也连着我。
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
        // 从输出端删除时，也顺手删掉目标输入端里对应的来源。
        BoundVoidPosition sourcePos = source.getVoidPosition();
        source.removeOutputTarget(target);
        ResolveResult targetResult = resolve(server, target);
        if (targetResult.status() == BindingStatus.OK) {
            targetResult.endpoint().removeInputSource(sourcePos);
        }
    }

    public static void removeInputBinding(MinecraftServer server, VoidEnergyTransferBlockEntity target, BoundVoidPosition source) {
        // 从输入端删除时，也顺手删掉来源输出端里对应的目标。
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
        NOT_RECIPROCAL("message.void_craft.coordinate_designator.status.not_reciprocal");

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

    private record TargetNeed(VoidEnergyTransferBlockEntity target, long needEnergy) {
    }
}
