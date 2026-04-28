package com.example.voidcraft.Custom;

import com.example.voidcraft.Item.custom.PhaseWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class ModuleSlotHelper {
    private ModuleSlotHelper(){}
    public static ItemStack getModuleStackFromSlot(Minecraft mc, int slot) {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        }

        if (slot < 0 || slot >= PhaseWatch.WATCH_MODULE_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        ItemStack gauntletStack = mc.player.getOffhandItem();

        ItemContainerContents contents = gauntletStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );

        NonNullList<ItemStack> items = NonNullList.withSize(
                PhaseWatch.WATCH_MODULE_SLOT_COUNT,
                ItemStack.EMPTY
        );

        contents.copyInto(items);

        return items.get(slot);
    }
}
