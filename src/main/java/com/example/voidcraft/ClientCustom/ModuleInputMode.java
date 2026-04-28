package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import static com.example.voidcraft.Item.custom.PhaseWatch.WATCH_MODULE_SLOT_COUNT;

public enum ModuleInputMode {
    CLICK,
    HOLD_RELEASE;
    public static ModuleInputMode getInputTypeFromSlot(Minecraft mc, int slot) {
        Player player = mc.player;
        if (player != null) {
            ItemStack itemStack = player.getOffhandItem();
           ItemContainerContents contents = itemStack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
            NonNullList<ItemStack> items = NonNullList.withSize(WATCH_MODULE_SLOT_COUNT, ItemStack.EMPTY);
            contents.copyInto(items);
            ItemStack moduleStack = items.get(slot);
           if(moduleStack.getItem() instanceof ModuleItem moduleItem){
               return moduleItem.getInputMode();
           }
        }
        return CLICK;
    }
}
