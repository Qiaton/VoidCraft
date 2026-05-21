package com.example.voidcraft.Network;

import com.example.voidcraft.VoidCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 客户端只同步手动炮台输入；辅助炮台不发送这个包。
public record UseTurretShotPayload(boolean shooting, boolean volleyShooting) implements CustomPacketPayload {
    public static final Type<UseTurretShotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoidCraft.MODID, "use_turret_shot"));

    public UseTurretShotPayload(boolean shooting) {
        this(shooting, false);
    }

    public static final StreamCodec<ByteBuf, UseTurretShotPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.shooting());
                        buf.writeBoolean(payload.volleyShooting());
                    },
                    buf -> new UseTurretShotPayload(
                            buf.readBoolean(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
