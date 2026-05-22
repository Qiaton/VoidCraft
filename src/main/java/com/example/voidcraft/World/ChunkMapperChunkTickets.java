package com.example.voidcraft.World;

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
    // NeoForge 区块票控制器：区块映射器用它申请和释放强加载。
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
        // 存档加载或方块被移除后，校验器会清掉已经不该保留的区块票。
        for (BlockPos owner : ticketHelper.getBlockTickets().keySet()) {
            BlockEntity blockEntity = level.getBlockEntity(owner);
            if (!(blockEntity instanceof ChunkMapperBlockEntity mapper) || !mapper.shouldKeepForcedTickets()) {
                ticketHelper.removeAllTickets(owner);
            }
        }
    }
}
