package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record OpenPhaseWatchPayload(int slot) implements CustomPacketPayload {
    public static final int HAND_SLOT = -1;

    public static final Type<OpenPhaseWatchPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "open_phase_watch"));

    public static final StreamCodec<ByteBuf, OpenPhaseWatchPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    OpenPhaseWatchPayload::slot,
                    OpenPhaseWatchPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
