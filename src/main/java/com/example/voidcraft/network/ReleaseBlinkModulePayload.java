package com.example.voidcraft.network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReleaseBlinkModulePayload(int slot, int ticks) implements CustomPacketPayload {
    public static final Type<ReleaseBlinkModulePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "release_blink_module"));

    public static final StreamCodec<ByteBuf, ReleaseBlinkModulePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ReleaseBlinkModulePayload::slot,
                    ByteBufCodecs.INT,
                    ReleaseBlinkModulePayload::ticks,
                    ReleaseBlinkModulePayload::new
            );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}