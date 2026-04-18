package com.example.voidcraft.Block;
import com.example.voidcraft.VoidCraft;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModBlock {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoidCraft.MODID);//创建方块注册器

    public static final DeferredBlock<Block> BLACK_BLOCK = BLOCKS.registerSimpleBlock(
            "black_block",
            props -> props
                    .friction(4)        //方块挖掘等级
                    .strength(1,900000) //前者是挖掘事件 后者是防爆等级
                    .requiresCorrectToolForDrops() //加这个属性代表着如果不被正确的挖掘等级挖掘 将不掉落物品
    );


    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}

