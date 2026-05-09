package com.example.voidcraft.Block;
import com.example.voidcraft.Block.Block.*;
import com.example.voidcraft.VoidCraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModBlock {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoidCraft.MODID);//创建方块注册器
    private static final float FUNCTIONAL_BLOCK_STRENGTH = 3.0F;
    private static final float FUNCTIONAL_BLOCK_BLAST_RESISTANCE = 3.0F;

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

    public static final DeferredBlock<VoidOreBlock> VOID_ORE_BLOCK = BLOCKS.registerBlock(
            "void_ore_block",
            VoidOreBlock::new,
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
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<ChunkMapperBlock> CHUNK_MAPPER_BLOCK = BLOCKS.registerBlock(
            "chunk_mapper_block",
            ChunkMapperBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<VoidEnergyConverterBlock> VOID_ENERGY_CONVERTER = BLOCKS.registerBlock(
            "void_energy_converter",
            VoidEnergyConverterBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> 4)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<VoidPhenomenonCollectorBlock> VOID_PHENOMENON_COLLECTOR = BLOCKS.registerBlock(
            "void_phenomenon_collector",
            VoidPhenomenonCollectorBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    // ACTIVE 时发光更强，用来反馈发电机正在消耗结晶。
                    .lightLevel(state -> state.getValue(VoidPhenomenonCollectorBlock.ACTIVE) ? 7 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<VoidPhenomenonCollectorBlock> IMPROVED_VOID_PHENOMENON_COLLECTOR = BLOCKS.registerBlock(
            "improved_void_phenomenon_collector",
            VoidPhenomenonCollectorBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VoidPhenomenonCollectorBlock.ACTIVE) ? 7 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<VoidPhenomenonCollectorBlock> ADVANCED_VOID_PHENOMENON_COLLECTOR = BLOCKS.registerBlock(
            "advanced_void_phenomenon_collector",
            VoidPhenomenonCollectorBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VoidPhenomenonCollectorBlock.ACTIVE) ? 7 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<VoidPhenomenonCollectorBlock> VOID_ATTUNER = BLOCKS.registerBlock(
            "void_attuner",
            VoidPhenomenonCollectorBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VoidPhenomenonCollectorBlock.ACTIVE) ? 7 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<ModuleBoostBlock> MODULE_BOOST_TABLE = BLOCKS.registerBlock(
            "module_boost_table",
            ModuleBoostBlock::new,
            props -> props
                    .friction(0.6F)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<VoidChargerBlock> LOW_VOID_CHARGER = BLOCKS.registerBlock(
            "low_void_charger",
            VoidChargerBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VoidChargerBlock.ACTIVE) ? 7 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<VoidChargerBlock> MID_VOID_CHARGER = BLOCKS.registerBlock(
            "mid_void_charger",
            VoidChargerBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VoidChargerBlock.ACTIVE) ? 8 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<VoidChargerBlock> HIGH_VOID_CHARGER = BLOCKS.registerBlock(
            "high_void_charger",
            VoidChargerBlock::new,
            props -> props
                    .friction(0.6F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VoidChargerBlock.ACTIVE) ? 10 : 3)
                    .strength(FUNCTIONAL_BLOCK_STRENGTH, FUNCTIONAL_BLOCK_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
    );


    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
