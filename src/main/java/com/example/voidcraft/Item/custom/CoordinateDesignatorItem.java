package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Block.entity.ChunkMapperBlockEntity;
import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransfer;
import com.example.voidcraft.Custom.Behavior.Energy.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.Network.ModNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CoordinateDesignatorItem extends Item {
    // 区块映射器只有一个输入口；覆盖前给玩家一个短时间二次确认。
    private static final long CHUNK_MAPPER_OVERWRITE_CONFIRM_TICKS = 100L;
    private static final Map<UUID, PendingChunkMapperInputOverwrite> PENDING_CHUNK_MAPPER_OVERWRITES = new HashMap<>();

    public CoordinateDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        if (player.isSecondaryUseActive()) {
            // 潜行右键方块只切模式，不参与绑定。
            switchMode(stack, player);
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity clickedBlockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(clickedBlockEntity instanceof VoidEnergyTransferBlockEntity clickedEndpoint)) {
            // 点到非虚空能方块时清掉已记录的第一点，避免下次误连。
            sendActionbar(player, Component.translatable("message.void_craft.coordinate_designator.use_functional_block"));
            setData(stack, getData(stack).clearFirstTarget());
            return InteractionResult.SUCCESS;
        }

        CoordinateDesignatorData data = getData(stack);
        BoundVoidPosition clicked = BoundVoidPosition.of(level, context.getClickedPos());
        if (data.mode() == CoordinateDesignatorData.Mode.UNBIND) {
            // 解绑模式不记录两点，直接打开当前方块的连接列表。
            setData(stack, data.clearFirstTarget());
            ModNetworking.sendCoordinateBindings(serverPlayer, clicked, clickedEndpoint);
            return InteractionResult.CONSUME;
        }

        Optional<BoundVoidPosition> firstTarget = data.firstTarget();
        if (firstTarget.isEmpty()) {
            // 第一次点击只保存坐标；第二次点击才真正创建连接。
            setData(stack, data.withFirstTarget(clicked));
            sendActionbar(player, Component.translatable("message.void_craft.coordinate_designator.first_recorded"));
            return InteractionResult.SUCCESS;
        }

        BoundVoidPosition first = firstTarget.get();
        MinecraftServer server = serverPlayer.level().getServer();
        ChunkMapperOverwriteTarget overwriteTarget = getChunkMapperOverwriteTarget(server, first, clicked, data.mode());
        if (overwriteTarget != null && !hasConfirmedChunkMapperOverwrite(serverPlayer, overwriteTarget, data.mode())) {
            // 映射器已有输入口时先提示，玩家再次点同一组方块才覆盖。
            PENDING_CHUNK_MAPPER_OVERWRITES.put(
                    serverPlayer.getUUID(),
                    new PendingChunkMapperInputOverwrite(
                            overwriteTarget.source(),
                            overwriteTarget.target(),
                            data.mode(),
                            level.getGameTime() + CHUNK_MAPPER_OVERWRITE_CONFIRM_TICKS
                    )
            );
            sendActionbar(player, Component.translatable("message.void_craft.chunk_mapper.input_overwrite_confirm"));
            return InteractionResult.SUCCESS;
        }

        // replaceTargetInput 只在二次确认通过时为 true，用来覆盖映射器旧输入。
        VoidEnergyTransfer.BindResult result = VoidEnergyTransfer.bind(server, first, clicked, data.mode(), overwriteTarget != null);
        PENDING_CHUNK_MAPPER_OVERWRITES.remove(serverPlayer.getUUID());
        setData(stack, data.clearFirstTarget());
        sendActionbar(player, Component.translatable(result.translationKey()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NonNull InteractionResultHolder<ItemStack> use(Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (player.isSecondaryUseActive()) {
                switchMode(stack, player);
            } else {
                sendActionbar(player, Component.translatable("message.void_craft.coordinate_designator.use_functional_block"));
                setData(stack, getData(stack).clearFirstTarget());
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        CoordinateDesignatorData data = getData(stack);
        tooltip.add(Component.translatable(
                "tooltip.void_craft.coordinate_designator.mode",
                data.mode().getDisplayName()
        ));
        data.firstTarget().ifPresent(target -> tooltip.add(Component.translatable(
                "tooltip.void_craft.coordinate_designator.first_target",
                target.shortText()
        )));
    }

    public static CoordinateDesignatorData getData(ItemStack stack) {
        CoordinateDesignatorData data = stack.get(ModDataComponents.COORDINATE_DESIGNATOR_DATA.get());
        return data == null ? CoordinateDesignatorData.DEFAULT : data;
    }

    private static void switchMode(ItemStack stack, Player player) {
        CoordinateDesignatorData.Mode nextMode = getData(stack).mode().next();
        setData(stack, CoordinateDesignatorData.DEFAULT.withMode(nextMode));
        sendActionbar(player, Component.translatable(
                "message.void_craft.coordinate_designator.mode_changed",
                nextMode.getDisplayName()
        ));
    }

    private static void setData(ItemStack stack, CoordinateDesignatorData data) {
        // 数据组件负责保存模式，CustomModelData 负责让物品模型跟着模式切换。
        stack.set(ModDataComponents.COORDINATE_DESIGNATOR_DATA.value(), data);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data.mode().ordinal() + 1));
    }

    private static void sendActionbar(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static ChunkMapperOverwriteTarget getChunkMapperOverwriteTarget(
            MinecraftServer server,
            BoundVoidPosition primary,
            BoundVoidPosition secondary,
            CoordinateDesignatorData.Mode mode
    ) {
        // 先按当前模式算出真正的供能端和收能端。
        BoundVoidPosition source;
        BoundVoidPosition target;
        if (mode == CoordinateDesignatorData.Mode.INPUT) {
            source = secondary;
            target = primary;
        } else if (mode == CoordinateDesignatorData.Mode.OUTPUT) {
            source = primary;
            target = secondary;
        } else {
            return null;
        }

        VoidEnergyTransfer.ResolveResult sourceResult = VoidEnergyTransfer.resolve(server, source);
        VoidEnergyTransfer.ResolveResult targetResult = VoidEnergyTransfer.resolve(server, target);
        if (sourceResult.status() != VoidEnergyTransfer.BindingStatus.OK
                || targetResult.status() != VoidEnergyTransfer.BindingStatus.OK
                || !(targetResult.endpoint() instanceof ChunkMapperBlockEntity mapper)) {
            return null;
        }

        // 只有“来源能输出，并且目标是已满输入的区块映射器”时才需要覆盖确认。
        VoidEnergyTransferBlockEntity sourceEndpoint = sourceResult.endpoint();
        if (!sourceEndpoint.canExtractVoidEnergy()
                || (!sourceEndpoint.hasOutputTarget(target) && !sourceEndpoint.canAddOutputTarget(target))) {
            return null;
        }

        if (mapper.hasInputSource(source) || mapper.getInputSources().isEmpty() || mapper.canAddInputSource(source)) {
            return null;
        }

        return new ChunkMapperOverwriteTarget(source, target);
    }

    private static boolean hasConfirmedChunkMapperOverwrite(
            ServerPlayer player,
            ChunkMapperOverwriteTarget target,
            CoordinateDesignatorData.Mode mode
    ) {
        // 确认必须来自同一玩家、同一模式、同一组来源/目标，并且还没过期。
        PendingChunkMapperInputOverwrite pending = PENDING_CHUNK_MAPPER_OVERWRITES.get(player.getUUID());
        if (pending == null || pending.expiresAt() < player.level().getGameTime()) {
            return false;
        }
        return pending.mode() == mode
                && pending.source().sameBlock(target.source())
                && pending.target().sameBlock(target.target());
    }

    private record ChunkMapperOverwriteTarget(BoundVoidPosition source, BoundVoidPosition target) {
    }

    private record PendingChunkMapperInputOverwrite(
            BoundVoidPosition source,
            BoundVoidPosition target,
            CoordinateDesignatorData.Mode mode,
            long expiresAt
    ) {
    }
}
