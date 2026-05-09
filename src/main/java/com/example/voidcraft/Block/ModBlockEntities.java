package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.BatteryBlockEntity;
import com.example.voidcraft.Block.entity.ChunkMapperBlockEntity;
import com.example.voidcraft.Block.entity.VoidChargerBlockEntity;
import com.example.voidcraft.Block.entity.VoidPhenomenonCollectorBlockEntity;
import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            VoidCraft.MODID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "battery_block",
            () -> new BlockEntityType<>(BatteryBlockEntity::new, Set.of(ModBlock.BATTERY_BLOCK.get()))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChunkMapperBlockEntity>> CHUNK_MAPPER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chunk_mapper_block",
            () -> new BlockEntityType<>(ChunkMapperBlockEntity::new, Set.of(ModBlock.CHUNK_MAPPER_BLOCK.get()))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoidPhenomenonCollectorBlockEntity>> VOID_PHENOMENON_COLLECTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "void_phenomenon_collector",
            // 发电机方块实体负责结晶槽、虚空能缓存、输出绑定和菜单数据。
            () -> new BlockEntityType<>(
                    VoidPhenomenonCollectorBlockEntity::new,
                    Set.of(
                            ModBlock.VOID_PHENOMENON_COLLECTOR.get(),
                            ModBlock.IMPROVED_VOID_PHENOMENON_COLLECTOR.get(),
                            ModBlock.ADVANCED_VOID_PHENOMENON_COLLECTOR.get(),
                            ModBlock.VOID_ATTUNER.get()
                    )
            )
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoidChargerBlockEntity>> VOID_CHARGER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "void_charger",
            () -> new BlockEntityType<>(
                    VoidChargerBlockEntity::new,
                    Set.of(
                            ModBlock.LOW_VOID_CHARGER.get(),
                            ModBlock.MID_VOID_CHARGER.get(),
                            ModBlock.HIGH_VOID_CHARGER.get()
                    )
            )
    );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
