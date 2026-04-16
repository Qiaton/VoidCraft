package com.example.testmod2.ClientCustom;

import com.example.testmod2.ModAttachments;
import com.example.testmod2.TestMod2;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import static net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3;

@EventBusSubscriber(modid = TestMod2.MODID, value = Dist.CLIENT)
public class SpatialSwordEffect {
    public static Boolean IN_VOID = false;

    @SubscribeEvent
    public static void EFFECT(ComputeFovModifierEvent e){
        if(IN_VOID){
            e.setNewFovModifier(1.0F);
        }

    }




}

