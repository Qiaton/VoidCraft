package com.example.voidcraft;

import com.example.voidcraft.ClientCustom.FlowEffect;
import com.example.voidcraft.ClientCustom.Coordinate.CoordinateBindingPreviewClient;
import com.example.voidcraft.ClientCustom.Event.HoldReleaseClientDispatcher;
import com.example.voidcraft.ClientCustom.Generator.VoidPhenomenonCollectorBlackHoleClient;
import com.example.voidcraft.ClientCustom.Turret.PhaseEmitterClientManager;
import com.example.voidcraft.Gui.EnergyHud;
import com.example.voidcraft.Gui.ModuleBoostScreen;
import com.example.voidcraft.Gui.ModuleScreen;
import com.example.voidcraft.Gui.PhaseWorldTransitionOverlay;
import com.example.voidcraft.Gui.PhaseWorldTransitionScreen;
import com.example.voidcraft.Gui.PhaseWorldTransitionScreenRegistration;
import com.example.voidcraft.Gui.VoidChargerScreen;
import com.example.voidcraft.Gui.VoidEnergyConverterScreen;
import com.example.voidcraft.Gui.VoidPhenomenonCollectorScreen;
import com.example.voidcraft.World.projection.PhaseProjectionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
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
    public static void tickFlowFov(net.neoforged.neoforge.client.event.ClientTickEvent.Post  event) {
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
        CoordinateBindingPreviewClient.tick();
        VoidPhenomenonCollectorBlackHoleClient.tick();
        PhaseProjectionClient.tick();
    }
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (PhaseEmitterClientManager.handleMouseButton(event.getButton(), event.getAction())) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (HoldReleaseClientDispatcher.handleScroll(Minecraft.getInstance(), event.getScrollDeltaY())) {
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
    public static void registerModuleMenu(RegisterMenuScreensEvent event){
        // 客户端把服务端菜单类型绑定到对应 Screen。
        event.register(ModMenuType.registerModuleMenu.get(), ModuleScreen::new);
        event.register(ModMenuType.MODULE_BOOST_MENU.get(), ModuleBoostScreen::new);
        event.register(ModMenuType.VOID_PHENOMENON_COLLECTOR_MENU.get(), VoidPhenomenonCollectorScreen::new);
        event.register(ModMenuType.VOID_CHARGER_MENU.get(), VoidChargerScreen::new);
        event.register(ModMenuType.VOID_ENERGY_CONVERTER_MENU.get(), VoidEnergyConverterScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "energy_hud"),
                EnergyHud::render
        );
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_world_transition"),
                (guiGraphics, deltaTracker) -> PhaseWorldTransitionOverlay.render(guiGraphics)
        );
    }

    @SubscribeEvent
    public static void renderPhaseScreenOverlay(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof PhaseWorldTransitionScreen)) {
            PhaseWorldTransitionOverlay.render(event.getGuiGraphics(), true);
        }
    }

}
