package com.example.voidcraft.Custom.Clock.ModuleSkill;

import com.example.voidcraft.Custom.Clock.ModuleSkillClock;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.VoidModule;
import com.example.voidcraft.Item.custom.PhaseWatch;
import com.example.voidcraft.ModAttachments;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class VoidModuleClock {

    @SubscribeEvent
    public static void tickVoidSkill(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack watchStack = player.getOffhandItem();

        if (!(watchStack.getItem() instanceof PhaseWatch)) {
            return;
        }

        ItemContainerContents contents =
                watchStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        NonNullList<ItemStack> items =
                NonNullList.withSize(PhaseWatch.WATCH_MODULE_SLOT_COUNT, ItemStack.EMPTY);

        contents.copyInto(items);

        for (int slot = 0; slot < PhaseWatch.WATCH_MODULE_SLOT_COUNT; slot++) {
            if (!ModuleSkillClock.hasChannel(player, slot)) {
                continue;
            }

            ItemStack moduleStack = items.get(slot);

            if (moduleStack.isEmpty()) {
                continue;
            }

            if (moduleStack.getItem() instanceof VoidModule) {
                VoidModule.Stats stats = VoidModule.getStats(moduleStack);

                if (stats == null) {
                    continue;
                }

                player.setData(ModAttachments.VOID_SPEED.get(), stats.voidSpeed());
                VoidClock.setVoidTicks(player, 2);
                return;
            }
        }
    }
}
