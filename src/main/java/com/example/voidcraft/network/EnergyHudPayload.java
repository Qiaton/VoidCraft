package com.example.voidcraft.network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EnergyHudPayload(int percent, boolean visible) implements CustomPacketPayload {
    public static final Type<EnergyHudPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "energy_hud"));

    public static final StreamCodec<ByteBuf, EnergyHudPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    EnergyHudPayload::percent,
                    ByteBufCodecs.BOOL,
                    EnergyHudPayload::visible,
                    EnergyHudPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
