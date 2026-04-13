package com.example.testmod2;

import com.example.testmod2.Block.ModBlock;
import com.example.testmod2.Block.ModBlockItem;
import com.example.testmod2.CreativeModeTab.CreativeModeTabs;
import com.example.testmod2.Item.ModItem;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.common.Mod;

@Mod(TestMod2.MODID)
public class TestMod2 {
    public static final String MODID = "testmod2";
    public TestMod2(IEventBus bus) {        //初始化类
        CreativeModeTabs.register(bus);
        ModBlock.register(bus);
        ModBlockItem.register(bus);
        ModItem.register(bus);
        ModAttachments.register(bus);
    }

    }

