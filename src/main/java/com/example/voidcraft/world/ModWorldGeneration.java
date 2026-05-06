package com.example.voidcraft.world;

import com.example.voidcraft.VoidCraft;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModWorldGeneration {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(
            Registries.CHUNK_GENERATOR,
            VoidCraft.MODID
    );

    public static final Supplier<MapCodec<? extends ChunkGenerator>> PHASE_SHALLOWS = CHUNK_GENERATORS.register(
            "phase_shallows",
            () -> PhaseShallowsChunkGenerator.CODEC
    );

    private ModWorldGeneration() {
    }

    public static void register(IEventBus bus) {
        CHUNK_GENERATORS.register(bus);
    }
}
