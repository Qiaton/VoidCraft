package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record CancelTeleportModulePayload(int slot) implements CustomPacketPayload {
    public static final Type<CancelTeleportModulePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "cancel_teleport_module"));

    public static final StreamCodec<ByteBuf, CancelTeleportModulePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CancelTeleportModulePayload::slot,
                    CancelTeleportModulePayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
