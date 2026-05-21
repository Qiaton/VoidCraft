package com.example.voidcraft;

import com.example.voidcraft.Block.ModBlockEntities;
import com.example.voidcraft.Block.entity.VoidEnergyConverterBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.VOID_ENERGY_CONVERTER_BLOCK_ENTITY.get(),
                VoidEnergyConverterBlockEntity::getEnergyHandler
        );
    }
}
