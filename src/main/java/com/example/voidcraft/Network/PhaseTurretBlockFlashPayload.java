package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PhaseTurretBlockFlashPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PhaseTurretBlockFlashPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_turret_block_flash"));

    public static final StreamCodec<ByteBuf, PhaseTurretBlockFlashPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeLong(payload.pos().asLong()),
            buf -> new PhaseTurretBlockFlashPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
