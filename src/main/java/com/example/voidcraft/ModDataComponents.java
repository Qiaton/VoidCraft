package com.example.voidcraft;



import com.mojang.serialization.Codec;
import com.example.voidcraft.Item.custom.EnergyCoreData;
import com.example.voidcraft.Item.custom.CoordinateDesignatorData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleData;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleModifierData;
import com.example.voidcraft.Item.custom.WatchEnergyData;
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
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<WatchEnergyData>> WATCH_ENERGY = DATA_COMPONENTS.registerComponentType(
            "watch_energy",
            builder -> builder.persistent(WatchEnergyData.CODEC)
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<EnergyCoreData>> ENERGY_CORE_DATA = DATA_COMPONENTS.registerComponentType(
            "energy_core_data",
            builder -> builder.persistent(EnergyCoreData.CODEC)
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<CoordinateDesignatorData>> COORDINATE_DESIGNATOR_DATA = DATA_COMPONENTS.registerComponentType(
            "coordinate_designator_data",
            builder -> builder.persistent(CoordinateDesignatorData.CODEC)
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> PHASE_TURRET_FORM = DATA_COMPONENTS.registerComponentType(
            "phase_turret_form",
            builder -> builder.persistent(Codec.INT)
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> VOID_MODULE_FORM = DATA_COMPONENTS.registerComponentType(
            "void_module_form",
            builder -> builder.persistent(Codec.INT)
    );
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> VOID_CRYSTAL_PROGRESS = DATA_COMPONENTS.registerComponentType(
            "void_crystal_progress",
            // 结晶自己的发电进度存在物品栈上，机器只负责读取和推进。
            builder -> builder.persistent(Codec.INT)
    );

    public static void register(IEventBus bus){
        DATA_COMPONENTS.register(bus);
    }
}
