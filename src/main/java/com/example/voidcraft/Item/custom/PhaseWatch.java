package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Container.WatchModuleContainer;
import com.example.voidcraft.Gui.ModuleMenu;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

public class PhaseWatch extends Item {
    public static final int WATCH_MODULE_SLOT_COUNT = 2;      //手套内模块插槽数
    public PhaseWatch(Properties properties) {
        super(properties.component(
                DataComponents.TOOLTIP_DISPLAY,
                new TooltipDisplay(
                        false,
                        new LinkedHashSet<>(List.of(DataComponents.CONTAINER))
                )
        ));

    }

    public static void useModule(ServerPlayer serverPlayer, ItemStack watchStack, int slot) {
        if (serverPlayer == null) return;
        if (watchStack.isEmpty()) return;
        if (!(watchStack.getItem() instanceof PhaseWatch)) return;
        if (slot < 0 || slot >= WATCH_MODULE_SLOT_COUNT) return;
        ItemContainerContents contents = watchStack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(WATCH_MODULE_SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(items);
        ItemStack moduleStack = items.get(slot);
        if(moduleStack.isEmpty()) return;
        if(moduleStack.getItem() instanceof ModuleItem moduleItem) {
            moduleItem.useSkill(serverPlayer, watchStack, moduleStack, slot);
        }

    }

    @Override
    public @NonNull InteractionResult use(Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        if (Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }

        if(!level.isClientSide() && player instanceof ServerPlayer serverPlayer){
            ItemStack itemStack = player.getItemInHand(hand);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId,playerInventory,openingPlayer)->
                            new ModuleMenu(containerId,playerInventory,new WatchModuleContainer(itemStack)),//创建菜单方法
                    Component.literal("Modules")                                                //菜单标题名
            ));
        }

        return InteractionResult.CONSUME;
    }
    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
       NonNullList<ItemStack> items = NonNullList.withSize(WATCH_MODULE_SLOT_COUNT, ItemStack.EMPTY);
       contents.copyInto(items);
       for(ItemStack itemStack : items){
           if(!itemStack.isEmpty()){
               ModuleData data = itemStack.get(ModDataComponents.MODULE_DATA.get());
               if(data != null){
                   ModuleMode mode = data.moduleMode();
                   tooltip.accept(Component.translatable("item.voidcraft.phase_watch.module",mode.getDisplayName()));
               }

           }
       }

    }
}
