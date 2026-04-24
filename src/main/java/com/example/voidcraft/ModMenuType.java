package com.example.voidcraft;

import com.example.voidcraft.Gui.ModuleMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuType {
    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(
            Registries.MENU,
            "void_craft"
    );
    public static final Supplier<MenuType<ModuleMenu>> MODULE_MENU = MENU_TYPE.register(
            "module_menu",
            () -> new MenuType<>(ModuleMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
    public static void register(IEventBus bus) {
        MENU_TYPE.register(bus);
    }
}
