package com.example.voidcraft;



import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE,
            VoidCraft.MODID
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<ModuleData>> MODULE_DATA = DATA_COMPONENTS.registerComponentType(
            "module_data",
            builder -> builder.persistent(ModuleData.CODEC)
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<ModuleModifierData>>  MODULE_MODIFIER_DATA = DATA_COMPONENTS.registerComponentType(
            "module_modifier_data",
            builder -> builder.persistent(ModuleModifierData.CODEC)
    );

    public static void register(IEventBus bus){
        DATA_COMPONENTS.register(bus);
    }
}
