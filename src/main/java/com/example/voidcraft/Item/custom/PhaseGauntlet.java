package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Container.GauntletModuleContainer;
import com.example.voidcraft.Gui.ModuleMenu;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleMode;
import com.example.voidcraft.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class PhaseGauntlet extends Item {
    public static final int GAUNTLET_MODULE_SLOT_COUNT = 2;      //手套内模块插槽数
    public PhaseGauntlet(Properties properties) {
        super(properties.component(
                DataComponents.TOOLTIP_DISPLAY,
                new TooltipDisplay(
                        false,
                        new java.util.LinkedHashSet<>(java.util.List.of(DataComponents.CONTAINER))
                )
        ));

    }

    public static void useModule(ServerPlayer serverPlayer, ItemStack gauntletStack, int slot) {
        if (serverPlayer == null) return;
        System.out.println("手套使用：玩家检验完毕");
        if (gauntletStack.isEmpty()) return;
        System.out.println("手套使用：手套道具是否为空检验完毕");
        if (!(gauntletStack.getItem() instanceof PhaseGauntlet)) return;
        System.out.println("手套使用：手套道具类型符合检验完毕");
        if (slot < 0 || slot >= GAUNTLET_MODULE_SLOT_COUNT) return;
        System.out.println("手套使用：槽位检验完毕");
        ItemContainerContents contents = gauntletStack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(GAUNTLET_MODULE_SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(items);
        ItemStack moduleStack = items.get(slot);
        if(moduleStack.isEmpty()) return;
        System.out.println("手套使用：模块是否为空检验完毕");
        if(moduleStack.getItem() instanceof ModuleItem moduleItem) {
            moduleItem.useSkill(serverPlayer, gauntletStack, moduleStack, slot);
            System.out.println("手套使用：模块道具类型符合检验完毕 已触发技能事件");
        }

    }

    @Override
    public @NonNull InteractionResult use(Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        if(!level.isClientSide() && player instanceof ServerPlayer serverPlayer){
            ItemStack itemStack = player.getItemInHand(hand);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId,playerInventory,openingPlayer)->
                            new ModuleMenu(containerId,playerInventory,new GauntletModuleContainer(itemStack)),//创建菜单方法
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
       NonNullList<ItemStack> items = NonNullList.withSize(GAUNTLET_MODULE_SLOT_COUNT, ItemStack.EMPTY);
       contents.copyInto(items);
       for(ItemStack itemStack : items){
           if(!itemStack.isEmpty()){
               ModuleData data = itemStack.get(ModDataComponents.MODULE_DATA.get());
               if(data != null){
                   ModuleMode mode = data.moduleMode();
                   tooltip.accept(Component.translatable("item.voidcraft.phase_gauntlet.module",mode.getDisplayName()));
               }

           }
       }

    }
}
