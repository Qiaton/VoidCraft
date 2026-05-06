package com.example.voidcraft;

import com.example.voidcraft.ClientCustom.EnergyHud;
import com.example.voidcraft.ClientCustom.FlowEffect;
import com.example.voidcraft.ClientCustom.Turret.PhaseEmitterClientManager;
import com.example.voidcraft.ClientCustom.Void.PhaseWorldTransitionOverlay;
import com.example.voidcraft.ClientCustom.Void.PhaseWorldTransitionScreen;
import com.example.voidcraft.ClientCustom.Void.PhaseWorldTransitionScreenRegistration;
import com.example.voidcraft.Gui.ModuleScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;


// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = VoidCraft.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = VoidCraft.MODID, value = Dist.CLIENT)
public class VoidCraftClient {
    public VoidCraftClient(IEventBus bus) {
        bus.addListener(PhaseWorldTransitionScreenRegistration::registerPhaseWorldTransitionScreen);
    }

    @SubscribeEvent
    public static void ClientFlowFov(net.neoforged.neoforge.client.event.ClientTickEvent.Post  event) {
        if(FlowEffect.fov_effect>1.38){             //控制视角缩放效果
            FlowEffect.fov_effect-=0.1F;
        }
        else if(FlowEffect.fov_effect>0.01){
            FlowEffect.fov_effect-=0.5F;
        }
        else{
            FlowEffect.fov_effect=0;
        }
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PhaseEmitterClientManager.tick();
        PhaseEmitterClientManager.tickLocalAttackInput();
    }
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (PhaseEmitterClientManager.handleMouseButton(event.getButton(), event.getAction())) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (PhaseEmitterClientManager.shouldHideLocalHands()) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void MODULE_MENU(RegisterMenuScreensEvent event){
        event.register(ModMenuType.MODULE_MENU.get(), ModuleScreen::new);
    }

    @SubscribeEvent
    public static void REGISTER_GUI_LAYERS(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(VoidCraft.MODID, "energy_hud"),
                EnergyHud::render
        );
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_world_transition"),
                (guiGraphics, deltaTracker) -> PhaseWorldTransitionOverlay.render(guiGraphics)
        );
    }

    @SubscribeEvent
    public static void PHASE_WORLD_TRANSITION_SCREEN_OVERLAY(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof PhaseWorldTransitionScreen)) {
            PhaseWorldTransitionOverlay.render(event.getGuiGraphics(), true);
        }
    }

}
