package com.example.testmod2.Block;
import com.example.testmod2.Item.ModItem;
import com.example.testmod2.TestMod2;
import com.jcraft.jorbis.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModBlockItem {
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(TestMod2.MODID);
    public static final DeferredItem<BlockItem> BLACK_BLOCK = BLOCK_ITEMS.registerSimpleBlockItem( //快捷创建BlockItem方法
            "black_block",//BlockItem名字
            ModBlock.BLACK_BLOCK //绑定的Block
    );
    public static void register(IEventBus bus) {
        BLOCK_ITEMS.register(bus);
    }
}

