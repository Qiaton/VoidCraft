package com.example.voidcraft.Block;
import com.example.voidcraft.VoidCraft;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModBlockItem {
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(VoidCraft.MODID);
    public static final DeferredItem<BlockItem> BLACK_BLOCK = BLOCK_ITEMS.registerSimpleBlockItem( //快捷创建BlockItem方法
            "black_block",//BlockItem名字
            ModBlock.BLACK_BLOCK //绑定的Block
    );
    public static final DeferredItem<BlockItem> VOID_ORE_BLOCK = BLOCK_ITEMS.registerSimpleBlockItem(
            "void_ore_block",
            ModBlock.VOID_ORE_BLOCK
    );
    public static final DeferredItem<BlockItem> BATTERY_BLOCK = BLOCK_ITEMS.registerSimpleBlockItem(
            "battery_block",
            ModBlock.BATTERY_BLOCK
    );
    public static void register(IEventBus bus) {
        BLOCK_ITEMS.register(bus);
    }
}
