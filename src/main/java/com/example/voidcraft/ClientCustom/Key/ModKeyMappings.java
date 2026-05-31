package com.example.voidcraft.ClientCustom.Key;

import com.example.voidcraft.VoidCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
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
    private static final KeyMapping.Category VOID_CRAFT_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "void_craft")
    );

    public static final KeyMapping SKILL_KEY_1 = new KeyMapping(
            "key.voidcraft.skill1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            VOID_CRAFT_CATEGORY
    );

    public static final KeyMapping SKILL_KEY_2 = new KeyMapping(
            "key.voidcraft.skill2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            VOID_CRAFT_CATEGORY
    );

    public static final KeyMapping CANCEL_HOLD_RELEASE_KEY = new KeyMapping(
            "key.voidcraft.cancel_hold_release",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Q,
            VOID_CRAFT_CATEGORY
    );

    public static final KeyMapping OPEN_PHASE_WATCH_KEY = new KeyMapping(
            "key.voidcraft.open_phase_watch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            VOID_CRAFT_CATEGORY
    );

    public static final KeyMapping SWITCH_MODULE_FORM_KEY = new KeyMapping(
            "key.voidcraft.switch_module_form",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            VOID_CRAFT_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SKILL_KEY_1);
        event.register(SKILL_KEY_2);
        event.register(CANCEL_HOLD_RELEASE_KEY);
        event.register(OPEN_PHASE_WATCH_KEY);
        event.register(SWITCH_MODULE_FORM_KEY);
    }
}
