package com.example.voidcraft;

import com.example.voidcraft.Block.ModBlock;
import com.example.voidcraft.Block.ModBlockItem;
import com.example.voidcraft.CreativeModeTab.CreativeModeTabs;
import com.example.voidcraft.Item.ModItem;
import com.example.voidcraft.network.ModNetworking;
import com.example.voidcraft.Sound.ModSound;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.common.Mod;

@Mod(VoidCraft.MODID)
public class VoidCraft {
    public static final String MODID = "testmod2";
    public VoidCraft(IEventBus bus) {        //初始化类
        CreativeModeTabs.register(bus);
        ModBlock.register(bus);
        ModBlockItem.register(bus);
        ModItem.register(bus);
        ModAttachments.register(bus);
        ModSound.register(bus);
        ModNetworking.register(bus);
    }

    }
