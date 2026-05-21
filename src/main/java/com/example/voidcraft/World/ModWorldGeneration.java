package com.example.voidcraft.World;

import com.example.voidcraft.VoidCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class ModWorldGeneration {
    private static final ResourceLocation PHASE_SHALLOWS_ID = ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_shallows");

    private ModWorldGeneration() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModWorldGeneration::registerChunkGenerators);
    }

    private static void registerChunkGenerators(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, PHASE_SHALLOWS_ID, () -> PhaseShallowsChunkGenerator.CODEC);
    }
}
