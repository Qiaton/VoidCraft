package com.example.voidcraft.ClientCustom.Event;

import com.example.voidcraft.ClientCustom.Key.ModKeyMappings;
import com.example.voidcraft.Custom.Clock.VoidClock;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleItem;
import com.example.voidcraft.ModAttachments;
import com.example.voidcraft.VoidCraft;
import com.example.voidcraft.network.UseGauntletModulePayload;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = VoidCraft.MODID,value = Dist.CLIENT)
public class ClientKeyEvents {
    @SubscribeEvent
    public static void ON_CLIENT_TICK(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;
        while (ModKeyMappings.SKILL_KEY_1.consumeClick()) {
            ClientPacketDistributor.sendToServer(new UseGauntletModulePayload(0));
            System.out.println("按键触发 slot0");
        }

        while (ModKeyMappings.SKILL_KEY_2.consumeClick()) {
            ClientPacketDistributor.sendToServer(new UseGauntletModulePayload(1));
            System.out.println("按键触发 slot1");
        }
    }

}
