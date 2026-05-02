package com.example.voidcraft;

import com.example.voidcraft.Block.ModBlock;
import com.example.voidcraft.Block.ModBlockItem;
import com.example.voidcraft.ClientCustom.Key.ModKeyMappings;
import com.example.voidcraft.CreativeModeTab.CreativeModeTabs;
import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.loot.ModLootTables;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;

@Mod(VoidCraft.MODID)
public class VoidCraft {
    public static final String MODID = "void_craft";
    public VoidCraft(IEventBus bus, ModContainer modContainer) {        //初始化类
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        CreativeModeTabs.register(bus);
        ModBlock.register(bus);
        ModBlockItem.register(bus);
        ModItem.register(bus);
        ModAttachments.register(bus);
        ModSound.register(bus);
        ModNetworking.register(bus);
        ModDataComponents.register(bus);
        ModMenuType.register(bus);
        ModLootTables.register();
        }
    }
