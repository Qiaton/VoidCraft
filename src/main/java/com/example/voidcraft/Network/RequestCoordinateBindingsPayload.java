package com.example.voidcraft.Network;

import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// 客户端从机器 GUI 请求打开连接列表时发送，真正列表由服务端即时组装。
public record RequestCoordinateBindingsPayload(BoundVoidPosition owner) implements CustomPacketPayload {
    public static final Type<RequestCoordinateBindingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "request_coordinate_bindings"));

    public static final StreamCodec<ByteBuf, RequestCoordinateBindingsPayload> STREAM_CODEC = StreamCodec.of(
            RequestCoordinateBindingsPayload::encode,
            RequestCoordinateBindingsPayload::decode
    );

    private static void encode(ByteBuf buffer, RequestCoordinateBindingsPayload payload) {
        CoordinateBindingsPayload.writePosition(buffer, payload.owner);
    }

    private static RequestCoordinateBindingsPayload decode(ByteBuf buffer) {
        return new RequestCoordinateBindingsPayload(CoordinateBindingsPayload.readPosition(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
