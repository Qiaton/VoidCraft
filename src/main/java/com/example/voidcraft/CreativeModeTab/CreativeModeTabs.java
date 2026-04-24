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
                        output.accept(ModItem.PHASE_GAUNTLET);
                        output.accept(getModuleItem());
                        output.accept(getModuleModifierItem());
                    }))
                    .build()

    );
    public static ItemStack getModuleItem(){
        ItemStack stack = new ItemStack(ModItem.MODULE_ITEM.get());
        stack.set(ModDataComponents.MODULE_DATA.value(),new ModuleData(ModuleMode.BURST,3, List.of(new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,5),new ModuleModifierData(ModuleModifierType.SPEED_BOOST,5))));
        return stack;
    }    public static ItemStack getModuleModifierItem(){
        ItemStack stack = new ItemStack(ModItem.MODULE_MODIFIER_ITEM.get());
        stack.set(ModDataComponents.MODULE_MODIFIER_DATA.value(),new ModuleModifierData(ModuleModifierType.COOLDOWN_REDUCTION,3));
        return stack;
    }
    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
