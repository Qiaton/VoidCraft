package com.example.voidcraft.Network;

import com.example.voidcraft.Custom.Behavior.Energy.BoundVoidPosition;
import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 解绑面板点击删除时发送：owner 是当前方块，target 是要删掉的另一端。
public record RemoveCoordinateBindingPayload(
        BoundVoidPosition owner,
        boolean outputList,
        BoundVoidPosition target
) implements CustomPacketPayload {
    public static final Type<RemoveCoordinateBindingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "remove_coordinate_binding"));

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
