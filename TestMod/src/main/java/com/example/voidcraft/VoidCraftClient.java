package com.example.voidcraft;

import com.example.voidcraft.ClientCustom.FlowEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;


// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = VoidCraft.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class VoidCraftClient {
    @SubscribeEvent
    public static void ClientFlowFov(net.neoforged.neoforge.client.event.ClientTickEvent.Post  event) {
        if(FlowEffect.fov_effect>1.38){             //控制视角缩放效果
            FlowEffect.fov_effect-=0.01F;
        }
        else if(FlowEffect.fov_effect>0.01){
            FlowEffect.fov_effect-=0.5F;
        }
    }

}
