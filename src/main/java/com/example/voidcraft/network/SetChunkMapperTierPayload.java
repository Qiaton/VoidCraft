package com.example.voidcraft.network;

import com.example.voidcraft.Block.entity.BoundVoidPosition;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetChunkMapperTierPayload(BoundVoidPosition owner, int tier) implements CustomPacketPayload {
    public static final Type<SetChunkMapperTierPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "set_chunk_mapper_tier"));

    public static final StreamCodec<ByteBuf, SetChunkMapperTierPayload> STREAM_CODEC = StreamCodec.of(
            SetChunkMapperTierPayload::encode,
            SetChunkMapperTierPayload::decode
    );

    private static void encode(ByteBuf buffer, SetChunkMapperTierPayload payload) {
        CoordinateBindingsPayload.writePosition(buffer, payload.owner);
        buffer.writeInt(payload.tier);
    }

    private static SetChunkMapperTierPayload decode(ByteBuf buffer) {
        return new SetChunkMapperTierPayload(
                CoordinateBindingsPayload.readPosition(buffer),
                buffer.readInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
