package com.example.voidcraft.ClientCustom.Key;

import com.example.voidcraft.VoidCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = VoidCraft.MODID,
        value = Dist.CLIENT
)
public class ModKeyMappings {

    public static final KeyMapping SKILL_KEY_1 = new KeyMapping(
            "key.voidcraft.skill1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            KeyMapping.CATEGORY_GAMEPLAY
    );

    public static final KeyMapping SKILL_KEY_2 = new KeyMapping(
            "key.voidcraft.skill2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyMapping.CATEGORY_GAMEPLAY
    );

    public static final KeyMapping CANCEL_HOLD_RELEASE_KEY = new KeyMapping(
            "key.voidcraft.cancel_hold_release",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Q,
            KeyMapping.CATEGORY_GAMEPLAY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SKILL_KEY_1);
        event.register(SKILL_KEY_2);
        event.register(CANCEL_HOLD_RELEASE_KEY);
    }
}
