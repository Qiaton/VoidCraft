package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;


public record UseWatchModulePayload(int slot) implements CustomPacketPayload {

    public static final Type<UseWatchModulePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "use_watch_module"));

    public static final StreamCodec<ByteBuf, UseWatchModulePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UseWatchModulePayload::slot,
                    UseWatchModulePayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}