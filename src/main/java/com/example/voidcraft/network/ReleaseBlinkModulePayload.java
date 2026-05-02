package com.example.voidcraft.network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReleaseBlinkModulePayload(int slot, int ticks, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<ReleaseBlinkModulePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "release_blink_module"));

    public static final StreamCodec<ByteBuf, ReleaseBlinkModulePayload> STREAM_CODEC = StreamCodec.of(
            ReleaseBlinkModulePayload::encode,
            ReleaseBlinkModulePayload::decode
    );

    private static void encode(ByteBuf buffer, ReleaseBlinkModulePayload payload) {
        ByteBufCodecs.INT.encode(buffer, payload.slot);
        ByteBufCodecs.INT.encode(buffer, payload.ticks);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.x);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.y);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.z);
    }

    private static ReleaseBlinkModulePayload decode(ByteBuf buffer) {
        return new ReleaseBlinkModulePayload(
                ByteBufCodecs.INT.decode(buffer),
                ByteBufCodecs.INT.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
