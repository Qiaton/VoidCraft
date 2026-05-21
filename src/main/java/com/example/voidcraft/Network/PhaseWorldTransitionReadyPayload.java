package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhaseWorldTransitionReadyPayload() implements CustomPacketPayload {
    public static final Type<PhaseWorldTransitionReadyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "phase_world_transition_ready"));

    public static final StreamCodec<ByteBuf, PhaseWorldTransitionReadyPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
            },
            buffer -> new PhaseWorldTransitionReadyPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
