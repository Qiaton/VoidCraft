package com.example.voidcraft.network;

import com.example.voidcraft.Block.entity.BoundVoidPosition;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemoveCoordinateBindingPayload(
        BoundVoidPosition owner,
        boolean outputList,
        BoundVoidPosition target
) implements CustomPacketPayload {
    public static final Type<RemoveCoordinateBindingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "remove_coordinate_binding"));

    public static final StreamCodec<ByteBuf, RemoveCoordinateBindingPayload> STREAM_CODEC = StreamCodec.of(
            RemoveCoordinateBindingPayload::encode,
            RemoveCoordinateBindingPayload::decode
    );

    private static void encode(ByteBuf buffer, RemoveCoordinateBindingPayload payload) {
        CoordinateBindingsPayload.writePosition(buffer, payload.owner);
        buffer.writeBoolean(payload.outputList);
        CoordinateBindingsPayload.writePosition(buffer, payload.target);
    }

    private static RemoveCoordinateBindingPayload decode(ByteBuf buffer) {
        return new RemoveCoordinateBindingPayload(
                CoordinateBindingsPayload.readPosition(buffer),
                buffer.readBoolean(),
                CoordinateBindingsPayload.readPosition(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
