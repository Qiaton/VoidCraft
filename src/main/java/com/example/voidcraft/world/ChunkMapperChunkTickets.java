package com.example.voidcraft.world;

import com.example.voidcraft.Block.entity.ChunkMapperBlockEntity;
import com.example.voidcraft.VoidCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

public final class ChunkMapperChunkTickets {
    public static final TicketController CHUNK_MAPPER = new TicketController(
            Identifier.fromNamespaceAndPath(VoidCraft.MODID, "chunk_mapper"),
            ChunkMapperChunkTickets::validateTickets
    );

    private ChunkMapperChunkTickets() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ChunkMapperChunkTickets::registerTicketControllers);
    }

    private static void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CHUNK_MAPPER);
    }

    private static void validateTickets(ServerLevel level, net.neoforged.neoforge.common.world.chunk.TicketHelper ticketHelper) {
        for (BlockPos owner : ticketHelper.getBlockTickets().keySet()) {
            BlockEntity blockEntity = level.getBlockEntity(owner);
            if (!(blockEntity instanceof ChunkMapperBlockEntity mapper) || !mapper.shouldKeepForcedTickets()) {
                ticketHelper.removeAllTickets(owner);
            }
        }
    }
}
