package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Container.WatchModuleContainer;
import com.example.voidcraft.Gui.ModuleMenu;
import com.example.voidcraft.Item.ModItem;
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
    public static final int WATCH_CORE_SLOT = 2;              // 能量核心固定放在第 3 槽，避免影响 0/1 模块槽
    public static final int WATCH_CONTAINER_SLOT_COUNT = 3;
    private static final long BASE_MAX_ENERGY = 1_000L;
    private static final long MIN_ENERGY = -10_000L;

    public PhaseWatch(Properties properties) {
        super(properties.component(
                DataComponents.TOOLTIP_DISPLAY,
                new TooltipDisplay(
                        false,
                        new LinkedHashSet<>(List.of(DataComponents.CONTAINER))
                )
        ));

    }

    public static long getMaxEnergy(ItemStack watchStack) {
        if (watchStack.isEmpty() || !(watchStack.getItem() instanceof PhaseWatch watch)) {
            return 0L;
        }
        return watch.getBaseMaxEnergy(watchStack);
    }

    protected long getBaseMaxEnergy(ItemStack watchStack) {
        return BASE_MAX_ENERGY;
    }

    public static long getEnergy(ItemStack watchStack) {
        long maxEnergy = getMaxEnergy(watchStack);
        if (maxEnergy <= 0L) {
            return 0L;
        }

        WatchEnergyData data = watchStack.get(ModDataComponents.WATCH_ENERGY.get());
        if (data == null) {
            return maxEnergy;
        }
        return clamp(data.energy(), MIN_ENERGY, maxEnergy);
    }

    public static void setEnergy(ItemStack watchStack, long energy) {
        long maxEnergy = getMaxEnergy(watchStack);
        if (maxEnergy <= 0L) {
            return;
        }
        watchStack.set(ModDataComponents.WATCH_ENERGY.value(), new WatchEnergyData(clamp(energy, MIN_ENERGY, maxEnergy)));
    }
    public static RechargeResult rechargeFromCore(ItemStack watchStack) {
        long maxEnergy = getMaxEnergy(watchStack);
        if (maxEnergy <= 0L) {
            return RechargeResult.NO_WATCH;
        }

        long currentEnergy = getEnergy(watchStack);
        if (currentEnergy >= maxEnergy) {
            return RechargeResult.FULL;
        }

        NonNullList<ItemStack> items = getContainerItems(watchStack, WATCH_CONTAINER_SLOT_COUNT);
        ItemStack coreStack = items.get(WATCH_CORE_SLOT);
        if (!(coreStack.getItem() instanceof EnergyCoreItem coreItem)) {
            return RechargeResult.MISSING_CORE;
        }

        if (coreItem.getCurrentMaxLifetime(coreStack) <= 0L) {
            replaceCoreWithResidue(watchStack, items);
            return RechargeResult.CORE_SCRAPPED;
        }

        if (coreItem.getCurrentLifetime(coreStack) <= 0L) {
            return RechargeResult.CORE_DEPLETED;
        }

        long actualRecovered = Math.min(coreItem.getRechargePerTick(), maxEnergy - currentEnergy);
        if (actualRecovered <= 0L) {
            return RechargeResult.FULL;
        }

        setEnergy(watchStack, currentEnergy + actualRecovered);
        EnergyCoreItem.WearResult wearResult = coreItem.applyRecoveredEnergy(coreStack, actualRecovered);

        if (wearResult.scrapped()) {
            replaceCoreWithResidue(watchStack, items);
            return RechargeResult.CORE_SCRAPPED;
        }

        items.set(WATCH_CORE_SLOT, coreStack);
        writeContainerItems(watchStack, items);

        if (wearResult.currentLifetime() <= 0L) {
            return RechargeResult.CORE_DEPLETED;
        }

        return RechargeResult.RECHARGED;
    }

    public static void useModule(ServerPlayer serverPlayer, ItemStack watchStack, int slot) {
        if (serverPlayer == null) return;
        if (watchStack.isEmpty()) return;
        if (!(watchStack.getItem() instanceof PhaseWatch)) return;
        if (slot < 0 || slot >= WATCH_MODULE_SLOT_COUNT) return;
        NonNullList<ItemStack> items = getContainerItems(watchStack, WATCH_MODULE_SLOT_COUNT);
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
       NonNullList<ItemStack> items = getContainerItems(stack, WATCH_MODULE_SLOT_COUNT);
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

    private static NonNullList<ItemStack> getContainerItems(ItemStack watchStack, int size) {
        ItemContainerContents contents = watchStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        contents.copyInto(items);
        return items;
    }

    private static void writeContainerItems(ItemStack watchStack, NonNullList<ItemStack> items) {
        watchStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    private static void replaceCoreWithResidue(ItemStack watchStack, NonNullList<ItemStack> items) {
        items.set(WATCH_CORE_SLOT, new ItemStack(ModItem.ENERGY_CORE_RESIDUE.get()));
        writeContainerItems(watchStack, items);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum RechargeResult {
        NO_WATCH,
        FULL,
        MISSING_CORE,
        CORE_DEPLETED,
        CORE_SCRAPPED,
        RECHARGED
    }
}
