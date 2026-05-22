package com.example.voidcraft;

import com.example.voidcraft.Gui.GuideBookMenu;
import com.example.voidcraft.Gui.ModuleMenu;
import com.example.voidcraft.Gui.ModuleBoostMenu;
import com.example.voidcraft.Gui.VoidChargerMenu;
import com.example.voidcraft.Gui.VoidEnergyConverterMenu;
import com.example.voidcraft.Gui.VoidPhenomenonCollectorMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuType {
    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(
            Registries.MENU,
            "void_craft"
    );
    public static final Supplier<MenuType<ModuleMenu>> registerModuleMenu = MENU_TYPE.register(
            "module_menu",
            () -> new MenuType<>(ModuleMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
    public static final Supplier<MenuType<ModuleBoostMenu>> MODULE_BOOST_MENU = MENU_TYPE.register(
            "module_boost_menu",
            () -> new MenuType<>(ModuleBoostMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
    public static final Supplier<MenuType<VoidPhenomenonCollectorMenu>> VOID_PHENOMENON_COLLECTOR_MENU = MENU_TYPE.register(
            "void_phenomenon_collector",
            // create 扩展会把服务端写入的 BlockPos 交给客户端菜单构造器。
            () -> IMenuTypeExtension.create(VoidPhenomenonCollectorMenu::new)
    );
    public static final Supplier<MenuType<VoidChargerMenu>> VOID_CHARGER_MENU = MENU_TYPE.register(
            "void_charger",
            () -> IMenuTypeExtension.create(VoidChargerMenu::new)
    );
    public static final Supplier<MenuType<VoidEnergyConverterMenu>> VOID_ENERGY_CONVERTER_MENU = MENU_TYPE.register(
            "void_energy_converter",
            () -> IMenuTypeExtension.create(VoidEnergyConverterMenu::new)
    );
    public static final Supplier<MenuType<GuideBookMenu>> GUIDE_BOOK_MENU = MENU_TYPE.register(
            "guide_book",
            () -> new MenuType<>(GuideBookMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
    public static void register(IEventBus bus) {
        MENU_TYPE.register(bus);
    }
}
