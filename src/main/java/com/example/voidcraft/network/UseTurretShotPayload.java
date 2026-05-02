package com.example.voidcraft.network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// 客户端只同步“手动炮台是否正在按住射击”；辅助炮台不发送这个包。
public record UseTurretShotPayload(boolean shooting) implements CustomPacketPayload {
    public static final Type<UseTurretShotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidCraft.MODID, "use_turret_shot"));

    public static final StreamCodec<ByteBuf, UseTurretShotPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.shooting());
                    },
                    buf -> new UseTurretShotPayload(buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
