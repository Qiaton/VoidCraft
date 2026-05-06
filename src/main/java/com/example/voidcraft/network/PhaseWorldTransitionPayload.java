package com.example.voidcraft.network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PhaseWorldTransitionPayload(Identifier sourceDimension, Identifier targetDimension) implements CustomPacketPayload {
    public static final Type<PhaseWorldTransitionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_world_transition"));

    public static final StreamCodec<ByteBuf, PhaseWorldTransitionPayload> STREAM_CODEC = StreamCodec.of(
            PhaseWorldTransitionPayload::encode,
            PhaseWorldTransitionPayload::decode
    );

    private static void encode(ByteBuf buffer, PhaseWorldTransitionPayload payload) {
        Identifier.STREAM_CODEC.encode(buffer, payload.sourceDimension());
        Identifier.STREAM_CODEC.encode(buffer, payload.targetDimension());
    }

    private static PhaseWorldTransitionPayload decode(ByteBuf buffer) {
        return new PhaseWorldTransitionPayload(
                Identifier.STREAM_CODEC.decode(buffer),
                Identifier.STREAM_CODEC.decode(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
