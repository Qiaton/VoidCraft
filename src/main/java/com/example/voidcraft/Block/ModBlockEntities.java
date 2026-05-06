package com.example.voidcraft.Block;

import com.example.voidcraft.Block.entity.BatteryBlockEntity;
import com.example.voidcraft.Block.entity.ChunkMapperBlockEntity;
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

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
