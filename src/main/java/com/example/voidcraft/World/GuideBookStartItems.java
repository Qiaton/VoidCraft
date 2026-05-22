package com.example.voidcraft.World;

import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = VoidCraft.MODID)
public final class GuideBookStartItems {
    private GuideBookStartItems() {
    }

    @SubscribeEvent
    public static void giveGuideBook(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getData(ModAttachments.GUIDE_BOOK_GIVEN.get())) {
            return;
        }

        ItemStack stack = new ItemStack(ModItem.GUIDE_BOOK.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.setData(ModAttachments.GUIDE_BOOK_GIVEN.get(), true);
    }
}
