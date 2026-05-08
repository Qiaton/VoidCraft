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
    public static final DeferredItem<BatteryBlockItem> BATTERY_BLOCK = BLOCK_ITEMS.registerItem(
            "battery_block",
            props -> new BatteryBlockItem(ModBlock.BATTERY_BLOCK.get(), props),
            props -> props.useBlockDescriptionPrefix()
    );
    public static final DeferredItem<BlockItem> CHUNK_MAPPER_BLOCK = BLOCK_ITEMS.registerSimpleBlockItem(
            "chunk_mapper_block",
            ModBlock.CHUNK_MAPPER_BLOCK
    );
    public static final DeferredItem<BlockItem> VOID_PHENOMENON_COLLECTOR = BLOCK_ITEMS.registerSimpleBlockItem(
            "void_phenomenon_collector",
            // 方块物品只负责放置，逻辑都在 VoidPhenomenonCollectorBlockEntity 里。
            ModBlock.VOID_PHENOMENON_COLLECTOR
    );
    public static final DeferredItem<BlockItem> IMPROVED_VOID_PHENOMENON_COLLECTOR = BLOCK_ITEMS.registerSimpleBlockItem(
            "improved_void_phenomenon_collector",
            ModBlock.IMPROVED_VOID_PHENOMENON_COLLECTOR
    );
    public static final DeferredItem<BlockItem> ADVANCED_VOID_PHENOMENON_COLLECTOR = BLOCK_ITEMS.registerSimpleBlockItem(
            "advanced_void_phenomenon_collector",
            ModBlock.ADVANCED_VOID_PHENOMENON_COLLECTOR
    );
    public static final DeferredItem<BlockItem> VOID_ATTUNER = BLOCK_ITEMS.registerSimpleBlockItem(
            "void_attuner",
            ModBlock.VOID_ATTUNER
    );
    public static void register(IEventBus bus) {
        BLOCK_ITEMS.register(bus);
    }
}
