package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Block.entity.BoundVoidPosition;
import com.example.voidcraft.Block.entity.VoidEnergyTransfer;
import com.example.voidcraft.Block.entity.VoidEnergyTransferBlockEntity;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.network.ModNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CoordinateDesignatorItem extends Item {
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
            switchMode(stack, player);
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity clickedBlockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(clickedBlockEntity instanceof VoidEnergyTransferBlockEntity clickedEndpoint)) {
            sendActionbar(player, Component.translatable("message.void_craft.coordinate_designator.use_functional_block"));
            setData(stack, getData(stack).clearFirstTarget());
            return InteractionResult.SUCCESS;
        }

        CoordinateDesignatorData data = getData(stack);
        BoundVoidPosition clicked = BoundVoidPosition.of(level, context.getClickedPos());
        if (data.mode() == CoordinateDesignatorData.Mode.UNBIND) {
            setData(stack, data.clearFirstTarget());
            ModNetworking.sendCoordinateBindings(serverPlayer, clicked, clickedEndpoint);
            return InteractionResult.CONSUME;
        }

        Optional<BoundVoidPosition> firstTarget = data.firstTarget();
        if (firstTarget.isEmpty()) {
            setData(stack, data.withFirstTarget(clicked));
            sendActionbar(player, Component.translatable("message.void_craft.coordinate_designator.first_recorded"));
            return InteractionResult.SUCCESS;
        }

        BoundVoidPosition first = firstTarget.get();
        VoidEnergyTransfer.BindResult result = VoidEnergyTransfer.bind(serverPlayer.level().getServer(), first, clicked, data.mode());
        setData(stack, data.clearFirstTarget());
        sendActionbar(player, Component.translatable(result.translationKey()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NonNull InteractionResult use(Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (player.isSecondaryUseActive()) {
                switchMode(stack, player);
            } else {
                sendActionbar(player, Component.translatable("message.void_craft.coordinate_designator.use_functional_block"));
                setData(stack, getData(stack).clearFirstTarget());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        CoordinateDesignatorData data = getData(stack);
        tooltip.accept(Component.translatable(
                "tooltip.void_craft.coordinate_designator.mode",
                data.mode().getDisplayName()
        ));
        data.firstTarget().ifPresent(target -> tooltip.accept(Component.translatable(
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
        stack.set(ModDataComponents.COORDINATE_DESIGNATOR_DATA.value(), data);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(),
                List.of(),
                List.of(data.mode().getId()),
                List.of()
        ));
    }

    private static void sendActionbar(Player player, Component message) {
        player.displayClientMessage(message, true);
    }
}
