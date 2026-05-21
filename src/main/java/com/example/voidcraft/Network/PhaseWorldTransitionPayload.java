package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhaseWorldTransitionPayload(ResourceLocation sourceDimension, ResourceLocation targetDimension) implements CustomPacketPayload {
    public static final Type<PhaseWorldTransitionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_world_transition"));

    public static final StreamCodec<ByteBuf, PhaseWorldTransitionPayload> STREAM_CODEC = StreamCodec.of(
            PhaseWorldTransitionPayload::encode,
            PhaseWorldTransitionPayload::decode
    );

    private static void encode(ByteBuf buffer, PhaseWorldTransitionPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buffer, payload.sourceDimension());
        ResourceLocation.STREAM_CODEC.encode(buffer, payload.targetDimension());
    }

    private static PhaseWorldTransitionPayload decode(ByteBuf buffer) {
        return new PhaseWorldTransitionPayload(
                ResourceLocation.STREAM_CODEC.decode(buffer),
                ResourceLocation.STREAM_CODEC.decode(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
