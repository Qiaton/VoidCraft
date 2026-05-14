package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReleaseBlackHoleModulePayload(int slot, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<ReleaseBlackHoleModulePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "release_black_hole_module"));

    public static final StreamCodec<ByteBuf, ReleaseBlackHoleModulePayload> STREAM_CODEC = StreamCodec.of(
            ReleaseBlackHoleModulePayload::encode,
            ReleaseBlackHoleModulePayload::decode
    );

    private static void encode(ByteBuf buffer, ReleaseBlackHoleModulePayload payload) {
        ByteBufCodecs.INT.encode(buffer, payload.slot);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.x);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.y);
        ByteBufCodecs.DOUBLE.encode(buffer, payload.z);
    }

    private static ReleaseBlackHoleModulePayload decode(ByteBuf buffer) {
        return new ReleaseBlackHoleModulePayload(
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
