package com.example.testmod2.CreativeModeTab;

import com.example.testmod2.Block.ModBlockItem;
import com.example.testmod2.Item.ModItem;
import com.example.testmod2.TestMod2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            TestMod2.MODID
    );
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TESTMOD2_TAB = TABS.register(
            "testmod2",()-> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.testmod2.test_tab"))
                    .icon(()-> new ItemStack(Items.IRON_AXE))
                    .displayItems(((parameters, output) -> {
                        output.accept(ModBlockItem.BLACK_BLOCK.get());
                        output.accept(ModItem.FLOW_TYPE.get());
                        output.accept(ModItem.SPATIAL_SWORD);
                    }))
                    .build()

    );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
