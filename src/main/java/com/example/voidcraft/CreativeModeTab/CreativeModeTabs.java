package com.example.voidcraft.CreativeModeTab;

import com.example.voidcraft.Block.ModBlockItem;
import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.Item.custom.ModuleItem.*;
import com.example.voidcraft.ModDataComponents;
import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class CreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            VoidCraft.MODID
    );
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> VOID_CRAFT_TAB = TABS.register(
            "void_craft",()-> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.void_craft.create_tab"))
                    .icon(()-> new ItemStack(Items.IRON_AXE))
                    .displayItems(((parameters, output) -> {
                        output.accept(ModBlockItem.BLACK_BLOCK.get());
                        output.accept(ModItem.FLOW_TYPE.get());
                        output.accept(ModItem.SPATIAL_SWORD);
                        output.accept(ModItem.PHASE_WATCH);
                        output.accept(getModuleItem());
                        output.accept(getModuleModifierItem());
                        output.accept(getHealthModuleItem());
                        output.accept(getDashVoidModuleItem());
                        output.accept(getBlinkVoidModuleItem());
                        output.accept(getSafeBlinkVoidModuleItem());
                        output.accept(getPhaseTurretModuleItem());
                        output.accept(getAssistPhaseTurretModuleItem());
                    }))
                    .build()

    );
    public static ItemStack getModuleItem(){
        ItemStack stack = new ItemStack(ModItem.MODULE_ITEM.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5))));
        return stack;
    }
    public static ItemStack getHealthModuleItem(){
        ItemStack stack = new ItemStack(ModItem.HEALTH_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.CHANNEL,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5))));
        return stack;
    }
    public static ItemStack getBlinkVoidModuleItem(){
        ItemStack stack = new ItemStack(ModItem.BLINK_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5)))); // Blink 只支持 BURST，创造栏样品也要给 BURST
        return stack;
    }
    public static ItemStack getSafeBlinkVoidModuleItem(){
        ItemStack stack = new ItemStack(ModItem.SAFE_BLINK_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getPhaseTurretModuleItem(){
        ItemStack stack = new ItemStack(ModItem.PHASE_TURRET_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.CHANNEL,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getAssistPhaseTurretModuleItem(){
        ItemStack stack = new ItemStack(ModItem.ASSIST_PHASE_TURRET_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.CHANNEL,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,5))));
        return stack;
    }
    public static ItemStack getModuleModifierItem(){
        ItemStack stack = new ItemStack(ModItem.MODULE_MODIFIER_ITEM.get());
        stack.set(ModDataComponents.MODULE_MODIFIER_DATA.value(),new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,3));
        return stack;
    }
    public static ItemStack getDashVoidModuleItem(){
        ItemStack stack = new ItemStack(ModItem.DASH_VOID_MODULE.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.CHANNEL,7, List.of(new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5),new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,20),new ModuleModifierData(ModuleModifierType.ACTIVE_DURATION,20))));
        return stack;
    }
    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
