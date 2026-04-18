package com.example.voidcraft.ClientCustom;

import com.example.voidcraft.VoidCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

import static net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3;

@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class SpatialSwordEffect {
    public static Boolean IN_VOID = false;

    @SubscribeEvent
    public static void EFFECT(ComputeFovModifierEvent e){
        if(IN_VOID){
            e.setNewFovModifier(1.0F);
        }

    }




}

