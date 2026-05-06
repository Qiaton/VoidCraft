package com.example.voidcraft.Block;
import com.example.voidcraft.VoidCraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModBlock {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoidCraft.MODID);//创建方块注册器

    public static final DeferredBlock<PhaseBlock> BLACK_BLOCK = BLOCKS.registerBlock(
            "black_block",
            PhaseBlock::new,
            props -> props
                    .friction(0.6F)     // 普通方块默认摩擦系数
                    .noOcclusion()      // 半透明相位材质需要关闭普通实心遮挡
                    .lightLevel(state -> 4)
                    .strength(1,900000) //前者是挖掘事件 后者是防爆等级
                    .requiresCorrectToolForDrops() //加这个属性代表着如果不被正确的挖掘等级挖掘 将不掉落物品
    );

    public static final DeferredBlock<PhaseBlock> VOID_ORE_BLOCK = BLOCKS.registerBlock(
            "void_ore_block",
            PhaseBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> 4)
                    .strength(1, 900000)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<BatteryBlock> BATTERY_BLOCK = BLOCKS.registerBlock(
            "battery_block",
            BatteryBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .strength(2.5F, 6.0F)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<ChunkMapperBlock> CHUNK_MAPPER_BLOCK = BLOCKS.registerBlock(
            "chunk_mapper_block",
            ChunkMapperBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .strength(2.5F, 6.0F)
                    .requiresCorrectToolForDrops()
    );


    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
